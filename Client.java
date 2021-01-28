package project;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import project.util.Operation;
import project.util.UtilityFunctions;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static project.util.UtilityFunctions.showConfirmation;
import static project.util.UtilityFunctions.showError;

public class Client extends Application {
    private TextField username, password;
    private Button signIn, signUp, back;
    private Socket socket;
    private ClientHandler clientHandler;
    private TextFlow textFlow;
    private final Background focusedBackground = new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY));
    private final Background unfocusedBackground = new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
    private List<File> selectedFiles = null;
    private String[] filesFoldersRoot;
    private static final String USERS_MAIN_DIRECTORY = "C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\directory-for-users";
    private static String currentPath = USERS_MAIN_DIRECTORY;
    private static boolean y = false;

    public void start(Stage stage) {
        stage.setTitle("FTP Application");
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20, 20, 20, 20));
        gridPane.setAlignment(Pos.CENTER);

        VBox vBox = getVBox();
        gridPane.add(vBox, 0, 0);

        Scene scene = new Scene(gridPane, 400, 400);
        stage.setScene(scene);
        stage.show();

        try {
            socket = new Socket("localhost", 8080);
        } catch (IOException e) {
            showError("Exception Occurred in ClientJeopardy start method: " + e.toString());
        }
        clientHandler = new ClientHandler(socket);
        signIn.setOnAction(e -> {
            boolean isVerified = clientHandler.verifyMe(this.username.getText(), this.password.getText());
            if (isVerified) {
                showConfirmation("User is verified");
                stage.setScene(getMainScene(stage));
                stage.setResizable(false);
                stage.show();
                showFilesInRoot(true);
            } else {
                showError("User Data not found");
            }
        });
        signUp.setOnAction(e -> clientHandler.addMe(this.username.getText(), this.password.getText()));
    }

    private void showFilesInRoot(boolean isForRoot) {
        if (isForRoot) {
            filesFoldersRoot = this.clientHandler.getAllFilesFolders();
        }
        if (filesFoldersRoot.length > 1) {
            for (String string : filesFoldersRoot) {
                ImageView imageView = new ImageView();
                String[] chars = string.split("\\.");
                String extension = chars.length > 1 ? chars[chars.length - 1] : "";
                if (extension.contains("mp3") || extension.contains("aac") || extension.contains("wav")) {
                    imageView.setImage(new Image(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\images\\toppng.com-music-icon-music-icon-black-and-white-1417x1558.png").toURI().toString()));
                } else if (extension.contains("txt")) {
                    imageView.setImage(new Image(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\images\\toppng.com-otepad-and-pencil-icon-free-download-png-svg-icon-notepad-and-pencil-icon-980x966.png").toURI().toString()));
                } else if (extension.contains("mp4")) {
                    imageView.setImage(new Image(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\images\\iconfinder_icons_video_1564536.png").toURI().toString()));
                } else if (extension.contains("jpg") || extension.contains("jpeg") || extension.contains("png")) {
                    imageView.setImage(new Image(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\images\\image.png").toURI().toString()));
                } else if (extension.equalsIgnoreCase("")) {
                    imageView.setImage(new Image(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\images\\iconfinder_folder-storage-files_2931141.png").toURI().toString()));
                } else {
                    imageView.setImage(new Image(new File("C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\project\\images\\document.png").toURI().toString()));
                }

                imageView.setFitWidth(20);
                imageView.setFitHeight(20);

                HBox hBox = new HBox();
                hBox.setSpacing(8);
                hBox.setAlignment(Pos.CENTER);
                hBox.getChildren().addAll(imageView, new Text(string));
                hBox.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        hBox.requestFocus();
                        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                            if (mouseEvent.getClickCount() == 2) {
                                String text = ((Text) hBox.getChildren().get(1)).getText();
                                String[] strings = text.split("\\.");
                                if (strings.length > 1) {
                                    showConfirmation(text + " file will be downloaded soon!");
                                    new Thread(() -> {
                                        boolean isDownloaded = clientHandler.downloadSelectedFile(text);
                                        if (isDownloaded) {
                                            showConfirmation("File downloaded: " + text);
                                        } else {
                                            showError("File is not downloaded!: " + text);
                                        }
                                    }).start();
                                } else {
                                    currentPath = currentPath + "\\" + text;
                                    String[] all = clientHandler.moveDownOneDirectory(text);
                                    if (all.length > 0) {
                                        filesFoldersRoot = all;
                                    } else {
                                        textFlow.getChildren().removeAll(textFlow.getChildren());
                                    }
                                    textFlow.getChildren().removeAll(textFlow.getChildren());
                                    showFilesInRoot(false);
                                }
                            }
                        }
                    }
                });
                hBox.backgroundProperty().bind(Bindings
                        .when(hBox.focusedProperty())
                        .then(focusedBackground)
                        .otherwise(unfocusedBackground)
                );
                textFlow.getChildren().addAll(hBox, new Text("\n"));
            }
        }
    }

    private VBox getVBox() {
        username = getTextField("Enter Username");
        password = getTextField("Enter Password");

        signIn = getButton("Sign In", 65);
        signUp = getButton("Sign Up", 65);

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(signIn, signUp);

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(username, password, hBox);
        return vBox;
    }

    private TextField getTextField(String placeholderText) {
        if (placeholderText.contains("name")) {
            TextField textField = new TextField();
            textField.setPromptText(placeholderText);
            textField.setMinWidth(260);
            textField.setPadding(new Insets(10, 10, 10, 10));
            return textField;
        } else {
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText(placeholderText);
            passwordField.setMinWidth(260);
            passwordField.setPadding(new Insets(10, 10, 10, 10));
            return passwordField;
        }
    }

    private Button getButton(String text, int width) {
        Button button = new Button(text);
        button.setMinWidth(width);
        return button;
    }

    private Scene getMainScene(Stage stage) {
        GridPane gridPane = new GridPane();

        back = getButton("Back", 65);
        back.setPadding(new Insets(5, 5, 5, 5));
        VBox vBoxZero = new VBox();
        vBoxZero.setSpacing(10);
        vBoxZero.getChildren().add(back);
        back.setOnAction(e -> {
            if (currentPath.equals(USERS_MAIN_DIRECTORY)) {
                showError("You are not allowed to go back");
            } else {
                if (y) {
                    currentPath = currentPath.substring(0, currentPath.lastIndexOf("\\"));
                }
                filesFoldersRoot = this.clientHandler.moveUpOneDirectory(currentPath);
                textFlow.getChildren().removeAll(textFlow.getChildren());
                this.showFilesInRoot(false);
                y = true;
            }
        });

        textFlow = new TextFlow();
        textFlow.setPadding(new Insets(10, 10, 10, 10));
        textFlow.setStyle("-fx-background-color: white");
        textFlow.setPrefWidth(1000);
        textFlow.setPrefHeight(300);

        ScrollPane scrollPane = new ScrollPane(textFlow);

        Button upload = this.getButton("Upload", 135);
        Button selectImage = this.getButton("Select File(s)", 135);
        SimpleStringProperty simpleStringProp = new SimpleStringProperty("Select File(s)");

        Label selectedFilesName = new Label();
        selectedFilesName.textProperty().bind(simpleStringProp);
        selectImage.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image to upload");
            fileChooser.getExtensionFilters().addAll(
                    new ExtensionFilter("Text Files", "*.txt"),
                    new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
                    new ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
                    new ExtensionFilter("All Files", "*.*")
            );
            selectedFiles = fileChooser.showOpenMultipleDialog(stage);
            boolean isPresent = false;
            if (selectedFiles != null) {
                if (selectedFiles.size() <= 5) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (File file : selectedFiles) {
                        stringBuilder.append(file.getName()).append(", ");
                        if (Arrays.asList(filesFoldersRoot).contains(file.getName())) {
                            isPresent = true;
                            showError("Selected file(s) already exists in the directory");
                            selectedFiles = null;
                            break;
                        }
                    }
                    if (isPresent) {
                        simpleStringProp.set("Select File(s)");
                    } else {
                        simpleStringProp.set(stringBuilder.toString());
                    }
                } else {
                    UtilityFunctions.showError("Select upto 5 Files");
                }
            } else {
                UtilityFunctions.showError("No File Selected");
                simpleStringProp.set("Select File(s)");
            }
        });

        upload.setOnAction(e -> {
            if (selectedFiles != null) {
                this.clientHandler.sendFiles(selectedFiles);
            } else {
                showError("Please Select Files to upload");
            }
        });

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(upload, selectImage, selectedFilesName);

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(vBoxZero, scrollPane, hBox);
        gridPane.add(vBox, 0, 0);
        gridPane.setAlignment(Pos.CENTER);
        return new Scene(gridPane, 1100, 400);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class ClientHandler {
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public ClientHandler(Socket socket) {
        if (socket != null) {
            try {
                this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                showError("Exception Occurred in SingleClientThread Constructor: " + ex.toString());
            }
            return;
        }
        showError("Server is not running");
    }

    public boolean verifyMe(String username, String password) {
        try {
            this.objectOutputStream.writeObject(Operation.VERIFY_USER);
            this.objectOutputStream.writeObject(username + "," + password);
            return (boolean) this.objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            showError("Exception Occurred in ClientHandler verifyMe: " + e.toString());
        }
        return false;
    }

    public void addMe(String username, String password) {
        try {
            this.objectOutputStream.writeObject(Operation.ADD_USER);
            this.objectOutputStream.writeObject(username + "," + password);
        } catch (IOException ex) {
            showError("Exception Occurred in ClientHandler verifyMe: " + ex.toString());
        }
    }

    public String[] getAllFilesFolders() {
        try {
            this.objectOutputStream.writeObject(Operation.READ_DIRECTORY);
            return ((StringBuilder) this.objectInputStream.readObject()).toString().split(",");
        } catch (IOException | ClassNotFoundException e) {
            showError("Exception Occurred in ClientHandler getAllFilesFolders: " + e.toString());
        }
        return null;
    }

    public void sendFiles(List<File> allFilesPaths) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            this.objectOutputStream.writeObject(Operation.FILE_UPLOAD);
            for (int i = 0; i < allFilesPaths.size(); i++) {
                stringBuilder.append(allFilesPaths.get(i).getName());
                if (i != allFilesPaths.size() - 1) {
                    stringBuilder.append(",");
                }
            }
            this.objectOutputStream.writeObject(stringBuilder.toString());
            for (File file : allFilesPaths) {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
                byte[] arrayToSend = new byte[(int) file.length()];
                bufferedInputStream.read(arrayToSend, 0, arrayToSend.length);
                this.objectOutputStream.writeObject(arrayToSend);

            }
        } catch (IOException ex) {
            showError("Exception occurred in ClientHandler sendFiles: " + ex.toString());
        }
    }

    public boolean downloadSelectedFile(String fileName) {
        try {
            this.objectOutputStream.writeObject(Operation.DOWNLOAD_FILE);
            this.objectOutputStream.writeObject(fileName);

            String currentCompletePath = System.getProperty("user.home") + "\\Downloads\\" + fileName;
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(currentCompletePath)));
            byte[] arrayReceived = (byte[]) this.objectInputStream.readObject();
            bufferedOutputStream.write(arrayReceived, 0, arrayReceived.length);
            bufferedOutputStream.flush();
            return true;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            showError("Exception occurred in SingleClientHandler: " + e.toString());
        }
        return false;
    }

    public String[] moveUpOneDirectory(String currentPath) {
        try {
            this.objectOutputStream.writeObject(Operation.MOVE_UP_ONE_STEP);
            this.objectOutputStream.writeObject(currentPath);
            return ((StringBuilder) this.objectInputStream.readObject()).toString().split(",");
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            showError("Exception occurred in ClientHandler in moveUpOneDirectory: " + ex.toString());
        }
        return null;
    }

    public String[] moveDownOneDirectory(String folderPath) {
        try {
            this.objectOutputStream.writeObject(Operation.MOVE_DOWN_ONE_STEP);
            this.objectOutputStream.writeObject(folderPath);
            return ((StringBuilder) this.objectInputStream.readObject()).toString().split(",");
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            showError("Error occurred in ClientHandler moveDownOneDirectory: " + ex.toString());
        }
        return null;
    }
}
