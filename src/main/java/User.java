
class User {
    
    private int id;
    private String username;
    private String password;
    private String extensions;
    private boolean isAvailable;
    
    public User(int id, String username, String password, String extensions, boolean available){
        this.id = id;
        this.username = username;
        this.password = password;
        this.extensions = extensions;
        this.isAvailable = available;
    }
    
    public void connect(){
        this.isAvailable = true;
    }
    
    public void setExtensions(String extensions){
        this.extensions = extensions;
    }
    
    public int getId(){
        return this.id;
    }
    
    public String getUsername(){
        return this.username;
    }
    
    public String getPassword(){
        return this.password;
    }
    
    public boolean isAvailable(){
        return this.isAvailable;
    }
    
    public String getExtensions(){
        return this.extensions;
    }
}
