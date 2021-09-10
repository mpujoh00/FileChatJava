
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {
    
    private final String url = "jdbc:mysql://localhost:3306/file_chat";
    private final String user = "root";
    private final String password = "";
    private Connection connection; 
    private PreparedStatement statement;
    
    public Database(){
        // creates connection to the database
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Successfully connected to the database!");
            
        } catch (ClassNotFoundException ex) {
            System.out.println("Couldn't connect to the database");
        } catch (SQLException ex) {
            System.out.println("Couldn't connect to the database");
        }
    }
    
    public void uploadFile(int toId, int fromId, String filename){
        
        try {
            statement = connection.prepareStatement("INSERT INTO Files (to_id, from_id, file, file_name) VALUES (?,?,?,?)");
            statement.setInt(1, toId);
            statement.setInt(2, fromId);
            File file = new File("./files/" + filename);
            FileInputStream fis = new FileInputStream(file);
            statement.setBinaryStream(3, fis);
            statement.setString(4, filename);
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void deleteFile(int fileId){
        
        try {
            statement = connection.prepareStatement("DELETE FROM Files WHERE id=?");
            statement.setInt(1, fileId);
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<Files> getFiles(int toUserId){
        
        ArrayList<Files> files = new ArrayList<Files>();
        try {
            
            statement = connection.prepareStatement("SELECT * FROM Files WHERE to_id=?");
            statement.setInt(1, toUserId);
            ResultSet result = statement.executeQuery();
            
            while(result.next()){
                int id = result.getInt("id");
                int fromId = result.getInt("from_id");
                String filename = result.getString("file_name");
                InputStream fileData = result.getBinaryStream("file");
                files.add(new Files(id, toUserId, fromId, filename, fileData));
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return files;
    }
    
    public void updateUserExtensions(int userId, String newExtensions){
        
        try {
            statement = connection.prepareStatement("UPDATE Users SET accepted_extensions=? WHERE id=?");
            statement.setString(1, newExtensions);
            statement.setInt(2, userId);
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void registerUser(String username, String password){
        
        try {
            statement = connection.prepareStatement("INSERT INTO Users (username, password, accepted_extensions, "
                    + "is_available) VALUES (?,?,?,?)");
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, "jpg,png,txt,pdf");
            statement.setBoolean(4, true);
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // returns User given username
    public User getUser(String username){
        try {
            statement = connection.prepareStatement("SELECT * FROM Users WHERE username=?");
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();
            
            // user exists
            if(result.next()){
                int id = result.getInt("id");
                String password = result.getString("password");
                String extensions = result.getString("accepted_extensions");
                boolean isAvailable = result.getBoolean("is_available");
                return new User(id, username, password, extensions, isAvailable);
            }
            // user doesn't exist
            else{
                return null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    // returns User given username
    public User getUser(int userId){
        try {
            statement = connection.prepareStatement("SELECT * FROM Users WHERE id=?");
            statement.setInt(1, userId);
            ResultSet result = statement.executeQuery();
            
            // user exists
            if(result.next()){
                String username = result.getString("username");
                String password = result.getString("password");
                String extensions = result.getString("accepted_extensions");
                boolean isAvailable = result.getBoolean("is_available");
                return new User(userId, username, password, extensions, isAvailable);
            }
            // user doesn't exist
            else{
                return null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    // connects User (is available to true)
    public void connectUser(String username){
        try {
            statement = connection.prepareStatement("UPDATE Users SET is_available=? WHERE username=?");
            statement.setBoolean(1, true);
            statement.setString(2, username);
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // disconnects User (is available to false)
    public void disconnectUser(String username){
        try {
            statement = connection.prepareStatement("UPDATE Users SET is_available=? WHERE username=?");
            statement.setBoolean(1, false);
            statement.setString(2, username);
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // executed every 90s to keep database's connection
    public void keepAlive(){
        try {
            statement = connection.prepareStatement("SELECT 1 AS keep_alive");
            statement.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
