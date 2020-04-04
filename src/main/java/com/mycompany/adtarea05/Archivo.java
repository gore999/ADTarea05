/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.adtarea05;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos
 */
public class Archivo {
    private long idDir;
    private long id;
    private String nombre;

    public Archivo() {
    }

    public Archivo(long idDir, long id, String nombre) {
        this.idDir = idDir;
        this.id = id;
        this.nombre = nombre;
    }

    public long getIdDir() {
        return idDir;
    }

    public void setIdDir(long idDir) {
        this.idDir = idDir;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
//    public byte[] getBytes(Repositorio rep){
//         String directorio=rep.getDirectorio(this.id);
//         
//         return ;
//    }

    //Añade la id del directorio a partir del File, hay que pasarle el repositorio para que haga la consulta.
    void setIdDir(File file, Repositorio rep) {
        String raiz=rep.getCj().getApp().getDirectory();
        String directorioEnDB=null;
        directorioEnDB=rep.adaptarNombreAFormatoDB(raiz, file.getParent());
        long idDir=rep.getIdDir(directorioEnDB);
        System.out.println("Direndb"+directorioEnDB);
        //directorioEnDB=directorioEnDB.substring(0, );
    }
        
}
