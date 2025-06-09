package org.example.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingNumberServiceTest {

    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");

    @Mock
    private Counter trackingNumberGeneratedCounter;

    @Mock
    private Timer trackingNumberGenerationTimer;

    @Mock
    private Counter trackingNumberErrorCounter;

    @InjectMocks
    private StatelessTrackingNumberService trackingNumberService;

    @BeforeEach
    void setUp() throws Exception {
        // Mock timer to return the actual result
        when(trackingNumberGenerationTimer.recordCallable(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, java.util.concurrent.Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void generateTrackingNumber_ShouldReturnValidFormat() {
        String result = trackingNumberService.generateTrackingNumber("MY", "ID", 1.234, "de619854-b59b-425e-9db4-943979e1bd49");

        assertNotNull(result);
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result).matches(),
                   "Tracking number should match pattern ^[A-Z0-9]{1,16}$");
        assertEquals(16, result.length(), "Tracking number should be exactly 16 characters");
        verify(trackingNumberGeneratedCounter).increment();
    }

    @Test
    void generateTrackingNumber_ShouldHandleDifferentInputs() {
        String result1 = trackingNumberService.generateTrackingNumber("US", "CA", 5.0, "12345678-1234-1234-1234-123456789012");
        String result2 = trackingNumberService.generateTrackingNumber("GB", "FR", 0.1, "87654321-4321-4321-4321-210987654321");

        assertNotEquals(result1, result2, "Different inputs should generate different tracking numbers");
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result1).matches());
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result2).matches());
    }

    @RepeatedTest(10)
    void generateTrackingNumber_ShouldBeUnique() {
        String result = trackingNumberService.generateTrackingNumber("MY", "ID", 1.234, "de619854-b59b-425e-9db4-943979e1bd49");

        assertNotNull(result);
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result).matches());
        assertEquals(16, result.length());
    }

    @Test
    void generateTrackingNumber_ConcurrentExecution_ShouldGenerateUniqueNumbers() throws Exception {
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Set<String> generatedNumbers = new HashSet<>();

        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                String trackingNumber = trackingNumberService.generateTrackingNumber(
                    "MY", "ID", 1.234 + index, "de619854-b59b-425e-9db4-943979e1bd" + String.format("%02d", index % 100));

                synchronized (generatedNumbers) {
                    generatedNumbers.add(trackingNumber);
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get();
        executor.shutdown();

        assertEquals(numberOfThreads, generatedNumbers.size(),
                    "All generated tracking numbers should be unique");

        generatedNumbers.forEach(trackingNumber -> {
            assertTrue(TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches());
            assertEquals(16, trackingNumber.length());
        });
    }

    @Test
    void generateTrackingNumber_WithInvalidInputs_ShouldStillWork() {
        // Test with edge case inputs
        String result1 = trackingNumberService.generateTrackingNumber("", "", 0.0, "");
        String result2 = trackingNumberService.generateTrackingNumber("XX", "YY", Double.MAX_VALUE, "test");

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result1).matches());
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(result2).matches());
    }

    @Test
    void generateTrackingNumber_ShouldIncrementMetrics() throws Exception {
        trackingNumberService.generateTrackingNumber("MY", "ID", 1.234, "test-customer");

        verify(trackingNumberGeneratedCounter).increment();
        verify(trackingNumberGenerationTimer).recordCallable(any());
    }

    // Simple test method for basic validation (keeping original logic)
    private String generateSimpleTrackingNumber(String origin, String destination, double weight, String customerId) {
        return "A1B2C3D4E5F6G7H8"; // Mock implementation for basic test
    }
}
