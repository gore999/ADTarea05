/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.adtarea05;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
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
        //Crear funcion y trigger:
        crearFuncionYTrigger();
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

    public void saveDir(Directorio dir) {
        String raiz = cj.getApp().getDirectory();
        String nombreReal = dir.getNombre();
        String nombreAdaptado = adaptarNombreAFormatoDB(raiz, nombreReal);
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
                System.out.print(dir.getNombre()+"Ya existe");
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
            System.out.println("Datos del archivo 110: "+ar.getNombre()+"--"+ar.getIdDir());
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
    public static String adaptarNombreAFormatoDB(String raiz, String real) {
        String salida = ".";
        salida += real.substring(raiz.length());
        return salida;
    }

//Cambia el . por el directorio raiz del json.
    public static String adaptarDBAPathReal(String raiz, String pathAlmacenado) {
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
        if (archivo.getIdDir() != -1) {//Si el archivo existe: Creamos directorio y reponemos archivo.
            String sqlArchivo = "SELECT count(*) FROM archivos where  nombre=? AND id_dir=?";
            try {
                PreparedStatement pst = con.prepareStatement(sqlArchivo);
                pst.setString(1, archivo.getNombre());
                pst.setLong(2, archivo.getIdDir());
                ResultSet rs = pst.executeQuery();
                rs.next();
                if(rs.getInt(1)==1)salida=true;// Si hay algun resultado, cambiamos a true, si no, se queda en false.
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

    void recuperaDirectoriosYArchivos() {
        String sql="SELECT id,nombre FROM directorios";
        String sqlArchivos="Select * FROM archivos WHERE id_dir=?";
        try {
            Statement st=con.createStatement();
            ResultSet rs=st.executeQuery(sql);
            while(rs.next()){//Leer cada directorio. 
                String dir=rs.getString("nombre");
                long id=rs.getLong("id");
                dir= this.adaptarDBAPathReal(cj.getApp().getDirectory(), dir);// Adaptar el nombre almacenado a nombre de path real
                System.out.println("EL dir a recuoerar es:"+dir);
                File f= new File(dir); //Creamos un objeto file para comprobar si el directorio existe. 
                if(!f.exists()){//Si no existe, lo creamos (con mkdirs, por si hay que crear tambien los directorios padre.
                    System.out.println(f.getCanonicalPath());
                    System.out.println(f.mkdirs());
                   
                }else{
                    System.out.println("el directorio "+dir+" existe");
                }
                //Recuperar los archivos del directorio.
                PreparedStatement pst=con.prepareStatement(sqlArchivos);
                pst.setLong(1,id);//Seleccionamos todos los archivos que tengan como id de directorio en BD la del directorio que estámos analizando.
                ResultSet rsArch=pst.executeQuery();
                while(rsArch.next()){
                    String rutaArchivo=dir+"\\"+rsArch.getString("nombre");
                    File f2=new File(rutaArchivo);
                    //System.out.print("Path: "+f2.getPath());
                    if(!f2.exists()){//Si el archivo no existe en el sistema, creamos el fichero.
                        System.out.println("f2"+f2.getParent());
                        f2.createNewFile();
                        FileOutputStream fos=new FileOutputStream(f2);
                        fos.write(rsArch.getBytes("binario"));//Escritura
                        fos.close();
                    }else{
                       // System.out.println("El archivo ya existe zzzzzz");
                    }
                    
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void crearFuncionYTrigger() {
        String sqlFunction="CREATE OR REPLACE FUNCTION notificar_archivo() "+
                "RETURNS trigger AS $$ "
                + "BEGIN "
                + " PERFORM pg_notify('nuevoarchivo',NEW.id::text); "
                + "RETURN NEW; "
                + "END; "
                + "$$ LANGUAGE plpgsql;";
         String sqlTrigger=" DROP TRIGGER IF EXISTS not_nuevo_arch ON archivos;"
                 + "CREATE TRIGGER not_nuevo_arch "
                 + "AFTER INSERT ON archivos "
                 + "FOR EACH ROW "
                 + "EXECUTE PROCEDURE notificar_archivo();";       
        try {
            CallableStatement callFun=con.prepareCall(sqlFunction);
            callFun.execute();
            callFun.close();
            CallableStatement callTrig=con.prepareCall(sqlTrigger);
            callTrig.execute();
            callTrig.close();
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }
         
    }
    
}
