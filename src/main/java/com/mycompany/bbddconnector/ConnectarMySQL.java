/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bbddconnector;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author alex
 */
public class ConnectarMySQL {

    private String url = "";
    private String user = "";
    private String passwd = "";
    private Connection miCon;
   
    
    public Connection connect(String user, String passwd, String url) {
        
        this.user = user;
        this.passwd = passwd;
        this.url = url;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            //PRIMER PASO, CREAMOS CONEXIÓN
            miCon = DriverManager.getConnection(url, user, passwd);
        } catch (SQLException ex) {
            ex.printStackTrace();
            miCon = null;

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConnectarMySQL.class.getName()).log(Level.SEVERE, null, ex);
        }
        return miCon;
    }
    
}
