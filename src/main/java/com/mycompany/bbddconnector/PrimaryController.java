package com.mycompany.bbddconnector;

import java.net.URL;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class PrimaryController implements Initializable {

    @FXML
    private ComboBox comboBBDD, comboTablas;

    @FXML
    private Button btnConnect, btnExec;

    @FXML
    private TextField textUrlBbdd, textSente;

    @FXML
    private TableView<ObservableList<String>> tableView;
    
    @FXML
    private Pane panel;

    private ArrayList<String> listaBaseDatos;
    private Connection miCon;
    private Statement miStatement;
    private ResultSet miResultSet;
    private String bbddSelectedString;
    private String tablaSelectedString;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        tableView.setPrefWidth(Double.MAX_VALUE);
//        tableView.setPrefHeight(Double.MAX_VALUE);

        comboBBDD.setPromptText("Debe conectar primero.");
        comboTablas.setPromptText("Debe conectar primero.");
        btnExec.setDisable(true);
        textUrlBbdd.setText("localhost:3306");

        comboBBDD.setOnAction(EventType -> {
            bbddSelectedString = comboBBDD.getValue().toString();
            mostrarTablas(bbddSelectedString);
        });

        comboTablas.setOnAction((event) -> {

            if (comboTablas.getValue() != null && comboBBDD.getValue() != null) {
                tablaSelectedString = comboTablas.getValue().toString();
                textSente.setText("SELECT * FROM " + tablaSelectedString);
            }
        });
    }

    public void connectBBDD() throws SQLException {

        String url = "jdbc:mysql://" + textUrlBbdd.getText() + "/";
        //Creamos la conexión con la bbdd.
        miCon = new Connectar().connect(url);

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

        btnConnectar();

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

    public boolean desconectar() {
        boolean respuesta;
        try {
            miCon.close();
            respuesta = true;
        } catch (SQLException ex) {
            respuesta = false;
        }
        return respuesta;
    }

    public void btnConnectar() {
        if (btnConnect.getText().equals("CONECTAR")) {
            if (miCon != null) {
                btnConnect.setText("DESCONECTAR");
                btnExec.setDisable(false);
            }
        } else {
            boolean respuesta = desconectar();

            if (!respuesta) {
                setAlert("No se ha desconectado correctamente.");
                return;
            }
            textUrlBbdd.setText("");
            comboBBDD.getItems().clear();
            comboBBDD.setPromptText("Debe conectar primero.");
            comboTablas.getItems().clear();
            comboTablas.setPromptText("Debe conectar primero.");
            btnConnect.setText("CONECTAR");
            btnExec.setDisable(true);
        }
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

    public void setAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("¡AVISO!");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
