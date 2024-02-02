package com.mycompany.bbddconnector;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
import javafx.event.ActionEvent;
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
    private Button btnConnect, btnExec, btnSelect, btnInsert, btnUpdate, btnDelete;

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
    private DatabaseMetaData metaData;
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
        btnSelect.setDisable(true);
        btnInsert.setDisable(true);
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
        comboBBDD.setDisable(true);
        comboTablas.setDisable(true);
        textSente.setDisable(true);

        //CONFIGURACIÓN PARA PRUEBAS
        textUser.setText("anonymous");
        textPasswd.setText("root");
        textUrlBbdd.setText("localhost");
        //CONFIGURACIÓN PARA PRUEBAS

        comboBBDD.setOnAction(EventType -> {
            if (comboBBDD.getValue() != null) {
                comboTablas.setDisable(false);
                mostrarTablas();
            }
        });

        comboTablas.setOnAction((event) -> {
            if (comboTablas.getValue() != null && comboBBDD.getValue() != null) {
                bbddSelectedString = comboBBDD.getValue().toString();
                tablaSelectedString = comboTablas.getValue().toString();
                textSente.setText("SELECT * FROM " + bbddSelectedString + "." + tablaSelectedString);
                btnExec.setDisable(false);
                btnSelect.setDisable(false);
                btnInsert.setDisable(false);
                btnUpdate.setDisable(false);
                btnDelete.setDisable(false);
                textSente.setDisable(false);
                execSetencia();
            }
        });

        String[] listaTiposBBDD = {"MySQL", "PostgreSQL", "OracleDB"};
        comboTiposBBDD.getItems().addAll(listaTiposBBDD);
        comboTiposBBDD.setPromptText("Selecione un tipo.");

        comboTiposBBDD.setOnAction(event -> {
            this.tipoBBDD = comboTiposBBDD.getValue().toString();
        });

    }

    public void btnConnectar() {

        if (btnConnect.getText().equals("CONECTAR")) {
            connect();
            if (miCon != null) {
                lblEstado.setText("ESTADO: CONECTADO");
                btnConnect.setText("DESCONECTAR");
                comboTiposBBDD.setDisable(true);
                textUser.setDisable(true);
                textPasswd.setDisable(true);
                textUrlBbdd.setDisable(true);
                comboBBDD.setDisable(false);
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
            btnSelect.setDisable(true);
            btnInsert.setDisable(true);
            btnUpdate.setDisable(true);
            btnDelete.setDisable(true);
            comboTiposBBDD.setDisable(false);
            comboTablas.setDisable(true);
            comboBBDD.setDisable(true);
            textSente.setDisable(true);
            textUser.setDisable(false);
            textPasswd.setDisable(false);
            textUrlBbdd.setDisable(false);
        }
    }

    public void connect() {

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

                case "OracleDB":
                    url = "jdbc:oracle:thin:@" + textUrlBbdd.getText() + ":1521";
                    connectOracleDB(url);
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
//                setAlert("Error en la conexión, vuelva a intentarlo o asegurese que el servidor está funcionando.");
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
        try {
            String user = textUser.getText();
            String passwd = textPasswd.getText();

            //Creamos la conexión con la bbdd.
            miCon = new ConnectarPostgreSQL().connect(user, passwd, url);

            //comprobamos que la conexión ha sido exitosa.
            if (miCon == null) {
//                setAlert("Error en la conexión, vuelva a intentarlo o asegurese que el servidor está funcionando.");
                return;
            }
            metaData = miCon.getMetaData();
            miResultSet = metaData.getSchemas();

            listaBaseDatos = new ArrayList<>();

            while (miResultSet.next()) {
                String schemaName = miResultSet.getString("TABLE_SCHEM");
                listaBaseDatos.add(schemaName);
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
            System.out.println("TETETET: "+ex.getCause());
        }
    }

    public void connectOracleDB(String url) {

        try {
            String user = textUser.getText();
            String passwd = textPasswd.getText();

            //Creamos la conexión con la bbdd.
            miCon = new ConnectarOracleDB().connect(user, passwd, url);

            //comprobamos que la conexión ha sido exitosa.
            if (miCon == null) {
//                setAlert("Error en la conexión, vuelva a intentarlo o asegurese que el servidor está funcionando.");
                return;
            }

            //Declaramos el objeto Statement.
            miStatement = miCon.createStatement();

            miResultSet = miStatement.executeQuery("SELECT table_name FROM user_tables");

            listaBaseDatos = new ArrayList<>();

            while (miResultSet.next()) {
                String schemaName = miResultSet.getString(1);
                listaBaseDatos.add(schemaName);
            }

            if (listaBaseDatos.size() == 0 | listaBaseDatos == null) {
                setAlert("No hay ninguna BBDD disponible en el servidor.");
                return;
            }

            comboBBDD.setPromptText("Seleccione una BBDD");
            comboBBDD.getItems().addAll(user);
            comboTablas.setPromptText("Seleccione una BBDD");

        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public boolean desconectar() {

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

            miStatement = miCon.createStatement();

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

    public void mostrarTablas() {
        String nombreBBDD = comboBBDD.getValue().toString();
        switch (comboTiposBBDD.getValue().toString()) {
            case "MySQL":
                tablaMySQL(nombreBBDD);
                break;
            case "PostgreSQL":
                tablaPostgreSQL(nombreBBDD);
                break;
            case "OracleDB":
                comboTablas.getItems().addAll(listaBaseDatos);
                break;
            default:
                throw new AssertionError();
        }

    }

    public void tablaMySQL(String nombreBBDD) {
        try {
            miStatement.execute("USE " + nombreBBDD);
            miResultSet = miStatement.executeQuery("SHOW TABLES FROM " + nombreBBDD);
            ArrayList<String> tablaList = new ArrayList<>();

            while (miResultSet.next()) {
                tablaList.add(miResultSet.getNString(1));
            }
            comboTablas.getItems().clear();
            comboTablas.getItems().addAll(tablaList);
        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void tablaPostgreSQL(String nombreBBDD) {
        try {

            boolean tablaVacia = true;
            ArrayList<String> tablaList = new ArrayList<>();

            comboTablas.getItems().clear();
            miResultSet = metaData.getTables(null, nombreBBDD, null, new String[]{"TABLE"});

            while (miResultSet.next()) {
                tablaList.add(miResultSet.getString("TABLE_NAME"));
                if (!tablaList.isEmpty()) {
                    tablaVacia = false;
                }
            }

            if (tablaVacia) {
                setAlert("Esta base de datos, no dispone de tablas.");
                return;
            }
            comboTablas.getItems().addAll(tablaList);
        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void setAlert(String msg) {

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("¡AVISO!");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    public void btnTipos(ActionEvent event) {

        try {
            Button btn = (Button) event.getSource();
            String btnType = btn.getText();
            miStatement = miCon.createStatement();
            String query = "SELECT * FROM " + bbddSelectedString + "." + tablaSelectedString;
            miResultSet = miStatement.executeQuery(query);
            ResultSetMetaData metaData = miResultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            switch (btnType) {
                case "SELECT":
                    textSente.setText("SELECT * FROM " + bbddSelectedString + "." + tablaSelectedString);
                    break;
                case "INSERT":

                    String attriName = "";
                    String attriNameValue = "";
                    for (int i = 1; i <= columnCount; i++) {
                        if (i < columnCount) {
                            attriName += metaData.getColumnName(i) + ",";
                            attriNameValue += "valor" + metaData.getColumnName(i) + ",";
                        } else {
                            attriName += metaData.getColumnName(i);
                            attriNameValue += "valor" + metaData.getColumnName(i);
                        }
                    }
                    textSente.setText("INSERT INTO " + bbddSelectedString + "." + tablaSelectedString + " (" + attriName + ") VALUES (" + attriNameValue + ")");

                    break;

                case "UPDATE":
                    String setsString = "";
                    for (int i = 1; i <= columnCount; i++) {
                        if (i < columnCount) {
                            setsString += metaData.getColumnName(i)+"=?, ";
                        } else {
                            setsString += metaData.getColumnName(i)+"=?";
                        }
                    }
                    textSente.setText("UPDATE " + bbddSelectedString + "." + tablaSelectedString + " SET "+setsString+" WHERE <condition>");
                    break;

                case "DELETE":

                    textSente.setText("DELETE FROM " + bbddSelectedString + "." + tablaSelectedString + " WHERE <SENTENCIA>");
                    break;
                default:
                    throw new AssertionError();
            }
        } catch (SQLException ex) {
            Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
