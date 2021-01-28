package project.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class UtilityFunctions {
    public static boolean checkForUserExistence(String username, String password) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\util\\users-data.txt"));
            while (scanner.hasNext()) {
                String[] completeString = scanner.nextLine().split(",");
                if (completeString[0].equalsIgnoreCase(username) && completeString[1].equalsIgnoreCase(password)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            System.out.println("Exception occurred in UtilityFunctions checkForUsername: " + ex.toString());
        }
        if (scanner != null) {
            scanner.close();
        }
        return false;
    }

    public static void addUserData(String username, String password) {
        try {
            PrintWriter printWriter = new PrintWriter(new FileOutputStream("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\util\\users-data.txt", true));
            printWriter.println(username + "," + password);
            printWriter.close();
        } catch (IOException e) {
            System.out.println("Exception Occurred in UtilityFunctions addUserData: " + e.toString());
        }
    }

    public static void showError(String errorStatement) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.getDialogPane().setContent(getText(errorStatement));
            alert.show();
        });
    }

    public static void showConfirmation(String confirmationStatement) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getDialogPane().setContent(getText(confirmationStatement));
            alert.show();
        });
    }
    private static Label getText(String string) {
        Label label = new Label(string);
        label.setPrefWidth(400);
        label.setPadding(new Insets(15, 15, 15, 15));
        return label;
    }
}
