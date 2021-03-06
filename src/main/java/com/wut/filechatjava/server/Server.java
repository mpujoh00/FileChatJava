package com.wut.filechatjava.server;

import com.wut.filechatjava.database.Database;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    
    private static ServerSocket serverSocket;
    private static Database database;
    private static int port = 2021;
    
    public static void main(String[] args) throws IOException{
        
        // starts the server
        startServer();
        
        // gets the database instance
        try{
            database = Database.getInstance();
            System.out.println("Successfully connected to the database!");
        }catch(Exception e){
            // database connection fail, can't run without database
            System.out.println("Couldn't connect to the database");
            return;
        }
        
        // keeping database connection alive
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
              database.keepAlive();
          }
        }, 0, 90*1000); // waits 1:30 min
        
        // waits for clients to request connection to the server
        while(true){
            System.out.println("Waiting for a client connection request");
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket, database);
            client.start();
        }
    }
    
    private static void startServer(){
        
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("192.168.1.37"));
            System.out.println("Server listening on port " + port);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
