package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.identity.application.dto.AuthRequest;
import com.ksa.financing.identity.application.dto.AuthResponse;
import com.ksa.financing.identity.application.dto.LoginWithPinRequest;
import com.ksa.financing.identity.application.dto.OnboardingRegisterRequest;
import com.ksa.financing.identity.application.dto.OnboardingRegisterResponse;
import com.ksa.financing.identity.application.dto.RegisterRequest;
import com.ksa.financing.identity.application.dto.RegisterResponse;
import com.ksa.financing.identity.application.dto.SsoLoginUrlResponse;
import com.ksa.financing.identity.application.dto.SsoTokenExchangeRequest;
import com.ksa.financing.identity.application.dto.SsoTokenResponse;
import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.port.in.AuthenticateUserUseCase;
import com.ksa.financing.identity.domain.port.in.LoginWithPinUseCase;
import com.ksa.financing.identity.domain.port.in.LogoutUseCase;
import com.ksa.financing.identity.domain.port.in.RegisterFromOnboardingUseCase;
import com.ksa.financing.identity.domain.port.in.RegisterUserUseCase;
import com.ksa.financing.identity.domain.port.in.SsoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration and authentication endpoints")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RegisterFromOnboardingUseCase registerFromOnboardingUseCase;
    private final LoginWithPinUseCase loginWithPinUseCase;
    private final SsoUseCase ssoUseCase;
    private final LogoutUseCase logoutUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user in Keycloak and maps identity internally")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "User already exists")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.username());

        RegisterUserUseCase.RegisterUserCommand command = new RegisterUserUseCase.RegisterUserCommand(
                UUID.fromString(request.tenantId()),
                request.realm(),
                request.username(),
                request.email(),
                request.mobileNumber(),
                request.password()
        );

        UserIdentity identity = registerUserUseCase.register(command);

        RegisterResponse response = new RegisterResponse(
                identity.getId(),
                identity.getKeycloakUserId(),
                identity.getKeycloakUsername(),
                identity.getStatus().name()
        );

        log.info("User registered successfully with ID: {}", identity.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user credentials and returns JWT tokens")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login request received for username: {}", request.username());

        AuthenticateUserUseCase.AuthCommand command = new AuthenticateUserUseCase.AuthCommand(
                request.username(),
                request.password(),
                request.realm()
        );

        AuthenticateUserUseCase.AuthResult result = authenticateUserUseCase.authenticate(command);

        AuthResponse response = new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                "Bearer"
        );

        log.info("User authenticated successfully: {}", request.username());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchanges a refresh token for a new access token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        AuthenticateUserUseCase.AuthResult result = authenticateUserUseCase.refreshToken(request.refreshToken());

        AuthResponse response = new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                "Bearer"
        );

        log.info("Token refreshed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/onboarding-register")
    @Operation(summary = "Register from onboarding",
            description = "Creates Keycloak user from NID after OTP verification and returns JWT tokens")
    @ApiResponse(responseCode = "201", description = "User registered and tokens returned")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "User already exists")
    public ResponseEntity<OnboardingRegisterResponse> registerFromOnboarding(
            @Valid @RequestBody OnboardingRegisterRequest request) {
        // Validate NID format (10 digits, starts with 1=citizen or 2=resident)
        NationalId.of(request.nationalId());

        log.info("Onboarding registration request for NID: {}", maskNid(request.nationalId()));

        var command = new RegisterFromOnboardingUseCase.RegisterFromOnboardingCommand(
                request.nationalId(), request.mobileNumber(), request.globalUid()
        );

        var result = registerFromOnboardingUseCase.register(command);

        var response = new OnboardingRegisterResponse(
                result.accessToken(), result.refreshToken(), result.expiresIn(),
                "Bearer", result.keycloakUserId()
        );

        log.info("Onboarding registration completed for NID: {}", maskNid(request.nationalId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login-with-pin")
    @Operation(summary = "Login with NID and PIN",
            description = "Authenticates a customer using their 10-digit Saudi National ID and 6-digit PIN "
                    + "that was set during the onboarding process. Returns JWT access and refresh tokens from Keycloak.")
    @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request - NID or PIN format incorrect")
    @ApiResponse(responseCode = "404", description = "User not found for the given National ID")
    @ApiResponse(responseCode = "422", description = "Invalid PIN or PIN not set for this account")
    public ResponseEntity<AuthResponse> loginWithPin(@Valid @RequestBody LoginWithPinRequest request) {
        // Validate NID format (10 digits, starts with 1=citizen or 2=resident)
        NationalId.of(request.nationalId());

        log.info("PIN login request for NID ending in: {}", maskNid(request.nationalId()));

        var command = new LoginWithPinUseCase.LoginWithPinCommand(
                request.nationalId(), request.pin()
        );

        var result = loginWithPinUseCase.login(command);

        var response = new AuthResponse(
                result.accessToken(), result.refreshToken(),
                result.expiresIn(), "Bearer"
        );

        log.info("PIN login successful for NID ending in: {}", maskNid(request.nationalId()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes all active Keycloak sessions for the authenticated user")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        log.info("Logout request for Keycloak user: {}", keycloakUserId);
        logoutUseCase.logout(keycloakUserId);
        log.info("User logged out: {}", keycloakUserId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SSO Endpoints — Authorization Code Flow with PKCE
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/sso/login-url")
    @Operation(
            summary = "Get SSO login URL",
            description = """
                    Frontend is endpoint ko call kare login ke liye.
                    Response mein authUrl hoga — frontend isko Keycloak pe redirect kare ga.
                    State bhi save karo kyunki /sso/token call mein zaroori hoga.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Login URL generated successfully")
    public ResponseEntity<SsoLoginUrlResponse> getSsoLoginUrl() {
        log.info("SSO login URL requested");
        return ResponseEntity.ok(ssoUseCase.generateLoginUrl());
    }

    @PostMapping("/sso/token")
    @Operation(
            summary = "Exchange authorization code for tokens",
            description = """
                    Keycloak redirect ke baad frontend yeh call kare.
                    URL mein ?code=xxx&state=yyy hoga — dono yahan bhejo.
                    Backend code + PKCE verifier se Keycloak pe token exchange karta hai.
                    Response mein access_token, refresh_token aur roles aate hain.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Tokens returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired code/state")
    public ResponseEntity<SsoTokenResponse> exchangeSsoToken(
            @Valid @RequestBody SsoTokenExchangeRequest request) {
        log.info("SSO token exchange requested for state: {}", request.state());
        return ResponseEntity.ok(ssoUseCase.exchangeToken(request));
    }

    private String maskNid(String nid) {
        if (nid == null || nid.length() < 4) {
            return "***";
        }
        return "***" + nid.substring(nid.length() - 4);
    }

    /**
     * Request body for the refresh token endpoint.
     */
    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required") String refreshToken
    ) {}
}
