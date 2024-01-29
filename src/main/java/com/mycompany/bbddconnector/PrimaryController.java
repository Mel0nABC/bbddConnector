package com.mycompany.bbddconnector;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class PrimaryController implements Initializable {

    @FXML
    private ComboBox comboBBDD, comboTablas, comboTiposBBDD;

    @FXML
    private Button btnConnect, btnExec;

    @FXML
    private TextField textUrlBbdd, textSente, textUser, textPasswd;

    @FXML
    private Label lblEstado;

    @FXML
    private TableView<ObservableList<String>> tableView;

    private ArrayList<String> listaBaseDatos;
    private Connection miCon;
    private Statement miStatement;
    private ResultSet miResultSet;
    private String bbddSelectedString;
    private String tablaSelectedString;
    private String tipoBBDD;
    private Task tarea;
    private static Thread hiloConexion;
    private String user = "";
    private String passwd = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboBBDD.setPromptText("Debe conectar primero.");
        comboTablas.setPromptText("Debe conectar primero.");
        btnExec.setDisable(true);
        comboBBDD.setDisable(true);
        comboTablas.setDisable(true);
        textSente.setDisable(true);

        //CONFIGURACIÓN PARA PRUEBAS
        textUser.setText("root");
        textPasswd.setText("root");
        textUrlBbdd.setText("localhost");
        //CONFIGURACIÓN PARA PRUEBAS

        comboBBDD.setOnAction(EventType -> {
            if (comboBBDD.getValue() != null) {
                comboTablas.setDisable(false);
                bbddSelectedString = comboBBDD.getValue().toString();
                mostrarTablas(bbddSelectedString);
            }
        });

        comboTablas.setOnAction((event) -> {
            if (comboTablas.getValue() != null && comboBBDD.getValue() != null) {
                tablaSelectedString = comboTablas.getValue().toString();
                textSente.setText("SELECT * FROM " + tablaSelectedString);
            }
        });

        String[] listaTiposBBDD = {"MySQL", "PostgreSQL"};
        comboTiposBBDD.getItems().addAll(listaTiposBBDD);
        comboTiposBBDD.setPromptText("Selecione un tipo.");
        
        comboTiposBBDD.setOnAction(event -> {
            this.tipoBBDD = comboTiposBBDD.getValue().toString();
        });

    }

    public void btnConnectar() {

        if (btnConnect.getText().equals("CONECTAR")) {
            if (miCon == null) {
                lblEstado.setText("ESTADO: CONECTADO");
                btnConnect.setText("DESCONECTAR");
                comboTiposBBDD.setDisable(true);
                comboBBDD.setDisable(false);
                btnExec.setDisable(false);
                textSente.setDisable(false);
                connect();
            }
        } else {
            boolean respuesta = desconectar();

            if (!respuesta) {
                setAlert("No se ha desconectado correctamente.");
                return;
            }

            lblEstado.setText("ESTADO: DESCONECTADO");
            comboBBDD.getItems().clear();
            comboBBDD.setPromptText("Debe conectar primero.");
            comboTablas.getItems().clear();
            comboTablas.setPromptText("Debe conectar primero.");
            btnConnect.setText("CONECTAR");
            btnExec.setDisable(true);
            comboTiposBBDD.setDisable(false);
            comboTablas.setDisable(true);
            comboBBDD.setDisable(true);
            textSente.setDisable(true);
        }
    }

    public void connect() {
        System.out.println("CONNECT()");
        
        try {
            if (textUrlBbdd.getText().equals("") | textUrlBbdd.getText() == null) {
                setAlert("Debe indicar un servidor al cual conectar.");
                textUrlBbdd.requestFocus();
                return;
            }

            if (tipoBBDD == null) {
                setAlert("Debe seleccionar un tipo de base de datos a la cual conectar.");
                comboTiposBBDD.requestFocus();
                return;
            }

            user = textUser.getText();
            passwd = textPasswd.getText();
            String url = "";

            switch (tipoBBDD) {
                case "MySQL":
                    url = "jdbc:mysql://" + textUrlBbdd.getText() + ":3306/";
                    connectMysql(url);
                    break;

                case "PostgreSQL":
                    url = "jdbc:postgresql://" + textUrlBbdd.getText() + ":5432/";
                    connectPostgreeSQL(url);
                    break;
            }

            //Tarea para ir comprobando que la conexión está establecida.
            if (miCon != null) {
                if (miCon.isValid(2)) {
                    tarea = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            boolean bucle = true;
                            while (bucle) {

                                boolean conexion = miCon.isValid(1);
                                //Tiempo cada cuánto se comprueba la conexión.
                                TimeUnit.SECONDS.sleep(3);
                                if (!conexion) {
                                    setAlert("Se ha perdido la conexión con el servidor.");
                                    bucle = false;
                                    btnConnectar();
                                    PrimaryController.stopThreadConexion();
                                }
                            }
                            return null;
                        }
                    };
                    hiloConexion = new Thread(tarea);
                    hiloConexion.setDaemon(true);
                    hiloConexion.setName("conexionThread");
                    hiloConexion.start();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void stopThreadConexion() {
        Platform.runLater(() -> {
            PrimaryController.hiloConexion.interrupt();
        });

    }

    public void connectMysql(String url) {

        try {
            String user = textUser.getText();
            String passwd = textPasswd.getText();

            //Creamos la conexión con la bbdd.
            miCon = new ConnectarMySQL().connect(user, passwd, url);

            //comprobamos que la conexión ha sido exitosa.
            if (miCon == null) {
                setAlert("Error en la conexión, vuelva a intentarlo o asegurese que el servidor está funcionando.");
                return;
            }

            //Declaramos el objeto Statement.
            miStatement = miCon.createStatement();

            //Obtenemos la lista de bbdd disponibles en el servidor.
            miResultSet = miStatement.executeQuery("SHOW DATABASES");

            listaBaseDatos = new ArrayList<>();
            //Creamos un array con los nombres de las bbdd.
            while (miResultSet.next()) {
                listaBaseDatos.add(miResultSet.getNString(1));
            }

            if (listaBaseDatos.size() == 0 | listaBaseDatos == null) {
                setAlert("No hay ninguna BBDD disponible en el servidor.");
                return;
            }

            comboBBDD.setPromptText("Seleccione una BBDD");
            comboBBDD.getItems().addAll(listaBaseDatos);
            comboTablas.setPromptText("Seleccione una BBDD");

        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void connectPostgreeSQL(String url) {
System.out.println("connectPostgreeSQL()");
//        try {
        String user = textUser.getText();
        String passwd = textPasswd.getText();

        //Creamos la conexión con la bbdd.
        miCon = new ConnectarPostgreSQL().connect(user, passwd, url);

        //comprobamos que la conexión ha sido exitosa.
        if (miCon == null) {
            setAlert("Error en la conexión, vuelva a intentarlo o asegurese que el servidor está funcionando.");
            return;
        }

//            //Declaramos el objeto Statement.
//            miStatement = miCon.createStatement();
//
//            //Obtenemos la lista de bbdd disponibles en el servidor.
//            miResultSet = miStatement.executeQuery("SHOW DATABASES");
//
//            listaBaseDatos = new ArrayList<>();
//            //Creamos un array con los nombres de las bbdd.
//            while (miResultSet.next()) {
//                listaBaseDatos.add(miResultSet.getNString(1));
//            }
//
//            if (listaBaseDatos.size() == 0 | listaBaseDatos == null) {
//                setAlert("No hay ninguna BBDD disponible en el servidor.");
//                return;
//            }
//
//            comboBBDD.setPromptText("Seleccione una BBDD");
//            comboBBDD.getItems().addAll(listaBaseDatos);
//            comboTablas.setPromptText("Seleccione una BBDD");

//        } catch (SQLException ex) {
//            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public boolean desconectar() {
        
        System.out.println("desconectar()");
        boolean respuesta = false;
        try {

            if (miStatement != null) {
                miStatement.close();
                miStatement = null;
            }
            if (miResultSet != null) {
                miResultSet.close();
                miResultSet = null;
            }

            if (miCon != null) {
                miCon.close();
                miCon = null;
                System.out.println("cerrado en desconectar.");
            }

            if (miCon == null) {
                tableView.getItems().clear();
                tableView.getColumns().clear();
                PrimaryController.stopThreadConexion();
                respuesta = true;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            respuesta = false;
        }
        return respuesta;

    }

    public void execSetencia() {
        try {

            tableView.getItems().clear();
            tableView.getColumns().clear();

            String query = textSente.getText();

            miResultSet = miStatement.executeQuery(query);

            ResultSetMetaData metaData = miResultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                final int j = i - 1;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));

                // Configuración de celda de fábrica
                column.setCellValueFactory(param -> {
                    ObservableList<String> row = param.getValue();
                    return new SimpleStringProperty(row.get(j));
                });

                tableView.getColumns().add(column);
            }

            // Ahora llenamos la tabla con datos del ResultSet
            while (miResultSet.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(miResultSet.getString(i));
                }
                tableView.getItems().add(row);
            }
        } catch (SQLException ex) {
//            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
            setAlert("La tabla seleccionada no corresponde a la base de datos " + bbddSelectedString);
        }

    }

    public void mostrarTablas(String bbddSelected) {
        try {
            miStatement.execute("USE " + bbddSelected);
            miResultSet = miStatement.executeQuery("SHOW TABLES FROM " + bbddSelected);
            ArrayList<String> tablaList = new ArrayList<>();

            while (miResultSet.next()) {
                tablaList.add(miResultSet.getNString(1));
            }

            Platform.runLater(() -> {
                comboTablas.getItems().clear();
                comboTablas.getItems().addAll(tablaList);
            });
        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setAlert(String msg) {

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("¡AVISO!");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

}
