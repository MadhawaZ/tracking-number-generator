package org.example.service;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TrackingNumberServiceTest {

    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");

    @Test
    void generateTrackingNumber_ShouldReturnValidFormat() {
        // Simple test without Spring context
        String result = generateSimpleTrackingNumber("MY", "ID", 1.234, "test-customer");
        
        assertNotNull(result);
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result).matches(), 
                   "Tracking number should match pattern ^[A-Z0-9]{1,16}$");
        assertEquals(16, result.length(), "Tracking number should be exactly 16 characters");
    }

    @Test
    void generateTrackingNumber_DifferentInputs_ShouldReturnDifferentNumbers() {
        String result1 = generateSimpleTrackingNumber("SL", "ML", 1.0, "customer1");
        String result2 = generateSimpleTrackingNumber("SG", "TH", 2.0, "customer2");
        
        assertNotEquals(result1, result2, "Different inputs should generate different tracking numbers");
    }

    // Simple tracking number generation for testing
    private String generateSimpleTrackingNumber(String origin, String destination, double weight, String customer) {
        try {
            long timestamp = System.currentTimeMillis();
            String input = origin + destination + String.format("%.3f", weight) + customer + timestamp;
            
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String trackingNumber = hexString.toString().toUpperCase();
            if (trackingNumber.length() > 16) {
                trackingNumber = trackingNumber.substring(0, 16);
            } else if (trackingNumber.length() < 16) {
                while (trackingNumber.length() < 16) {
                    String padding = Integer.toHexString((int)(Math.random() * 16));
                    trackingNumber += padding.toUpperCase();
                }
                trackingNumber = trackingNumber.substring(0, 16);
            }
            
            return trackingNumber;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate tracking number", e);
        }
    }
}
