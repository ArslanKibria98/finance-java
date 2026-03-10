package com.ksa.financing.risk.workflow.activity.impl;

import com.ksa.islamic.orchestration.activity.risk.NidFormatValidationActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NidFormatValidationActivityImplTest {

    private NidFormatValidationActivityImpl activity;

    @BeforeEach
    void setUp() {
        activity = new NidFormatValidationActivityImpl();
    }

    @Test
    @DisplayName("Valid Saudi citizen NID (starts with 1) should pass")
    void validCitizenNid() {
        // Use a valid Luhn NID starting with 1
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput("1000000006"));
        assertTrue(result.valid());
        assertEquals("CITIZEN", result.idType());
        assertNull(result.failureReason());
    }

    @Test
    @DisplayName("Valid resident Iqama (starts with 2) should pass")
    void validResidentIqama() {
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput("2000000004"));
        assertTrue(result.valid());
        assertEquals("RESIDENT", result.idType());
        assertNull(result.failureReason());
    }

    @Test
    @DisplayName("NID with wrong length should fail")
    void wrongLength() {
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput("12345"));
        assertFalse(result.valid());
        assertNotNull(result.failureReason());
    }

    @Test
    @DisplayName("NID starting with 3 should fail")
    void wrongPrefix() {
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput("3000000002"));
        assertFalse(result.valid());
    }

    @Test
    @DisplayName("NID with non-numeric characters should fail")
    void nonNumeric() {
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput("1ABCDEFGH0"));
        assertFalse(result.valid());
    }

    @Test
    @DisplayName("Null NID should fail")
    void nullNid() {
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput(null));
        assertFalse(result.valid());
        assertNotNull(result.failureReason());
    }

    @Test
    @DisplayName("Empty NID should fail")
    void emptyNid() {
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput(""));
        assertFalse(result.valid());
    }

    @Test
    @DisplayName("Valid format but invalid Luhn checksum should fail")
    void invalidLuhn() {
        // 1000000001 - valid format but should fail Luhn
        var result = activity.validate(new NidFormatValidationActivity.NidValidationInput("1000000001"));
        assertFalse(result.valid());
        assertEquals("CITIZEN", result.idType());
    }
}
