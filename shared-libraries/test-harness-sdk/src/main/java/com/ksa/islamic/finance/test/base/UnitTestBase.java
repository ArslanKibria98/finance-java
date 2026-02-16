package com.ksa.islamic.finance.test.base;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Base class for unit tests with common utilities
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public abstract class UnitTestBase {

    protected final Random random = ThreadLocalRandom.current();
    protected Clock fixedClock;
    protected LocalDateTime testTime;

    @BeforeEach
    void setUpBase(TestInfo testInfo) {
        log.debug("Running test: {}", testInfo.getDisplayName());

        // Initialize fixed time for testing
        testTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        fixedClock = Clock.fixed(testTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Call subclass setup
        setUp();
    }

    /**
     * Hook for additional setup in subclasses
     */
    protected void setUp() {
        // Override in subclasses if needed
    }

    /**
     * Generate random string
     */
    protected String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random email
     */
    protected String randomEmail() {
        return randomString(8).toLowerCase() + "@" + randomString(6).toLowerCase() + ".com";
    }

    /**
     * Generate random phone number
     */
    protected String randomPhone() {
        return "+966" + (500000000 + random.nextInt(100000000));
    }

    /**
     * Generate random ID
     */
    protected String randomId() {
        return String.format("%08d", random.nextInt(100000000));
    }

    /**
     * Advance test time
     */
    protected void advanceTime(Duration duration) {
        testTime = testTime.plus(duration);
        fixedClock = Clock.fixed(testTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }

    /**
     * Set specific test time
     */
    protected void setTestTime(LocalDateTime time) {
        testTime = time;
        fixedClock = Clock.fixed(testTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }

    /**
     * Get current test time
     */
    protected LocalDateTime getCurrentTime() {
        return LocalDateTime.now(fixedClock);
    }

    /**
     * Assert exception with message
     */
    protected <T extends Throwable> void assertThrowsWithMessage(
        Class<T> expectedType,
        Runnable runnable,
        String expectedMessage
    ) {
        try {
            runnable.run();
            throw new AssertionError("Expected " + expectedType.getName() + " but nothing was thrown");
        } catch (Throwable actualException) {
            if (!expectedType.isInstance(actualException)) {
                throw new AssertionError(
                    "Expected " + expectedType.getName() + " but got " + actualException.getClass().getName(),
                    actualException
                );
            }

            String actualMessage = actualException.getMessage();
            if (actualMessage == null || !actualMessage.contains(expectedMessage)) {
                throw new AssertionError(
                    "Expected exception message containing: '" + expectedMessage +
                    "' but got: '" + actualMessage + "'",
                    actualException
                );
            }
        }
    }

    /**
     * Sleep for testing
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}