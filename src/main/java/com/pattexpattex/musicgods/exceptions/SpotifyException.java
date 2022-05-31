package com.pattexpattex.musicgods.exceptions;

public class SpotifyException extends RuntimeException {

    public final boolean isFromLoader;

    public SpotifyException(String message) {
        super(message);
        isFromLoader = message != null;
    }

    public SpotifyException(Throwable cause) {
        super(cause);
        isFromLoader = false;
    }
}
