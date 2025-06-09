package org.example.performance;

import org.example.service.StatelessTrackingNumberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.org.example=WARN" // Reduce logging for performance tests
})
class TrackingNumberPerformanceTest {

    @Autowired
    private StatelessTrackingNumberService trackingNumberService;

    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");

    @Test
    void testHighConcurrencyPerformance() throws Exception {
        int numberOfThreads = 50;
        int requestsPerThread = 100;
        int totalRequests = numberOfThreads * requestsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        String trackingNumber = trackingNumberService.generateTrackingNumber(
                            "MY", "ID", 1.234 + j, 
                            String.format("de619854-b59b-425e-9db4-943979e1bd%02d", (threadId * requestsPerThread + j) % 100)
                        );
                        
                        if (TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches()) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            }, executor);
        }
        
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Performance Test Results:");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Requests/second: " + (totalRequests * 1000.0 / duration));
        
        assertEquals(totalRequests, successCount.get(), "All requests should succeed");
        assertEquals(0, errorCount.get(), "No errors should occur");
        assertTrue(duration < 10000, "Should complete within 10 seconds");
    }

    @Test
    void testSingleThreadPerformance() {
        int numberOfRequests = 1000;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfRequests; i++) {
            String trackingNumber = trackingNumberService.generateTrackingNumber(
                "MY", "ID", 1.234 + i, 
                String.format("de619854-b59b-425e-9db4-943979e1bd%02d", i % 100)
            );
            
            assertTrue(TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches());
            assertEquals(16, trackingNumber.length());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Single Thread Performance:");
        System.out.println("Requests: " + numberOfRequests);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Requests/second: " + (numberOfRequests * 1000.0 / duration));
        
        assertTrue(duration < 5000, "Should complete within 5 seconds");
    }

    @Test
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate many tracking numbers
        for (int i = 0; i < 10000; i++) {
            trackingNumberService.generateTrackingNumber(
                "MY", "ID", 1.234 + i, 
                String.format("de619854-b59b-425e-9db4-943979e1bd%02d", i % 100)
            );
        }
        
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        
        System.out.println("Memory Usage Test:");
        System.out.println("Initial Memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final Memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory Used: " + (memoryUsed / 1024 / 1024) + " MB");
        
        // Should not use more than 50MB for 10k requests
        assertTrue(memoryUsed < 50 * 1024 * 1024, "Memory usage should be reasonable");
    }
}
