import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
 
/*
Client that connects with Python server
*/
public class Client {
    
    static Socket socket;
    static InputStream is;
    static BufferedReader in;
    static PrintWriter out;
    static DataInputStream din;
    static DataOutputStream dos;
 
    public static void main(String args[])throws Exception {
 
        String host = "localhost";
        int port = 2021;
        try {
            Socket socket = new Socket(host, port);
            System.out.println("Java client connecting to Python server of ip: " + host + " port: " + port);
             
            
            is = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(is));
            out = new PrintWriter(socket.getOutputStream(), true);
            din = new DataInputStream(is);
            dos = new DataOutputStream(socket.getOutputStream());
            
            System.out.println("Waiting for messages");
            Scanner sc = new Scanner(System.in);
            String receivedMessage;
           
            // reads messages from the server
            while((receivedMessage = in.readLine()) != null){
                // send file to friend
                if(receivedMessage.equals("What file do you want to send?")){
                    System.out.println(" - " + receivedMessage);
                    JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                    int result = fileChooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        // user chooses a file
                        File chosenFile = fileChooser.getSelectedFile();
                        
                        // sends file size (only allowed up to 64 kb)
                        out.print(Long.toString(chosenFile.length()));
                        out.flush();
                        String validSize = in.readLine();
                        
                        // waits to know if the file size is allowed
                        if(validSize.equals("ok")){
                            // sends file name
                            out.print(chosenFile.getName());
                            out.flush();

                            // waits to know if the file extension is accepted
                            String validExtension = in.readLine();
                            if(validExtension.equals("ok")){
                                
                                // gets byte array from file
                                FileInputStream fis = new FileInputStream(chosenFile);
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
                            else{
                                System.out.println(" - " + validExtension);
                            }
                        }else{
                            System.out.println(" - " + validSize);
                        }
                    }
                    else{
                        out.print("q");
                        out.flush();
                    }
                }
                // files received
                else if(receivedMessage.startsWith("You have received a new file from")){
                    System.out.println(" - " + receivedMessage);
                    
                    // gets file name
                    String filename = receivedMessage.split(": ")[1];
                    
                    // creates directory and file
                    File folder = new File("./received files/");
                    folder.mkdir();
                    File file = new File("./received files/" + filename);
                    if(!file.exists()){
                        file.createNewFile();
                    }
                    FileOutputStream fos = new FileOutputStream(file);   
                    
                    // gets file size
                    int fileSize = Integer.parseInt(in.readLine());
                                        
                    // writes file data
                    int totalBytesRead = 0;
                    int counter = 0;
                    byte[] buffer = new byte[8192];
                    
                    while(totalBytesRead != fileSize){
                        // reception confirmation message
                        out.print("ok");
                        out.flush();
                        // reads bytes
                        counter = din.read(buffer, 0, buffer.length);
                        // writes file
                        fos.write(buffer, 0, counter);
                        totalBytesRead += counter;
                    }          
                    fos.flush();
                    fos.close();
                    
                    // sends confirmation message
                    out.print("ok");
                    out.flush();
                } 
                else if(receivedMessage.equals("Goodbye!")){
                    System.out.println(" - " + receivedMessage);
                    closeConnection();
                    break;
                }
                else if(!receivedMessage.isBlank()){
                    System.out.println(" - " + receivedMessage);
                }
                else{
                    System.out.println(receivedMessage);
                }                
                // answers to the message when it is expected
                if(receivedMessage.endsWith(":")){
                    out.print(sc.nextLine());
                    out.flush();
                }                
            }                        
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void closeConnection() throws IOException{
        
        in.close();
        out.close();
        din.close();
        dos.close();
        is.close();
        if(socket != null)    
            socket.close();
    }
}