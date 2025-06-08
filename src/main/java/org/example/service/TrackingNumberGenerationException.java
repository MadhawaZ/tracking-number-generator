package org.example.service;

/**
 * Exception thrown when tracking number generation fails
 */
public class TrackingNumberGenerationException extends RuntimeException {
    
    public TrackingNumberGenerationException(String message) {
        super(message);
    }
    
    public TrackingNumberGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
