package com.example.personalinfoservice.common.error;

import java.time.Instant;
import java.util.List;

/**
 * The project's standard error envelope, documented in
 * docs/api/contracts.md ("Error response format") and mirrored in
 * docs/api/openapi.yaml as {@code ErrorResponse}. Every non-2xx response
 * from this service uses this shape — do not hand-roll error bodies in
 * individual controllers.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<ValidationError> validationErrors
) {

    public record ValidationError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null);
    }

    public static ErrorResponse ofValidation(
            int status, String error, String message, String path, List<ValidationError> validationErrors
    ) {
        return new ErrorResponse(status, error, message, path, Instant.now(), validationErrors);
    }
}
