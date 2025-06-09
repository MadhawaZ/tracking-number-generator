package org.example.controller;

import org.example.service.TrackingNumberGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TrackingNumberGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleTrackingNumberGenerationException(
            TrackingNumberGenerationException ex) {
        
        logger.error("Tracking number generation failed", ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "TRACKING_NUMBER_GENERATION_FAILED");
        errorResponse.put("message", "Unable to generate tracking number. Please try again.");
        errorResponse.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        logger.warn("Validation failed for request", ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        errorResponse.put("error", "VALIDATION_FAILED");
        errorResponse.put("message", "Request validation failed");
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameterException(
            MissingServletRequestParameterException ex) {
        
        logger.warn("Missing required parameter: {}", ex.getParameterName());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "MISSING_PARAMETER");
        errorResponse.put("message", "Required parameter '" + ex.getParameterName() + "' is missing");
        errorResponse.put("parameter", ex.getParameterName());
        errorResponse.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        
        logger.warn("Type mismatch for parameter: {}", ex.getName());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "INVALID_PARAMETER_TYPE");
        errorResponse.put("message", "Invalid type for parameter '" + ex.getName() + "'");
        errorResponse.put("parameter", ex.getName());
        errorResponse.put("expectedType", ex.getRequiredType().getSimpleName());
        errorResponse.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        
        logger.error("Unexpected error occurred", ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "INTERNAL_SERVER_ERROR");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
