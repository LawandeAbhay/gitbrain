package dev.abhay.gitbrain.exceptions;


public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message){
        super(message);
    }
}
