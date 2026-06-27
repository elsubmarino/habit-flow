package io.streak.habitflow.global.error.exception;

public class FileStorageException extends RuntimeException{
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
