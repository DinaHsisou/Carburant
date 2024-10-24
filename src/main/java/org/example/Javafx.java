package org.example;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Javafx extends Application {

    private ProgressBar fuelProgressBar;
    private Label fuelLabel;
    private double fuelAmount = 1.0; // 100% fuel
    private final Object fuelLock = new Object();
    private ExecutorService carThreadPool;
    private VBox carDisplayArea;


    @Override
    public void start(Stage primaryStage) {
        // UI setup
        fuelProgressBar = new ProgressBar(fuelAmount);
        fuelLabel = new Label("Fuel Level: 100%");
        Button addCarButton = new Button("Add Car");
        Button rechargeButton = new Button("Recharge Reservoir");
        TextField rechargeAmountField = new TextField("0.5"); // Default recharge amount
        carDisplayArea = new VBox(10); // Area to display car shapes

        // Add car button action
        addCarButton.setOnAction(e -> addCar());

        // Recharge button action
        rechargeButton.setOnAction(e -> recharge(Double.parseDouble(rechargeAmountField.getText())));

        VBox root = new VBox(10, fuelLabel, fuelProgressBar, addCarButton, rechargeButton, rechargeAmountField, carDisplayArea);
        Scene scene = new Scene(root, 400, 500);

        primaryStage.setTitle("Fuel Reservoir Simulation with Cars");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize car thread pool
        carThreadPool = Executors.newCachedThreadPool();
    }

    private void addCar() {
        // Create car shape (rectangle) and place it in the car display area
        Platform.runLater(() -> {
            Rectangle carShape = createCarShape(Color.BLUE); // Blue means arriving
            carDisplayArea.getChildren().add(carShape);
            simulateCarBehavior(carShape);
        });
    }

    private Rectangle createCarShape(Color color) {
        Rectangle car = new Rectangle(50, 30); // Represent car with a rectangle
        car.setFill(color);
        return car;
    }

    private void simulateCarBehavior(Rectangle carShape) {
        carThreadPool.submit(() -> {
            double fuelNeeded = Math.random() * 0.3; // Each car needs random amount of fuel (up to 30%)

            synchronized (fuelLock) {
                while (fuelNeeded > fuelAmount) {
                    // Not enough fuel, car waits (change color to red to indicate waiting)
                    Platform.runLater(() -> carShape.setFill(Color.RED)); // Red for waiting
                    try {
                        fuelLock.wait(); // Car waits until it's notified that the reservoir has been refilled
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Once there's enough fuel, consume it
                fuelAmount -= fuelNeeded;
                updateUI();

                // Simulate the car consuming fuel (turn green and move it out of view)
                Platform.runLater(() -> {
                    carShape.setFill(Color.GREEN); // Green means departing
                    animateCarDeparture(carShape);
                });
            }
        });
    }

    private void animateCarDeparture(Rectangle carShape) {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(3), carShape);
        transition.setByX(300); // Move the car to the right
        transition.setOnFinished(e -> carDisplayArea.getChildren().remove(carShape)); // Remove the car after moving
        transition.play();
    }

    private void recharge(double amount) {
        synchronized (fuelLock) {
            fuelAmount += amount;
            if (fuelAmount > 1.0) {
                fuelAmount = 1.0; // Cap at full reservoir (100%)
            }
            updateUI();
            fuelLock.notifyAll(); // Notify all waiting cars that they can try consuming fuel again
        }
    }

    private void updateUI() {
        Platform.runLater(() -> {
            fuelProgressBar.setProgress(fuelAmount);
            fuelLabel.setText(String.format("Fuel Level: %.2f%%", fuelAmount * 100));
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        carThreadPool.shutdown(); // Shutdown the thread pool when the application is closed
    }

    public static void main(String[] args) {
        launch();
    }

}