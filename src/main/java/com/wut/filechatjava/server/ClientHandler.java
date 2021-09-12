package com.wut.filechatjava.server;

import com.wut.filechatjava.database.Database;
import com.wut.filechatjava.model.Files;
import com.wut.filechatjava.model.User;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler extends Thread{

    private final Socket socket;
    private final String address;
    private final Database database;
    private User currentUser;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream din;
    private DataOutputStream dos;
    private String client;
    
    public ClientHandler(Socket socket, Database database) {
        
        this.address = "(" + socket.getInetAddress() + ", " + socket.getPort() + ")";
        System.out.println("Connection established with client: " + this.address);
        this.socket = socket;
        this.database = database;
    }
    
    @Override
    public void run() {
        
        try {
            initializeStreams();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // indicates what server it is
        sendMessage("java");
        try {
            // gets what client it is
            this.client = readMessage();
            
        } catch (SocketException ex) {
            // client fail
            System.out.println("Client: " + this.address + " unexpectedly disconnected");
            return;
        }
        
        // login or registration
        System.out.println(" - Asking to login or register");
        boolean quit = true;
        try {
            quit = showLoginMenu();
            
        } catch (SocketException ex) {
            // client fail
            System.out.println("Client: " + this.address + " unexpectedly disconnected");
            
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            // main menu
            showMenu(quit);
        } catch (SocketException ex) {
            // client fail
            System.out.println("Client: " + this.address + " unexpectedly disconnected");
            // disconnects user
            this.database.disconnectUser(this.currentUser.getUsername());
            
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean showLoginMenu() throws SocketException, IOException{
        
        boolean run = true;
        boolean quit = false;
        while(run){
            sendMessage("What do you want to do?\n\ta. Login\n\tb. Register\n\tc.Quit\nChoice:");
            String choice = readMessage();

            if(choice.equals("a")){
                // login
                System.out.println(" - Requesting login data");
                this.currentUser = login();
                if(this.currentUser != null){
                    run = false;
                }
            }
            else if(choice.equals("b")){
                // register
                System.out.println(" - Registering new user");
                this.currentUser = register();
                if(this.currentUser != null){
                    run = false;
                }
            }
            else if(choice.equals("c")){
                // quit
                System.out.println(" - Disconnecting client " + this.address + " from the server");
                sendMessage("Goodbye!");
                run = false;
                quit = true;
            }
            else{
                System.out.println(" - Incorrect choice, trying again");
                sendMessage("Incorrect choice, try again\n");
            }
        }
        return quit;
    }
    
    private void showMenu(boolean quit) throws IOException, SocketException{
        
        while(!quit){
            System.out.println(" - Displaying menu");
            sendMessage("What do you want to do?\n\ta. Check user's state\n\tb. Send file\n\tc. Refresh received files"
                    + "\n\td. Change file extensions\n\te. Quit\nChoice:");
            String choice = readMessage();

            switch (choice) {
                case "a":
                    // checks another user's state
                    System.out.println(" - Checking user's state");
                    checkState();
                    break;
                case "b":
                    // sends a file to the specified user
                    System.out.println(" - Sending a file");
                    openSendFile();
                    break;
                case "c":
                    // checks if the user has received any new files
                    System.out.println(" - Checking for new received files");
                    receivedFiles();
                    break;
                case "d":
                    // changes its accepted file extensions
                    System.out.println(" - Changing accepted file extensions");
                    changeExtensions();
                    break;
                case "e":
                    // quits app
                    System.out.println(" - Disconnecting client '" + this.currentUser.getUsername() + "' from the server");
                    sendMessage("Goodbye!");
                    this.database.disconnectUser(this.currentUser.getUsername());
                    quit = true;
                    break;
                default:
                    System.out.println(" - Trying to choose a menu option again");
                    sendMessage("Incorrect option, try again\n");
                    break;
            }
        }
    }
    
    private void openSendFile() throws IOException, SocketException{
        
        while(true){
            // asks for friend
            System.out.println(" - Asking for username");
            sendMessage("To whom do you want to send the file? (q)\nUsername:");
            String friendUsername = readMessage();
            if(friendUsername.equals("q")){
                return;
            }
            System.out.println("Username: " + friendUsername);
            
            // checks if friend's username exists
            User friend = this.database.getUser(friendUsername);
            // friend doesn't exist
            if(friend == null){
                System.out.println(" - Username doesn't exist");
                sendMessage("Username doesn't exist, try again\n");
            }
            // same current user
            else if(friendUsername.equals(this.currentUser.getUsername())){
                System.out.println(" - Same user");
                sendMessage("You can't send a file to yourself, try again\n");
            }
            // friend is disconnected
            else if(!friend.isAvailable()){
                System.out.println(" - User is disconnected");
                sendMessage("The user is disconnected, can't send message\n");
                return;
            }
            // friend is connected
            else{
                while(true){
                    // asks for file to send
                    System.out.println(" - Choosing file to send");
                    sendMessage("What file do you want to send?");
                    
                    // gets file size
                    int fileSize = Integer.parseInt(readMessage());
                    // file size not allowed (too big)
                    if(fileSize > 65536){
                        sendMessage("File too big (only allowed up to 64kb), try again\n");
                        continue;
                    }
                    // file size allowed
                    else{
                        sendMessage("ok");
                    }
                    
                    // gets filename
                    String filename = readMessage();
                    // checks if the user chose a file
                    if(filename.equals("q")){
                        continue;
                    }
                    System.out.println("File name: " + filename);
                    // gets file extension
                    String extension = filename.split("\\.")[1];
                    // checks if the extension is allowed
                    List<String> acceptedExtensions = Arrays.asList(friend.getExtensions().split(","));
                    // invalid file extension
                    if(!acceptedExtensions.contains(extension)){
                        System.out.println(" - File extension not valid");
                        sendMessage("File extension not accepted, only valid: " + friend.getExtensions() + ". Try again\n");
                    }
                    // correct file extension
                    else{
                        sendMessage("ok");
                        // creates directory and file
                        File folder = new File("./files/");
                        folder.mkdir();
                        File file = new File("./files/" + filename);
                        if(!file.exists()){
                            file.createNewFile();
                        }
                        FileOutputStream fos = new FileOutputStream(file);
                        
                        // writes file data
                        int totalBytesRead = 0;
                        int counter = 0;
                        byte[] buffer = new byte[8192];

                        while(totalBytesRead < fileSize){
                            // reception confirmation message
                            if(this.client.equals("python")){
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
                                                
                        // saves file to database
                        System.out.println(" - Uploading file to database");
                        this.database.uploadFile(friend.getId(), this.currentUser.getId(), filename);
                        sendMessage("File correctly sent\n");
                        break;
                    }  
                    break;
                }
                break;
            }
            
        }
    }
        
    private void receivedFiles() throws IOException, SocketException{
        
        System.out.println(" - Checking if any file has been received");
        List<Files> files = this.database.getFiles(currentUser.getId());
        
        // there aren't files received
        if(files.isEmpty()){
            System.out.println(" - No new files");
            sendMessage("You haven't received any new files\n");
        }
        // new files received
        else{
            System.out.println(" - New files received");
            for(Files file: files){
                // sends file to the user
                User friend = this.database.getUser(file.getFromId());
                sendMessage("You have received a new file from '" + friend.getUsername() + "': " + file.getFilename());
                sendFile(file.getFilename());
                // waits for confirmation message
                readMessage();
                System.out.println(" - File received by user");
            }
            for(Files file: files) this.database.deleteFile(file.getId());
        }
        
    }
        
    // sends file to user
    private void sendFile(String filename) throws FileNotFoundException, IOException, SocketException{
        // sends file to socket
        System.out.println(" - Sending new file to user");
        
        // gets byte array from file
        File file = new File("/files/" + filename);
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        int nBytesRead;
        byte[] data = new byte[8192];
        while((nBytesRead = fis.read(data, 0, data.length)) != -1){
            buffer.write(data, 0, nBytesRead);
        }                                
        buffer.flush();
        byte[] byteArray = buffer.toByteArray();
    
        // sends file size (byte array lenght)
        sendMessage(Integer.toString(byteArray.length));
         
        // waits for confirmation message
        readMessage();
        
        // sends file
        dos.write(byteArray);
        dos.flush();
        fis.close();        
    }
    
    private void changeExtensions() throws IOException, SocketException{
        
        String currentExtensions = this.currentUser.getExtensions();
        
        while(true){
            
            // asks for new extensions
            sendMessage("Your current accepted extensions are: " + currentExtensions +
                        "\nIntroduce the new ones from the list [jpg,png,txt,pdf] (separated by commas) (q):");
            String newExtensions = readMessage();
            if(newExtensions.equals("q")){
                return;
            }
            // checks if they are correct
            String temp[] = {"jpg", "png", "txt", "pdf"};
            List<String> availableExtensions = Arrays.asList(temp);
            String chosenExtensions[] = newExtensions.split(",");
            boolean incorrect = false;
            for(String extension: chosenExtensions){
                // chosen extensions are not correct
                if(!availableExtensions.contains(extension)){
                    System.out.println(" - Incorrect extensions, trying again");
                    sendMessage("Incorrect extensions, try again\n");
                    incorrect = true;
                    break;
                }
            }
            // incorrect extensions
            if(incorrect){
                continue;
            }
            // correct extensions, updates user
            this.database.updateUserExtensions(this.currentUser.getId(), newExtensions);
            this.currentUser.setExtensions(newExtensions);
            System.out.println(" - Correctly changed extensions");
            sendMessage("Extensions changed\n");
            break;
        }
    }
    
    private void checkState() throws IOException, SocketException{
        
        // asks for the other user's username
        sendMessage("Who do you want to know about? (q)\nUsername:");
        String username = readMessage();
        if(username.equals("q")){
            return;
        }
        System.out.println("Username: " + username);
        
        // finds user in database
        User user = this.database.getUser(username);
        if(user == null){
            System.out.println(" - User doesn't exist");
            sendMessage("That user doesn't exist\n");
        }
        else if(user.isAvailable()){
            System.out.println(" - The user is connected");
            sendMessage("The user is connected\n");
        }
        else{
            System.out.println(" - The user is disconnected");
            sendMessage("The user is disconnected\n");
        }
        
    }
    
    private User login() throws IOException, SocketException{
        
        while(true){
            // asks for username
            sendMessage("Introduce your username (q):");
            String username = readMessage();
            if(username.equals("q")){
                return null;
            }
            System.out.println("Username: " + username);
            // asks for password
            sendMessage("Introduce your password (q):");
            String password = readMessage();
            if(password.equals("q")){
                return null;
            }
            System.out.println("Password: " + password);
            
            // checks if data is correct
            User user = this.database.getUser(username);
            // user doesn't exist
            if(user == null){
                sendMessage("The user doesn't exist, try again\n");
                System.out.println(" - Trying to login again");                
            }
            // incorrect password
            else if(!user.getPassword().equals(password)){
                sendMessage("Incorrect password, try again\n");
                System.out.println(" - Trying to login again");
            }
            // user already connected
            else if(user.isAvailable()){
                sendMessage("That user is already connected, try again\n");
                System.out.println(" - Trying to login again");
            }
            // correct data
            else{
                sendMessage("Correct data, logging in...\n");
                user.connect();
                this.database.connectUser(username);
                return user;
            }
        }
    }
    
    private User register() throws IOException, SocketException{
        
        while(true){
            // asks for username
            sendMessage("Introduce your username (q):");
            String username = readMessage();
            if(username.equals("q")){
                return null;
            }
            System.out.println("Username: " + username);
            // asks for password
            sendMessage("Introduce your password (q):");
            String password = readMessage();
            if(password.equals("q")){
                return null;
            }
            System.out.println("Password: " + password);
            
            // checks if the username already exists
            User user = this.database.getUser(username);
            // user doesn't exist
            if(user == null){
                System.out.println(" - Creating new user...");     
                // registers user in the database
                this.database.registerUser(username, password);
                return this.database.getUser(username);
            }
            // user already exists
            else{
                sendMessage("That username already exists, try again\n");
                System.out.println(" - Trying to register again");
            }
        }
    }
    
    private String readMessage() throws SocketException{
        String message;
        try {
            message = in.readLine();
        } catch (Exception e){
            throw new SocketException();
        }
        if(message == null){
            throw new SocketException();
        }
        return message;
    }
    
    private void sendMessage(String message){
        out.println(message);
        out.flush();
    }
    
    private void initializeStreams() throws IOException{
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        din = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }
}
