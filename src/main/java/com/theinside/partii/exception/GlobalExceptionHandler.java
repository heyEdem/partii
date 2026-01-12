package com.theinside.partii.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URI = "https://api.partii.com/problems/";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "not-found"));
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Validation Failed");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        logger.warn("Request validation failed: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Validation Failed");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", fieldErrors);

        return problem;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorizedException(UnauthorizedException ex) {
        logger.warn("Unauthorized access attempt: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "unauthorized"));
        problem.setTitle("Unauthorized");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("Bad credentials provided");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "bad-credentials"));
        problem.setTitle("Authentication Failed");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException ex) {
        logger.warn("JWT validation failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired token"
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "invalid-token"));
        problem.setTitle("Token Validation Failed");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication required"
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "authentication-required"));
        problem.setTitle("Authentication Required");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource"
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "forbidden"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(VerificationFailedException.class)
    public ProblemDetail handleVerificationFailedException(VerificationFailedException ex) {
        logger.warn("Verification failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "verification-failed"));
        problem.setTitle("Verification Failed");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        logger.error("Illegal state: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "conflict"));
        problem.setTitle("Operation Not Allowed");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problem.setType(URI.create(PROBLEM_BASE_URI + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}
