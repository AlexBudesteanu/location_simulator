package com.laird.geotracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Hello world!
 *
 */
public class App extends Application {
	
    public static void main( String[] args )
    {
        launch(args);
    }

	@Override
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/Scene.fxml"));
		
		Scene scene = new Scene(root);
        
        primaryStage.setTitle("GeoTracker");
        primaryStage.setScene(scene);
        primaryStage.show();
	}
}
