/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.adtarea05;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos
 * Hereda de runnable. Puede ejecutarse como un thread independiente.
 */
public class ComprobadorFiles implements Runnable {
    static long contador=0L;
    private Connection con;
    String dir = null;
    private ConexionJson cj;
    private int milis;

    public ComprobadorFiles() {
    }

    public ComprobadorFiles(ConexionJson cj, int milis) {
        this.cj = cj;//Datos de la conexion
        this.milis=milis;//Tiempo entre ciclos de ejecucion del Thread.
        //Datos para crear la conexion a partir del json.
        String address = cj.getDb().getAddress();
        String dbName = cj.getDb().getName();
        String user = cj.getDb().getUser();
        String pass = cj.getDb().getPassword();
        dir = cj.getApp().getDirectory();
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);
        String conectarStr = "jdbc:postgresql://" + address + "/" + dbName;
        try {
            con = DriverManager.getConnection(conectarStr, props);
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        @Override
    public void run() {
        File f = new File(cj.getApp().getDirectory());// 
        while (true) {
            System.out.println("comprobacion nº"+contador++);
              leerDirectorios(f);
              this.leerArchivos(f);
            try {//Dormimos el hilo la cantidad de milisegundos que hayamos establecido.
                Thread.sleep(milis);
            } catch (InterruptedException ex) {
                Logger.getLogger(ComprobadorFiles.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    // Adaptacion de los metodos del repositorio para esta clase.
    private void leerDirectorios(File file) {//Reibe repositorio, File del raiz del json, arraylist directorios.

        if (file.isDirectory()) {//Si es un directorio
            //OBJETO
            Directorio dir = new Directorio();//Instanciar objeto.
            try {
                dir.setNombre(file.getCanonicalPath());//Añadir nombre al objeto directorio. En el objeto está la ruta entera. 
            } catch (IOException ex) {
                Logger.getLogger(ADTarea05.class.getName()).log(Level.SEVERE, null, ex);
            }
            //GRABADO SI EXISTE
            //Grabar el objeto file. El propio metodo añade el id al Objeto que se le pasa
            if (!existeDir(dir.getNombre())) {//Si no existe el directorio, se guarda.
                saveDir(dir);
            }
            //RECURSIVIDAD----------
            File[] innerFiles = file.listFiles();// Obtener un array de File con los Files dentro del directorio
            for (File eachFile : innerFiles) {
                leerDirectorios(eachFile);//Aplicamos este mismo metodo a cada uno de los files de forma recursiva
            }
        }
    }

    private void leerArchivos(File file) {
        if (file.isDirectory()) {//Si es un directorio, aplicamos Recursivamente este mismo metodo a todo su contenido.
            File[] innerFiles = file.listFiles();// Obtener un array de File con los Files dentro del directorio
            for (File eachFile : innerFiles) {
                leerArchivos( eachFile);//Aplicamos este mismo metodo
            }
        } else {// Es archivo. Creamos un objeto de la clase archivo para abstraer el codigo.
            Archivo ar = new Archivo();
            ar.setNombre(file.getName());
            String directorioReal=file.getParent();
            String directorioEnBD=Repositorio.adaptarNombreAFormatoDB(cj.getApp().getDirectory(),directorioReal);
            
            ar.setIdDir(getIdDir(directorioEnBD));
            if (!existeArchivo(ar)) {
                saveArchivo(ar, file);
            }
        }
    }

    public void saveDir(Directorio dir) {
        String raiz = cj.getApp().getDirectory();
        String nombreReal = dir.getNombre();
        String nombreAdaptado = Repositorio.adaptarNombreAFormatoDB(raiz, nombreReal);
        try {

            if (getIdDir(nombreAdaptado) == -1) {//Si no existe el directorio, lo guardamos. El directorio no existe si el metodo de recuperar id devuelve -1
                String sqlInsert = "Insert into directorios(nombre) values (?)";
                PreparedStatement pst = con.prepareStatement(sqlInsert);
                pst = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
                pst.setString(1, nombreAdaptado);
                pst.execute();
                //Obtener las keys e insertarlas en 
                ResultSet rsk = pst.getGeneratedKeys();
                rsk.next();
                int key = rsk.getInt(1);
                dir.setId(key);//Añadimos la id de directorio
            } else {
                
            }
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    long getIdDir(String directorioEnDB) {
        long id = -1;
        String cc = "";
        String sql = "Select * from directorios where nombre=?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, directorioEnDB);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                id = rs.getLong(1);
            }
            pst.close();
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
        return id;
    }

    boolean existeDir(String directorioEnDB) {
        boolean existe = false;
        String cc = "";
        String sql = "Select count(*) from directorios where nombre=?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, directorioEnDB);
            ResultSet rs = pst.executeQuery();
            rs.next();
            int cantidad = rs.getInt(1);
            if (cantidad == 1) {
                existe = true;
            }
            pst.close();//cerrar
            rs.close();//cerrar resultset
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
        return existe;
    }

    boolean existeArchivo(Archivo archivo) {
        boolean salida = false; //presuponemos que no existe
        if (archivo.getIdDir() != -1) {//Si el archivo existe: Creamos directorio y reponemos archivo.
            String sqlArchivo = "SELECT count(*) FROM archivos where  nombre=? AND id_dir=?";
            try {
                PreparedStatement pst = con.prepareStatement(sqlArchivo);
                pst.setString(1, archivo.getNombre());
                pst.setLong(2, archivo.getIdDir());
                ResultSet rs = pst.executeQuery();
                rs.next();
                if (rs.getInt(1) == 1) {
                    salida = true;// Si hay algun resultado, cambiamos a true, si no, se queda en false.
                }
            } catch (SQLException ex) {
                Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return salida;
    }

    //Añade la id del directorio a partir del File, hay que pasarle el repositorio para que haga la consulta.
    void setIdDir(File file, Repositorio rep) {
        String raiz = cj.getApp().getDirectory();
        String directorioEnDB = Repositorio.adaptarNombreAFormatoDB(raiz, file.getParent());
        long idDir = getIdDir(directorioEnDB);
        
    }
 public void saveArchivo(Archivo ar, File file) {
        System.out.println("Grabando "+ar.getNombre());
        try {
            //Comprobar si existe directorio.
            FileInputStream fis=new FileInputStream(file);
            String sqlArchivo="INSERT INTO archivos(nombre, id_dir, binario) VALUES (?,?,?)";
            PreparedStatement pst=con.prepareStatement(sqlArchivo);
            pst.setString(1, ar.getNombre());
            pst.setLong(2, ar.getIdDir());
            pst.setBinaryStream(3, fis);
            pst.execute();
            pst.close();
            fis.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
 // Metodo que se ejecuta al crear el Thread.

}
