module com.mycompany.bbddconnector {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    opens com.mycompany.bbddconnector to javafx.fxml;
    exports com.mycompany.bbddconnector;
    requires javafx.graphicsEmpty;

    
    
}
