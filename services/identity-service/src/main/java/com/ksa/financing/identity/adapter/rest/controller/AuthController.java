package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.identity.application.dto.AuthRequest;
import com.ksa.financing.identity.application.dto.AuthResponse;
import com.ksa.financing.identity.application.dto.LoginWithMobilePinRequest;
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
import com.ksa.financing.identity.domain.port.in.ChangePasscodeUseCase;
import com.ksa.financing.identity.domain.port.in.ForgotPasscodeUseCase;
import com.ksa.financing.identity.domain.port.in.VerifyMpinUseCase;
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
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
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
    private final ChangePasscodeUseCase changePasscodeUseCase;
    private final ForgotPasscodeUseCase forgotPasscodeUseCase;
    private final VerifyMpinUseCase verifyMpinUseCase;

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
                "Bearer",
                null,
                null,
                null,
                null,
                result.name()
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
                "Bearer",
                null,
                null,
                null,
                null,
                result.name()
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
                request.nationalId(), request.mobileNumber(), request.globalUid(), request.firstName()
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
                result.expiresIn(), "Bearer", result.customerId(),
                result.pepStatus(), result.nationalId(), result.mobileNumber(), result.name()
        );

        log.info("PIN login successful for NID ending in: {}", maskNid(request.nationalId()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-with-mobile-pin")
    @Operation(summary = "Login with mobile number and PIN",
            description = "Authenticates a customer using their mobile number (international format) and 6-digit PIN. "
                    + "Same as login-with-pin but uses mobile number instead of NID for user lookup.")
    @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request - mobile or PIN format incorrect")
    @ApiResponse(responseCode = "404", description = "User not found for the given mobile number")
    @ApiResponse(responseCode = "422", description = "Invalid PIN or PIN not set for this account")
    public ResponseEntity<AuthResponse> loginWithMobilePin(
            @Valid @RequestBody LoginWithMobilePinRequest request) {
        log.info("Mobile PIN login request for mobile ending in: ****{}",
                request.mobileNumber().substring(request.mobileNumber().length() - 4));

        var command = new LoginWithPinUseCase.LoginWithMobileCommand(
                request.mobileNumber(), request.pin()
        );

        var result = loginWithPinUseCase.loginWithMobile(command);

        var response = new AuthResponse(
                result.accessToken(), result.refreshToken(),
                result.expiresIn(), "Bearer", result.customerId(),
                result.pepStatus(), result.nationalId(), result.mobileNumber(), result.name()
        );

        log.info("Mobile PIN login successful for mobile ending in: ****{}",
                request.mobileNumber().substring(request.mobileNumber().length() - 4));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes all active Keycloak sessions for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Map<String, Object>> logout(@AuthenticationPrincipal Jwt jwt,
                                                      jakarta.servlet.http.HttpServletRequest request) {
        if (jwt == null) {
            // Extract token manually from header as fallback (e.g. when token is valid but SecurityContext not set)
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Unauthorized",
                        "message", "Valid Bearer token required for logout"
                ));
            }
            // Try to decode the token manually to get the subject
            try {
                String token = authHeader.substring(7);
                com.nimbusds.jwt.JWT parsedJwt = com.nimbusds.jwt.JWTParser.parse(token);
                String subject = parsedJwt.getJWTClaimsSet().getSubject();
                UUID keycloakUserId = UUID.fromString(subject);
                log.info("Logout (fallback) for Keycloak user: {}", keycloakUserId);
                logoutUseCase.logout(keycloakUserId);
                return ResponseEntity.ok(Map.of("message", "Logged out successfully", "loggedOut", true));
            } catch (Exception e) {
                log.warn("Logout failed — could not parse token: {}", e.getMessage());
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Unauthorized",
                        "message", "Invalid or expired token"
                ));
            }
        }
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        log.info("Logout request for Keycloak user: {}", keycloakUserId);
        logoutUseCase.logout(keycloakUserId);
        log.info("User logged out: {}", keycloakUserId);
        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "loggedOut", true
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Passcode Management — Mobile App
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/change-passcode")
    @Operation(summary = "Change passcode",
            description = "Changes the 6-digit app passcode for the authenticated user. Requires current passcode for verification.")
    @ApiResponse(responseCode = "200", description = "Passcode changed successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "422", description = "Current passcode incorrect or new passcodes do not match")
    public ResponseEntity<Map<String, Object>> changePasscode(
            @Valid @RequestBody ChangePasscodeRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        log.info("Change passcode request for keycloakUserId={}", keycloakUserId);

        var command = new ChangePasscodeUseCase.ChangePasscodeCommand(
                keycloakUserId,
                request.newPasscode(),
                request.confirmPasscode()
        );

        var result = changePasscodeUseCase.changePasscode(command);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
    }

    @PostMapping("/forgot-passcode/send-otp")
    @Operation(summary = "Forgot passcode — send OTP",
            description = "Sends a 6-digit OTP to the registered mobile number for passcode reset. "
                    + "Returns a workflowId that identifies the active reset session (valid 10 minutes).")
    @ApiResponse(responseCode = "200", description = "OTP sent successfully")
    @ApiResponse(responseCode = "404", description = "No account found for given mobile number")
    public ResponseEntity<Map<String, Object>> forgotPasscodeSendOtp(
            @Valid @RequestBody ForgotPasscodeSendOtpRequest request) {

        log.info("Forgot passcode OTP request for mobile ****{}",
                request.mobileNumber().length() > 4
                        ? request.mobileNumber().substring(request.mobileNumber().length() - 4) : "****");

        var command = new ForgotPasscodeUseCase.SendOtpCommand(request.mobileNumber());
        var result  = forgotPasscodeUseCase.sendOtp(command);

        return ResponseEntity.ok(Map.of(
                "sent",         result.sent(),
                "maskedMobile", result.maskedMobile(),
                "workflowId",   result.workflowId()
        ));
    }

    @PostMapping("/forgot-passcode/verify-otp")
    @Operation(summary = "Forgot passcode — verify OTP",
            description = "Verifies the OTP sent to the registered mobile number. "
                    + "Must be called after send-otp and before reset.")
    @ApiResponse(responseCode = "200", description = "OTP verified successfully")
    @ApiResponse(responseCode = "422", description = "Invalid or expired OTP")
    public ResponseEntity<Map<String, Object>> forgotPasscodeVerifyOtp(
            @Valid @RequestBody ForgotPasscodeVerifyOtpRequest request) {

        log.info("Forgot passcode OTP verification for mobile ****{}",
                request.mobileNumber().length() > 4
                        ? request.mobileNumber().substring(request.mobileNumber().length() - 4) : "****");

        var command = new ForgotPasscodeUseCase.VerifyOtpCommand(
                request.mobileNumber(),
                request.otp()
        );

        var result = forgotPasscodeUseCase.verifyOtp(command);
        // failures throw BusinessException — this path is success-only
        return ResponseEntity.ok(Map.of("verified", result.valid(), "message", result.message()));
    }

    @PostMapping("/forgot-passcode/reset")
    @Operation(summary = "Forgot passcode — reset passcode",
            description = "Resets the app passcode after OTP has been verified via verify-otp. "
                    + "Signals the Temporal workflow to complete the passcode reset.")
    @ApiResponse(responseCode = "200", description = "Passcode reset successfully")
    @ApiResponse(responseCode = "422", description = "Session expired or passcodes do not match")
    public ResponseEntity<Map<String, Object>> forgotPasscodeReset(
            @Valid @RequestBody ForgotPasscodeResetRequest request) {

        log.info("Forgot passcode reset for mobile ****{}",
                request.mobileNumber().length() > 4
                        ? request.mobileNumber().substring(request.mobileNumber().length() - 4) : "****");

        var command = new ForgotPasscodeUseCase.ResetPasscodeCommand(
                request.mobileNumber(),
                request.newPasscode(),
                request.confirmPasscode()
        );

        var result = forgotPasscodeUseCase.resetPasscode(command);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
    }

    @PostMapping("/verify-mpin")
    @Operation(summary = "Verify MPIN",
            description = "Checks whether the given MPIN is valid for the authenticated user (identified via Bearer JWT). "
                    + "Returns {valid: true} when the MPIN matches, throws 422 on mismatch.")
    @ApiResponse(responseCode = "200", description = "MPIN is valid")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "422", description = "Invalid MPIN or MPIN not set")
    public ResponseEntity<Map<String, Object>> verifyMpin(
            @Valid @RequestBody VerifyMpinRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        log.info("Verify MPIN request for keycloakUserId={}", keycloakUserId);

        var command = new VerifyMpinUseCase.VerifyMpinCommand(keycloakUserId, request.mpin());
        var result = verifyMpinUseCase.verifyMpin(command);
        return ResponseEntity.ok(Map.of("valid", result.valid(), "message", result.message()));
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
    public ResponseEntity<SsoLoginUrlResponse> getSsoLoginUrl(
            @RequestParam(value = "redirect_uri", required = false) String redirectUri) {
        log.info("SSO login URL requested, redirect_uri: {}", redirectUri);
        return ResponseEntity.ok(ssoUseCase.generateLoginUrl(redirectUri));
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

    public record ChangePasscodeRequest(
            @NotBlank(message = "New passcode is required")
            @Pattern(regexp = "\\d{6}", message = "New passcode must be exactly 6 digits")
            String newPasscode,
            @NotBlank(message = "Confirm passcode is required")
            @Pattern(regexp = "\\d{6}", message = "Confirm passcode must be exactly 6 digits")
            String confirmPasscode
    ) {}

    public record ForgotPasscodeSendOtpRequest(
            @NotBlank(message = "Mobile number is required") String mobileNumber
    ) {}

    public record ForgotPasscodeVerifyOtpRequest(
            @NotBlank(message = "Mobile number is required") String mobileNumber,
            @NotBlank(message = "OTP is required")
            @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
            String otp
    ) {}

    public record ForgotPasscodeResetRequest(
            @NotBlank(message = "Mobile number is required") String mobileNumber,
            @NotBlank(message = "New passcode is required")
            @Pattern(regexp = "\\d{6}", message = "New passcode must be exactly 6 digits")
            String newPasscode,
            @NotBlank(message = "Confirm passcode is required")
            @Pattern(regexp = "\\d{6}", message = "Confirm passcode must be exactly 6 digits")
            String confirmPasscode
    ) {}

    public record VerifyMpinRequest(
            @NotBlank(message = "MPIN is required")
            @Pattern(regexp = "\\d{6}", message = "MPIN must be exactly 6 digits")
            String mpin
    ) {}
}
