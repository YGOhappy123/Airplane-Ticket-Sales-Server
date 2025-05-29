module com.ygohappy123.server {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires MaterialFX;
    requires java.sql;
    requires org.json;

    opens com.ygohappy123.server to javafx.fxml;
    opens com.ygohappy123.server.controllers to javafx.fxml;

    exports com.ygohappy123.server;
    exports com.ygohappy123.server.controllers;
}