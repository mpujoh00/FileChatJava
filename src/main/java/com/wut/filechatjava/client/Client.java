package com.wut.filechatjava.client;

import com.wut.filechatjava.exception.ServerConnectionException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
 
/*
client.Client that connects with Python server
*/
public class Client {
    
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
    private static DataInputStream din;
    private static DataOutputStream dos;
    private static String server;

    //PYTHON SERVER
    private static final String PYTHON_HOST = "localhost";
    private static final int PYTHON_PORT = 2022;
    //JAVA SERVER
    private static final String JAVA_HOST = "localhost";
    private static final int JAVA_PORT = 2021;


    public static void main(String[] args) throws ServerConnectionException, IOException {

        // tries to connect to server
        try {
            socket = connectToServer(JAVA_HOST, JAVA_PORT);
        } catch (ServerConnectionException e) {
            socket = connectToServer(PYTHON_HOST, PYTHON_PORT);
        }

        initializeStreams();
        
        // gets what server it is
        server = readMessage();

        // indicates what client it is
        sendMessage("java");

        try {
            readMessages();
        } catch (ServerConnectionException e){
            System.out.println("fallaron los dos");
        }
    }

    private static void readMessages() throws ServerConnectionException {
        System.out.println("Waiting for messages");
        Scanner sc = new Scanner(System.in);
        String receivedMessage = readMessage();
        // reads messages from the server
        while(receivedMessage != null){
            try {
                // send file to friend
                if (receivedMessage.equals("What file do you want to send?")) {
                    System.out.println(" - " + receivedMessage);
                    openSendFile();
                }
                // files received
                else if (receivedMessage.startsWith("You have received a new file from")) {
                    System.out.println(" - " + receivedMessage);
                    receiveFiles(receivedMessage);
                // quit app
                } else if (receivedMessage.equals("Goodbye!")) {
                    System.out.println(" - " + receivedMessage);
                    closeConnection();
                    break;
                // empty message
                } else if (!receivedMessage.isBlank()) {
                    System.out.println(" - " + receivedMessage);
                // ordinary message
                } else {
                    System.out.println(receivedMessage);
                }
                // answers to the message when it is expected
                if (receivedMessage.endsWith(":")) {
                    sendMessage(sc.nextLine());
                }
                
                // reads next message
                receivedMessage = readMessage();
                
            } catch (ServerConnectionException e) {
                System.out.println("Falló la conexión, probando con otro cliente");
                if(server.equals("java")) {
                    System.out.println("Conectandose al cliente de Python");
                    socket = connectToServer(PYTHON_HOST, PYTHON_PORT);
                } else {
                    System.out.println("Conectandose al cliente de Java");
                    socket = connectToServer(JAVA_HOST, JAVA_PORT);
                }
                try {
                    initializeStreams();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                server = readMessage();
                sendMessage("java");
                
                // reads next message
                receivedMessage = readMessage();
            }
        }
    }

    private static void openSendFile() throws ServerConnectionException {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            // user chooses a file
            File chosenFile = fileChooser.getSelectedFile();

            // sends file size (only allowed up to 64 kb)
            sendMessage(Long.toString(chosenFile.length()));
            String validSize = readMessage();

            // waits to know if the file size is allowed
            if(validSize.equals("ok")){
                // sends file name
                sendMessage(chosenFile.getName());

                // waits to know if the file extension is accepted
                String validExtension = readMessage();
                if(validExtension.equals("ok")){
                    try {
                        // gets byte array from file
                        FileInputStream fis = new FileInputStream(chosenFile);
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nBytesRead;
                        byte[] data = new byte[1024];
                        while ((nBytesRead = fis.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nBytesRead);
                        }
                        buffer.flush();
                        byte[] byteArray = buffer.toByteArray();
                        dos.write(byteArray);
                        dos.flush();
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println(" - " + validExtension);
                }
            }else{
                System.out.println(" - " + validSize);
            }
        }
        else{
            sendMessage("q");
        }
    }

    private static void receiveFiles(String receivedMessage) throws ServerConnectionException {
        // gets file name
        String filename = receivedMessage.split(": ")[1];

        // creates directory and file
        File folder = new File("./received files/");
        folder.mkdir();
        File file = new File("./received files/" + filename);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(file);

            // gets file size
            int fileSize = Integer.parseInt(readMessage());

            // sends confirmation message
            sendMessage("ok");

            // writes file data
            int totalBytesRead = 0;
            int counter = 0;
            byte[] buffer = new byte[8192];

            while (totalBytesRead < fileSize) {
                // reception confirmation message
                if (server.equals("python")) {
                    sendMessage("ok");
                }
                // reads bytes
                counter = din.read(buffer, 0, buffer.length);
                // writes file
                fos.write(buffer, 0, counter);
                totalBytesRead += counter;
            }
            fos.flush();
            fos.close();

            // sends confirmation message
            sendMessage("ok");
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    
    // sends a file to python server
    private static void sendFilePython(File file) throws IOException{
        
        // gets byte array from file
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nBytesRead;
        byte[] data = new byte[1024];
        while((nBytesRead = fis.read(data, 0, data.length)) != -1){
            buffer.write(data, 0, nBytesRead);
        }                                
        buffer.flush();
        byte[] byteArray = buffer.toByteArray();

        // sends byte array lenght
        dos.writeInt(byteArray.length);

        // sends file
        dos.write(byteArray);
        dos.flush();
        fis.close();
    }
    
    // sends a file to java server
    private static void sendFileJava(File file) throws IOException{
        
        // gets byte array from file
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nBytesRead;
        byte[] data = new byte[1024];
        while((nBytesRead = fis.read(data, 0, data.length)) != -1){
            buffer.write(data, 0, nBytesRead);
        }                                
        buffer.flush();
        byte[] byteArray = buffer.toByteArray();

        // sends byte array lenght
        dos.writeInt(byteArray.length);

        // sends file
        dos.write(byteArray);
        dos.flush();
        fis.close();
    }

    private static String readMessage() throws ServerConnectionException {
        try {
            return in.readLine();
        } catch (Exception e){
            System.out.println("Servidor falló a la hora de leer el mensaje: " + e.toString());
            throw new ServerConnectionException("Servidor falló, intentando conectar con el servidor de Python", e);
        }
    }
    
    private static void sendMessage(String message){
        out.println(message);
        out.flush();
    }

    private static Socket connectToServer(String host, int port) throws ServerConnectionException {
        try {
            System.out.println("Java client connecting to server of ip: " + host + " port: " + port);
            return new Socket(host, port);
        } catch (Exception e) {
            throw new ServerConnectionException("", e);
        }
    }
    
    private static void initializeStreams() throws IOException{
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        din = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }
    
    private static void closeConnection() {
        try {
            in.close();
            out.close();
            din.close();
            dos.close();
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}