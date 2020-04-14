/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.adtarea05;

import java.io.File;

/**
 *
 * @author Carlos
 */

/*
* Esta clase representa los datos recogidos del json de configuracion.
* Contiene dos instancias de sendas clases  internas: DbConnection y App 
 */
public class ConexionJson {

    private DbConnection dbConnection;
    private App app;

    public ConexionJson() {
    }

    public ConexionJson(DbConnection db, App app) {
        this.dbConnection = db;
        this.app = app;
    }

    public DbConnection getDb() {
        return dbConnection;
    }

    public void setDb(DbConnection db) {
        this.dbConnection = db;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public String toString() {
        return "Connection{" + "db=" + dbConnection + ", app=" + app + '}';
    }

    /// Clases privadas internas
    static class DbConnection {

        String address;
        String name;
        String user;
        String password;

        public DbConnection() {
            String address = null;
            String name = null;
            String user = null;
            String password = null;
        }

        public DbConnection(String address, String name, String user, String password) {
            this.address = address;
            this.name = name;
            this.user = user;
            this.password = password;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "DbConnection{" + "address=" + address + ", name=" + name + ", user=" + user + ", password=" + password + '}';
        }

    }

    static class App {

        String directory;

        public App() {
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {

            this.directory = directory;
        }

        @Override
        public String toString() {
            return "App{" + "directory=" + directory + '}';
        }

    }

}
