package org.example.controller;

import org.example.service.TrackingNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;

@RestController
public class TrackingNumberController {

    @Autowired
    private TrackingNumberService trackingNumberService;
    
    // Request validation interface
    public sealed interface ValidTrackingRequest permits TrackingNumberRequest {}
    
    @GetMapping("/next-tracking-number")
    public ResponseEntity<Map<String, Object>> getNextTrackingNumber(@Valid TrackingNumberRequest request) {
        return generateResponse(request.origin_country_id(), request.destination_country_id(),
                              request.weight(), request.customer_id());
    }

    
    private ResponseEntity<Map<String, Object>> generateResponse(String originCountryId, String destinationCountryId,
                                                                 double weight, String customerId) {
        String trackingNumber = trackingNumberService.generateTrackingNumber(
            originCountryId, destinationCountryId, weight, customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("tracking_number", trackingNumber);
        response.put("created_at", Instant.now().toString());
        response.put("origin_country_id", originCountryId);
        response.put("destination_country_id", destinationCountryId);
        
        return ResponseEntity.ok(response);
    }
    

    
    // Request data structure
    public record TrackingNumberRequest(
            @NotNull(message = "Origin country ID is required")
            @Pattern(regexp = "[A-Z]{2}", message = "Origin country code must be in ISO 3166-1 alpha-2 format")
            String origin_country_id,

            @NotNull(message = "Destination country ID is required")
            @Pattern(regexp = "[A-Z]{2}", message = "Destination country code must be in ISO 3166-1 alpha-2 format")
            String destination_country_id,

            @Positive(message = "Weight must be positive")
            double weight,

            @NotNull(message = "Customer ID is required")
            @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                     message = "Customer ID must be a valid UUID")
            String customer_id) implements ValidTrackingRequest {}
    

}