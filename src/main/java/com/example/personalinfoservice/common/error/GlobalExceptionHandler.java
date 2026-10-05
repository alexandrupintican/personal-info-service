package com.example.personalinfoservice.common.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Central mapping from exceptions to the project's standard error envelope
 * ({@link ErrorResponse}, see docs/api/contracts.md). Individual controllers
 * should not build their own error responses.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} rather than hand-rolling
 * {@code @ExceptionHandler} methods for every Spring MVC exception type
 * (bad request body, unsupported method, 404 for an unmapped route, etc.):
 * Spring already resolves each of those to the correct {@link HttpStatusCode}.
 * Re-implementing that mapping ad hoc risks getting it wrong — e.g. an
 * earlier version of this class caught {@code ResponseStatusException} but
 * not sibling exceptions like {@code NoResourceFoundException} (thrown for
 * any unmapped route), which meant a plain 404 was being reported as a 500.
 * Overriding just {@link #handleExceptionInternal} reshapes every one of
 * those already-correctly-classified exceptions into our envelope in a
 * single place.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Anything Spring MVC itself resolves to a specific HTTP status
     * (validation failures, unmapped routes, unsupported methods, malformed
     * request bodies, etc. — see {@link ResponseEntityExceptionHandler}'s
     * javadoc for the full list) ends up here with that status already
     * decided; this only reshapes the response body.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request
    ) {
        String path = requestPath(request);
        String errorCode = errorCode(statusCode);

        ErrorResponse errorResponse;
        if (ex instanceof MethodArgumentNotValidException validationException) {
            List<ErrorResponse.ValidationError> validationErrors = validationException.getBindingResult()
                    .getFieldErrors().stream()
                    .map(fieldError -> new ErrorResponse.ValidationError(
                            fieldError.getField(), fieldError.getDefaultMessage()))
                    .toList();
            errorResponse = ErrorResponse.ofValidation(
                    statusCode.value(), errorCode, "Request validation failed", path, validationErrors);
        } else {
            String message = ex.getMessage() != null ? ex.getMessage() : "Request could not be completed";
            errorResponse = ErrorResponse.of(statusCode.value(), errorCode, message, path);
        }

        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
    }

    /** Catch-all for exceptions Spring MVC doesn't already classify above: always a 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    private static String errorCode(HttpStatusCode statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved.name() : "ERROR";
    }
}
