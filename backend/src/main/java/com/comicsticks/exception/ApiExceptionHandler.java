package com.comicsticks.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Returns every error as an RFC 9457 problem document whose {@code detail} the UI can display. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ComicGenerationException.class)
    ProblemDetail handleGenerationFailure(ComicGenerationException e) {
        if (e.status().is5xxServerError()) {
            log.error("Comic generation failed: {}", e.getMessage(), e);
        }
        return ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return handleExceptionInternal(e, ProblemDetail.forStatusAndDetail(status, detail), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus tooLarge = HttpStatus.CONTENT_TOO_LARGE;
        ProblemDetail body = ProblemDetail.forStatusAndDetail(tooLarge, "That file is too large. The limit is 10 MB.");
        return handleExceptionInternal(e, body, headers, tooLarge, request);
    }
}
