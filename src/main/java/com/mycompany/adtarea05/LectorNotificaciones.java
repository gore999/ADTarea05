/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.adtarea05;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

/**
 *
 * @author Carlos
 */
public class LectorNotificaciones implements Runnable {

    private Connection con;
    String dir = null;
    private ConexionJson cj;
    private int milis;
    PGConnection pgconn;

    public LectorNotificaciones(ConexionJson cj, int milis) {
        this.cj = cj;
        this.milis = milis;

        //Recuperar datos del json.
        String address = cj.getDb().getAddress();
        String dbName = cj.getDb().getName();
        String user = cj.getDb().getUser();
        String pass = cj.getDb().getPassword();
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);
        String conectarStr = "jdbc:postgresql://" + address + "/" + dbName;
        try {
            con = DriverManager.getConnection(conectarStr, props);
            //Suscripcion al canal
            pgconn = con.unwrap(PGConnection.class);
            Statement stmt = con.createStatement();
            stmt.execute("LISTEN nuevoarchivo");
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(Repositorio.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        while (true) {
            System.out.println("Comprobando notificaciones");
            try {
                String sql="Select * from archivos where id=?";
                PGNotification notificaciones[] = pgconn.getNotifications();
                if(notificaciones!=null){
                    for(PGNotification not:notificaciones){
                        String nombre;
                        int id_dir;
                        byte[] datos;
                        
                        PreparedStatement pst=con.prepareStatement(sql);
                        int id=Integer.parseInt(not.getParameter());
                        System.out.println("Notificado"+id);
                        pst.setInt(1, id);
                        ResultSet rs=pst.executeQuery();
                        while(rs.next()){
                            //Recuperar datos.
                            nombre=rs.getString("nombre");
                            id_dir=rs.getInt("id_dir");
                            datos=rs.getBytes("binario");
                            String directorio=recuperarDirectorio(id_dir);
                            File f=new File(directorio+File.separator+nombre);//Creo File con la ruta completa del archivo.
                            if(!f.exists()){//Si el archivo no existe, lo recuperamos.
                                System.out.println("Restaurando");
                                FileOutputStream fos=new FileOutputStream(f);
                                fos.write(datos);//Escribimos los datos recuperados.
                                fos.close();
                            }else{
                                System.out.println("Ya existe el archivo, no necesita reataurar.");
                            }
                        }
                        //Cierres.
                        pst.close();
                        rs.close();
                    }
                }
                Thread.sleep(milis);//dormimos el hilo 
            } catch (SQLException ex) {
                Logger.getLogger(LectorNotificaciones.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(LectorNotificaciones.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LectorNotificaciones.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LectorNotificaciones.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }

    private String recuperarDirectorio(long id_dir) {
        String salida="";
        String sql="SELECT nombre FROM directorios where id=?";
        try {
            PreparedStatement pst=con.prepareStatement(sql);
            pst.setLong(1, id_dir);
            ResultSet rs=pst.executeQuery();
            while(rs.next()){
                String nombre=rs.getString("nombre");
                salida=Repositorio.adaptarDBAPathReal(cj.getApp().getDirectory(), nombre);
            }
            //Cierres
            pst.close();
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(LectorNotificaciones.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return salida;
    }

}
