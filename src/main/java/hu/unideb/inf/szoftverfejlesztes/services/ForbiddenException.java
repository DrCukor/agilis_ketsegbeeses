package hu.unideb.inf.szoftverfejlesztes.services;

// Jogosultság hiányában dobott kivétel (a GlobalExceptionHandler 403-ra alakítja)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
