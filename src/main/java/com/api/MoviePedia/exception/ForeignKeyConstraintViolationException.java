package com.api.MoviePedia.exception;

public class ForeignKeyConstraintViolationException extends RuntimeException{
    public ForeignKeyConstraintViolationException(String message){
        super(message);
    }
}
