
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    
    static ServerSocket serverSocket;
    static Database database;
    static int port = 2021;
    
    public static void main(String args[]) throws IOException{
        
        // starts the server
        startServer();
        
        // connects the database 
        connectDatabase();
        
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
            new ClientHandler(serverSocket.accept(), database).start();
        }
    }
    
    public static void startServer(){
        
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void connectDatabase(){
        
        database = new Database();
    }
}