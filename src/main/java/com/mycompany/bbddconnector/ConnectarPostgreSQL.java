/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bbddconnector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 *
 * @author alex
 */
public class ConnectarPostgreSQL {

    private Connection miCon;

    public Connection connect(String user, String passwd, String url) {
        try {

            Class.forName("org.postgresql.Driver");
            //PRIMER PASO, CREAMOS CONEXIÓN
            miCon = DriverManager.getConnection(url, user, passwd);
        } catch (SQLException ex) {
            PrimaryController.setAlert(ex.getMessage());
            miCon = null;

        } catch (ClassNotFoundException ex) {
            System.out.println("error usuario y contraseña");
        }
        return miCon;
    }

}
