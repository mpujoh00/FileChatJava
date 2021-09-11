package com.wut.filechatjava.exception;

public class ServerConnectionException extends Exception {

    public ServerConnectionException(String message, Exception e){
        super(message, e);
    }

}
