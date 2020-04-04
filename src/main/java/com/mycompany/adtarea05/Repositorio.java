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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos
 */
public class Repositorio {

    private static Repositorio rep;
    private Connection con;
    String dir = null;
    private ConexionJson cj;

    //Constructor privado
    private Repositorio(ConexionJson cj) {
        //DB configuracion
        this.cj = cj;
        String address = cj.getDb().getAddress();
        String dbName = cj.getDb().getName();
        String user = cj.getDb().getUser();
        String pass = cj.getDb().getPassword();
        //Directorio
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

    public static Repositorio getInstance(ConexionJson cj) {
        if (rep == null) {
            rep = new Repositorio(cj);
        }
        return rep;
    }

    public int contaFilasDir() {
        String sql = "Select count(*) from directorios";
        int salida = -1;
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            salida = rs.getInt(1);
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
        return salida;
    }

    public void showDirectorios() {
        String sql = "Select * from directorios";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString("nombre"));
            }
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void saveDir(Directorio dir) {
        String raiz = cj.getApp().getDirectory();
        String nombreReal = dir.getNombre();
        String nombreAdaptado = adaptarNombreAFormatoDB(raiz, nombreReal);
        System.out.println("id recuperada" + getIdDir(nombreAdaptado));
        try {

            if (getIdDir(nombreAdaptado) == -1) {//Si no existe el directorio, lo guardamos. El directorio no existe si el metodo de recuperar id devuelve -1
                String sqlInsert = "Insert into directorios(nombre) values (?)";
                PreparedStatement pst = con.prepareStatement(sqlInsert);
                pst = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
                System.out.println(nombreAdaptado);
                pst.setString(1, nombreAdaptado);
                pst.execute();
                //Obtener las keys e insertarlas en 
                ResultSet rsk = pst.getGeneratedKeys();
                rsk.next();
                int key = rsk.getInt(1);
                dir.setId(key);
                System.out.println("Key: " + dir.getId());
            } else {
                System.out.println("Ya existe");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveArchivo(Archivo ar, File file) {
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

    public ConexionJson getCj() {
        return cj;
    }

    public void setCj(ConexionJson cj) {
        this.cj = cj;
    }

/// Adaptacion de directiros
    //Convierte un directorio al formato que se pide en el ejercicio (empleando . como directorio raiz)
    public String adaptarNombreAFormatoDB(String raiz, String real) {
        String salida = ".";
        salida += real.substring(raiz.length());
        return salida;
    }

//Cambia el . por el directorio raiz del json.
    public String adaptarDBAPathReal(String raiz, String pathAlmacenado) {
        //Quitar el punto
        String salida = pathAlmacenado.substring(1);
        salida = raiz + salida;
        return salida;
    }
//Devuelve un String con el path completo (ya adaptado) del directorio a partir de su id. 

    String getDirectorio(long id) {
        String sql = "SELECT nombre FROM directorios WHERE id=?";
        String salida = null;
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setLong(1, id);
            ResultSet rs = pst.executeQuery();
            rs.next();
            salida = rs.getString(1);
            salida = this.adaptarDBAPathReal(this.cj.getApp().getDirectory(), salida);
            pst.close();
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
        return salida;
    }

    //Devuelve un boleano que indica si un File existe o no. 
    boolean existeArchivo(Archivo archivo) {
        boolean salida = false; //presuponemos que no existe

        if (archivo.getIdDir() != -1) {//Si el directorio existe: Creamos directorio y reponemos archivo.
            String sqlArchivo = "SELECT count(*) FROM archivos where  nombre=? AND id_dir=?";
            try {
                PreparedStatement pst = con.prepareStatement(sqlArchivo);
                pst.setString(1, archivo.getNombre());
                pst.setLong(2, archivo.getIdDir());
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {//Si existe algun archivo con igual id de directorio , se cambia salida a true.
                    salida = true;
                }
            } catch (SQLException ex) {
                Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return salida;
    }

    /*
    Comprueba si existe un directorio. Devuelve boolean 
     */
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

    long getIdDir(String directorioEnDB) {
        long id = -1;
        String cc = "";
        String sql = "Select * from directorios where nombre=?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            System.out.println("Directorio en DB" + directorioEnDB);
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

}
