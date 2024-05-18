package br.com.ccs.exceptions;

public class PrecoVendaInvalidoException extends RuntimeException {
    public PrecoVendaInvalidoException(String message) {
        super(message);
    }
}
