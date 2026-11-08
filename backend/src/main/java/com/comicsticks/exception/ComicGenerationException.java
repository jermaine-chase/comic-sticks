package com.comicsticks.exception;

import org.springframework.http.HttpStatus;

/**
 * A failure with a message that is safe to show the user. Anything sensitive, such as a
 * provider's raw response, belongs in the {@code cause}, which is logged and never returned.
 */
public class ComicGenerationException extends RuntimeException {

    private final HttpStatus status;

    public ComicGenerationException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ComicGenerationException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
