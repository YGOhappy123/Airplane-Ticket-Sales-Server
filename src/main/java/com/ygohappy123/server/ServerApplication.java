package com.ygohappy123.server;

import com.ygohappy123.server.controllers.MainController;
import io.github.palexdev.materialfx.css.themes.MFXThemeManager;
import io.github.palexdev.materialfx.css.themes.Themes;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.paint.Color;

public class ServerApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MFXResourcesLoader.loadURL("fxml/main-view.fxml"));
        loader.setControllerFactory(c -> new MainController(primaryStage));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setTitle("PTIT - Airplane Ticket Sales");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app-logo.png")));
        primaryStage.show();
    }

    @Override
    public void stop() {

    }
}