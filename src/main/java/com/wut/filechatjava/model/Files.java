package com.wut.filechatjava.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Files {
    
    private int id;
    private int toId;
    private int fromId;
    private String filename;
    private File file;
    
    public Files(int id, int toId, int fromId, String filename, InputStream fileData){
        
        this.id = id;
        this.toId = toId;
        this.fromId = fromId;
        this.filename = filename;
        
        // writes file
        this.file = new File("/files/" + filename);
        try {
            this.file.createNewFile(); // creates file (if it didn't exist)
        
            // writes data to file
            FileOutputStream fos = new FileOutputStream(this.file);
            byte[] buffer = new byte[1024];
            while (fileData.read(buffer) > 0) {
                fos.write(buffer);
            }
        } catch (IOException ex) {
            Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int getId(){
        return this.id;
    }
    
    public int getToId(){
        return this.toId;
    }
    
    public int getFromId(){
        return this.fromId;
    }
    
    public String getFilename(){
        return this.filename;
    }
    
    public File getFile(){
        return this.file;
    }
}
