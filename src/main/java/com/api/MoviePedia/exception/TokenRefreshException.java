package com.api.MoviePedia.exception;

public class TokenRefreshException extends RuntimeException{
    public TokenRefreshException(String message){
        super(message);
    }
}
