package br.com.ccs.exceptions;

public class LockModeException extends RuntimeException {
    public LockModeException(String message) {
        super(message);
    }
}
