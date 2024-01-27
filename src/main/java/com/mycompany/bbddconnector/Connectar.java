/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bbddconnector;

import java.sql.*;
/**
 *
 * @author alex
 */
public class Connectar {

    private String url = "";
    private String usuario = "root";
    private String contraseña = "";
    private Connection miCon;

    public Connection connect(String url) {
        this.url = url;
        try {

            //PRIMER PASO, CREAMOS CONEXIÓN
            miCon = DriverManager.getConnection(url, usuario, contraseña);
        } catch (SQLException ex) {
           miCon = null;
        }
        return miCon;
    }

}
