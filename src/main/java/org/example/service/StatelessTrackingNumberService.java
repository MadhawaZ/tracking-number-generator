package org.example.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for generating unique tracking numbers using cryptographic hashing.
 */
@Service
public class StatelessTrackingNumberService implements TrackingNumberService {

    private static final Logger logger = LoggerFactory.getLogger(StatelessTrackingNumberService.class);

    private final SecureRandom secureRandom = new SecureRandom();
    
    @Autowired
    private Counter trackingNumberGeneratedCounter;
    
    @Autowired
    private Timer trackingNumberGenerationTimer;
    
    @Autowired
    private Counter trackingNumberErrorCounter;
    
    @Override
    public String generateTrackingNumber(String originCountryId, String destinationCountryId,
                                        double weight, String customerId) {
        try {
            return trackingNumberGenerationTimer.recordCallable(() -> {
                logger.info("Generating tracking number for route: {} -> {}, weight: {}, customer: {}",
                           originCountryId, destinationCountryId, weight, customerId);
                
                try {
                    // Create input string with high-resolution timing and randomness
                    long nanoTime = System.nanoTime();
                    long currentTime = System.currentTimeMillis();
                    int randomInt = ThreadLocalRandom.current().nextInt();
                    long secureRandomLong = secureRandom.nextLong();

                    String input = originCountryId + destinationCountryId +
                                  String.format("%.3f", weight) + customerId +
                                  nanoTime + currentTime + randomInt + secureRandomLong;
                    
                    // Hash the input using SHA-256
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hashBytes = digest.digest(input.getBytes());

                    // Convert to hex string
                    StringBuilder hexString = new StringBuilder();
                    for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                        String hex = Integer.toHexString(0xff & hashBytes[i]);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex);
                    }
                    
                    // Format to 16 character uppercase string
                    String trackingNumber = hexString.toString().toUpperCase();
                    if (trackingNumber.length() > 16) {
                        trackingNumber = trackingNumber.substring(0, 16);
                    } else if (trackingNumber.length() < 16) {
                        // Pad if needed
                        while (trackingNumber.length() < 16) {
                            String padding = Integer.toHexString((int)(Math.random() * 16));
                            trackingNumber += padding.toUpperCase();
                        }
                        trackingNumber = trackingNumber.substring(0, 16);
                    }
                    
                    trackingNumberGeneratedCounter.increment();
                    logger.info("Successfully generated tracking number: {}", trackingNumber);
                    
                    return trackingNumber;
                    
                } catch (NoSuchAlgorithmException e) {
                    trackingNumberErrorCounter.increment();
                    logger.warn("SHA-256 not available, using fallback", e);

                    // Fallback generation method with randomness
                    long nanoTime = System.nanoTime();
                    long currentTime = System.currentTimeMillis();
                    int randomInt = ThreadLocalRandom.current().nextInt();

                    String combined = originCountryId + destinationCountryId +
                                    String.format("%.0f", weight * 1000) +
                                    customerId.substring(0, Math.min(8, customerId.length())) +
                                    nanoTime + currentTime + randomInt;
                    
                    // Clean and format the string
                    String trackingNumber = combined.replaceAll("[^A-Z0-9]", "").toUpperCase();

                    // Trim to 16 characters
                    if (trackingNumber.length() > 16) {
                        trackingNumber = trackingNumber.substring(0, 16);
                    } else if (trackingNumber.length() < 16) {
                        // Pad with numeric characters
                        String padding = String.valueOf(System.nanoTime()).replaceAll("[^0-9]", "");
                        trackingNumber += padding;
                        trackingNumber = trackingNumber.substring(0, 16);
                    }
                    
                    trackingNumberGeneratedCounter.increment();
                    logger.info("Generated tracking number using fallback: {}", trackingNumber);
                    
                    return trackingNumber;
                }
            });
        } catch (Exception e) {
            trackingNumberErrorCounter.increment();
            logger.error("Error generating tracking number", e);
            throw new TrackingNumberGenerationException("Failed to generate tracking number", e);
        }
    }
}
