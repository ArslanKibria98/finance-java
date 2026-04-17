package com.ksa.financing.customer.application.dto;

import jakarta.validation.constraints.Email;

/**
 * Request DTO for the mobile-app "Update My Profile" endpoint.
 * Email and profilePictureKey are updatable by the customer themselves.
 * Name, mobile, and address changes require agent/CSA action.
 */
public record UpdateMyProfileRequest(

        @Email(message = "Email must be a valid email address")
        String email,

        String profilePicture

) {}
