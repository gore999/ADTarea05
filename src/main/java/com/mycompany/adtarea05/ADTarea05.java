/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.adtarea05;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos
 */
public class ADTarea05 {

    public static void main(String[] args) {
        //Preparar variables-----------------------------------------
        ConexionJson cj = conexionFromJson();//Lectura del json con metodo estatico al efecto.
        cj.getApp().setDirectory(cj.getApp().getDirectory().replaceAll("/", File.separator)); // Convertimos el separador usado en el json el en separador para sistemas windows.
        
//Obtener un repositorio, objeto destinado a tratar con datos.
        Repositorio rep = Repositorio.getInstance(cj);

        //PASO 2: Primero leemos los directorios e insertarlos en base de datos. 
        File f = new File(cj.getApp().getDirectory()); // Creamos el primer directorio, el raiz, desde el que leemos todos los datos. 
        //RECUPERAR DE DB Y RESTAURAR LO QUE NO EXISTA
        //Crear funcion y trigger:
        rep.crearFuncionYTrigger();// Creamos la funcion y el trigger.
        rep.recuperaDirectoriosYArchivos(); //Recuperamos los archivos y directorios

    //LECTURA DESDE DISCO Y GRABADO EN DB
        leerDirectorios(rep, f);//Metodo recursivo para leer los directorios.
        leerArchivos(rep, f);//metodo recursivo para leer los archivos.
        //Thread que comprueba y graba archivos cada 5000 milisegundos.
        Thread threadSaver=new Thread(new ComprobadorFiles(cj,5000)); //cada 5 segundos se comprueban los files.
        //Thread que comprueba notificaciones cada 5000 milisegundos
        Thread threadLectorNotif=new Thread(new LectorNotificaciones(cj,5000));
        //Iniciar los threads
        threadSaver.start();
        threadLectorNotif.start();

    }

    /*
      Metodo que devuelve un objeto ConexionJson. Lee el json de configuracion y devuelve el objeto que lo representa. 
     */
    private static ConexionJson conexionFromJson() {
        FileReader json = null;
        Repositorio rep = null;
        ConexionJson cj = null;
        try {
            Gson g = new Gson();
            File f = new File("conexion.json");//File que representa al json
            json = new FileReader(f);
            cj = g.fromJson(json, ConexionJson.class);//Crear el objeto a partir de datos json.
            File f2 = new File(cj.getApp().getDirectory());//Crear iun
            cj.getApp().setDirectory(f2.getCanonicalPath());
            System.out.println("Directorio" + cj.getApp().getDirectory());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ADTarea05.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ADTarea05.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                json.close();
            } catch (IOException ex) {
                Logger.getLogger(ADTarea05.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return cj;
    }
    //Lee y graba de forma recursiva los directorios bajo el file que se le pasa como parametro. 
    private static void leerDirectorios(Repositorio rep, File file) {//Reibe repositorio, File del raiz del json, arraylist directorios.
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
            if (!rep.existeDir(dir.getNombre())) {//Si no existe el directorio, se guarda.
                rep.saveDir(dir);
            }
            //RECURSIVIDAD----------
            File[] innerFiles = file.listFiles();// Obtener un array de File con los Files dentro del directorio
            for (File eachFile : innerFiles) {
                leerDirectorios(rep, eachFile);//Aplicamos este mismo metodo a cada uno de los files de forma recursiva
            }
        }
    }
 //Lee y graba de forma recursiva todos los archivos bajo el file que recibe.
    private static void leerArchivos(Repositorio rep, File file) {
        if (file.isDirectory()) {//Si es un directorio, aplicamos Recursivamente este mismo metodo a todo su contenido.
            File[] innerFiles = file.listFiles();// Obtener un array de File con los Files dentro del directorio
            for (File eachFile : innerFiles) {
                leerArchivos(rep, eachFile);//Aplicamos este mismo metodo
            }
        } else {// Es archivo. Creamos un objeto de la clase archivo para abstraer el codigo.
            Archivo ar = new Archivo();
            ar.setNombre(file.getName());
            ar.setIdDir(file, rep);

            if (!rep.existeArchivo(ar)) {
                rep.saveArchivo(ar, file);
            }
        }

    }

}
