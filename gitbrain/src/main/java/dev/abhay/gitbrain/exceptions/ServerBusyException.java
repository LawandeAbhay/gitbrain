package dev.abhay.gitbrain.exceptions;

public class ServerBusyException extends RuntimeException{
    public ServerBusyException(String message){
        super(message);
    }
}
