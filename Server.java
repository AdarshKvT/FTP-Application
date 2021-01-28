package project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import project.util.Operation;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static project.util.UtilityFunctions.*;

public class Server extends Application {
    public static final HashMap<Socket, SingleClientHandler> socketAndThreadRecord = new HashMap<>();
    public static final long INTERVAL_FOR_IDLE = 300000;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setPrefHeight(700);
        ScrollPane scrollPane = new ScrollPane(textArea);
        VBox vBox = new VBox();
        vBox.getChildren().addAll(scrollPane);
        vBox.setSpacing(20);
        Scene scene = new Scene(vBox, 400, 700);
        Timer timer = new Timer();
        timer.schedule(new CloseConnectionOfIdling(), INTERVAL_FOR_IDLE, INTERVAL_FOR_IDLE);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        textArea.appendText("Server Started!\n");
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8080);
                while (true) {
                    if (socketAndThreadRecord.size() <= 5) {
                        Socket socket = serverSocket.accept();
                        textArea.appendText(socket.getInetAddress().getHostAddress() + " is connected to the server" + "\n");
                        System.out.println("Socket accepted");
                        SingleClientHandler singleClientHandler = new SingleClientHandler(socket);
                        socketAndThreadRecord.put(socket, singleClientHandler);
                        Thread thread = new Thread(singleClientHandler);
                        thread.start();
                    } else {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setContentText("Cannon Connect More Than 5 instances..!!");
                            alert.show();
                            alert.showAndWait();
                            alert.initModality(Modality.APPLICATION_MODAL);
                        });
                    }
                }
            } catch (IOException e) {
                showError("Exception Occurred in Server in start: " + e.toString());
            }
        }).start();
    }

    static class CloseConnectionOfIdling extends TimerTask {
        @Override
        public void run() {
            for (HashMap.Entry<Socket, SingleClientHandler> socketData : socketAndThreadRecord.entrySet()) {
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - socketData.getValue().getMilliSeconds() > INTERVAL_FOR_IDLE) {
                    try {
                        socketData.getKey().close();
                        socketAndThreadRecord.remove(socketData.getKey());
                    } catch (IOException e) {
                        System.out.println("Exception occurred in CloseConnectionOfIdling run method: " + e.toString());
                    }
                }
            }
        }
    }
}

class SingleClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private long milliSeconds = System.currentTimeMillis();
    static String USERS_MAIN_DIRECTORY = "C:\\Users\\themg\\IdeaProjects\\DCCN Assignment Lab 01\\src\\main\\java\\directory-for-users";
    private File[] files;
    private String completeCurrentPath = USERS_MAIN_DIRECTORY;

    public SingleClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException ex) {
            System.out.println("Exception Occurred in SingleClientThread Constructor: " + ex.toString());
        }
    }

    public long getMilliSeconds() {
        return this.milliSeconds;
    }

    private void verifyUser() throws IOException, ClassNotFoundException {
        String[] completeString = ((String) this.objectInputStream.readObject()).split(",");
        boolean result = checkForUserExistence(completeString[0], completeString[1]);
        this.objectOutputStream.writeObject(result);
    }

    private void addUser() throws IOException, ClassNotFoundException {
        String[] completeString = ((String) this.objectInputStream.readObject()).split(",");
        addUserData(completeString[0], completeString[1]);
    }

    private void readRootDirectory(String path) {
        try {
            files = new File(path).listFiles();
            if (files != null) {
                StringBuilder allFiles = new StringBuilder();
                for (File singleFile : files) {
                    allFiles.append(singleFile.getName()).append(",");
                }
                this.objectOutputStream.writeObject(allFiles);
            } else {
                this.objectOutputStream.writeObject(new StringBuilder());
            }
        } catch (IOException ex) {
            System.out.println("Error Occurred in SingleClientHandler in readRootDirectory: " + ex.toString());
        }
    }

    private void receiveFiles() {
        try {
            String[] allFileNames = ((String) this.objectInputStream.readObject()).split(",");
            for (String singleFileName : allFileNames) {
                String currentCompletePath = this.completeCurrentPath + "\\uploaded-to-server-" + singleFileName;
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(currentCompletePath)));
                byte[] arrayReceived = (byte[]) this.objectInputStream.readObject();
                bufferedOutputStream.write(arrayReceived, 0, arrayReceived.length);
                bufferedOutputStream.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            showError("Exception occurred in SingleClientHandler: " + e.toString());
        }
    }

    private void sendTheSelectedFile(String fileName) {
        try {
            files = new File(completeCurrentPath).listFiles();
            assert files != null;
            for (File file : files) {
                if (file.toString().contains(fileName)) {
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
                    byte[] arrayToSend = new byte[(int) file.length()];
                    bufferedInputStream.read(arrayToSend, 0, arrayToSend.length);
                    this.objectOutputStream.writeObject(arrayToSend);
                    break;
                }
            }
        } catch (IOException ex) {
            showError("Error occurred in SingleClientHandler sendSelectedFile: " + ex.toString());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Operation operation = (Operation) this.objectInputStream.readObject();
                if (operation == Operation.VERIFY_USER) {
                    this.verifyUser();
                } else if (operation == Operation.ADD_USER) {
                    this.addUser();
                } else if (operation == Operation.READ_DIRECTORY) {
                    this.readRootDirectory(USERS_MAIN_DIRECTORY);
                } else if (operation == Operation.MOVE_DOWN_ONE_STEP) {
                    String fileName = (String) this.objectInputStream.readObject();
                    this.moveDownOneDirectory(fileName);
                } else if (operation == Operation.MOVE_UP_ONE_STEP) {
                    String currentDirectory = (String) this.objectInputStream.readObject();
                    this.moveUpOneDirectory(currentDirectory);
                } else if (operation == Operation.FILE_UPLOAD) {
                    this.receiveFiles();
                } else if (operation == Operation.DOWNLOAD_FILE) {
                    String fileName = (String) this.objectInputStream.readObject();
                    this.sendTheSelectedFile(fileName);
                }
                this.milliSeconds = System.currentTimeMillis();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception Occurred in ClientSingleThread run method: " + e.toString());
        }
    }

    private void moveUpOneDirectory(String fileName) {
        String path = fileName.substring(0, fileName.lastIndexOf('\\'));
        this.completeCurrentPath = path;
        this.readRootDirectory(path);
    }

    private void moveDownOneDirectory(String fileName) {
        for (File file : this.files) {
            if (file.getName().contains(fileName)) {
                this.completeCurrentPath = this.completeCurrentPath + "\\" + file.getName();
                this.readRootDirectory(this.completeCurrentPath);
                break;
            }
        }
    }
}