package com.ksa.financing.customer.adapter.rest.controller;

import com.ksa.financing.customer.application.dto.AddBankAccountRequest;
import com.ksa.financing.customer.application.dto.AddEmploymentRequest;
import com.ksa.financing.customer.application.dto.CreateCustomerRequest;
import com.ksa.financing.customer.application.dto.Customer360Response;
import com.ksa.financing.customer.application.dto.CustomerResponse;
import com.ksa.financing.customer.application.dto.UpdateCustomerRequest;
import com.ksa.financing.customer.application.dto.UpdateMyProfileRequest;
import com.ksa.financing.customer.application.usecase.GetCustomer360Service;
import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.EmploymentInfo;
import com.ksa.financing.customer.domain.model.EmploymentType;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.port.in.CreateCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.port.in.UpdateCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.EmploymentInfoRepository;
import com.ksa.financing.customer.domain.port.out.WalletPort;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customers", description = "Customer management endpoints")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final ManageBankAccountsUseCase manageBankAccountsUseCase;
    private final WalletPort walletPort;
    private final com.ksa.financing.customer.domain.port.out.PiiVaultPort piiVaultPort;
    private final GetCustomer360Service getCustomer360Service;
    // TODO: Refactor to use AddEmploymentUseCase instead of direct repository access (hexagonal violation)
    private final EmploymentInfoRepository employmentInfoRepository;

    @SecuredEndpoint(obj = "customers", act = "create")
    @PostMapping
    @Operation(summary = "Create a new customer", description = "Registers a new individual customer")
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Customer with this national ID already exists")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = parseTenantIdOrDefault(request.tenantId(), jwt);
        UUID keycloakUserId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        log.info("Creating customer for tenant: {}, user: {}", tenantId, keycloakUserId);

        // Derive firstName/lastName from fullNameEn if split fields not provided
        String firstName = request.firstName();
        String lastName = request.lastName();
        if ((firstName == null || firstName.isBlank()) && request.fullNameEn() != null) {
            String[] parts = request.fullNameEn().trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : parts[0];
        }
        String firstNameAr = request.firstNameAr();
        String lastNameAr = request.lastNameAr();
        if ((firstNameAr == null || firstNameAr.isBlank()) && request.fullNameAr() != null) {
            String[] parts = request.fullNameAr().trim().split("\\s+", 2);
            firstNameAr = parts[0];
            lastNameAr = parts.length > 1 ? parts[1] : parts[0];
        }
        String residencyType = request.residencyType() != null ? request.residencyType() : "CITIZEN";
        String nationalIdType = request.nationalIdType() != null ? request.nationalIdType() : "NID";
        String nationality = request.nationality() != null ? request.nationality() : "SA";

        CreateCustomerUseCase.CreateCustomerCommand command = new CreateCustomerUseCase.CreateCustomerCommand(
                tenantId,
                request.nationalId(),
                nationalIdType,
                firstName != null ? firstName : "Unknown",
                request.middleName(),
                lastName != null ? lastName : "Unknown",
                firstNameAr,
                lastNameAr,
                request.dateOfBirth(),
                request.gender(),
                nationality,
                residencyType,
                request.mobileNumber(),
                request.email(),
                keycloakUserId,
                request.lifecycleStage(),
                null,  // globalUid — will be created via GPS call
                request.idempotencyKey()
        );

        Customer customer = createCustomerUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(customer));
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer details by internal ID")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        boolean superAdmin = isSuperAdmin(jwt);
        log.info("Getting customer by ID: {} superAdmin: {}", id, superAdmin);

        Customer customer = superAdmin
                ? getCustomerUseCase.getById(id)
                : getCustomerUseCase.getById(extractTenantId(jwt), id);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/{id}/detail")
    @Operation(summary = "Get full customer detail", description = "Returns customer data enriched with PII vault, bank accounts, employment info, and wallet IBAN")
    @ApiResponse(responseCode = "200", description = "Customer detail found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        boolean superAdmin = isSuperAdmin(jwt);
        log.info("Getting full customer detail for ID: {} superAdmin: {}", id, superAdmin);

        Customer customer = superAdmin
                ? getCustomerUseCase.getById(id)
                : getCustomerUseCase.getById(extractTenantId(jwt), id);

        // PII Vault data
        java.util.Map<String, String> piiData = java.util.Collections.emptyMap();
        if (customer.getGlobalUid() != null) {
            try {
                piiData = piiVaultPort.retrievePii(customer.getGlobalUid(), jwt.getTokenValue());
            } catch (Exception e) {
                log.warn("PII vault retrieval failed for globalUid={}: {}", customer.getGlobalUid(), e.getMessage());
            }
        }

        // Bank accounts
        List<BankAccountResponse> bankAccounts = List.of();
        try {
            bankAccounts = manageBankAccountsUseCase.getBankAccounts(customer.getTenantId(), id).stream()
                    .map(this::toBankAccountResponse)
                    .toList();
        } catch (Exception e) {
            log.warn("Bank accounts retrieval failed for customerId={}: {}", id, e.getMessage());
        }

        // Employment info
        List<EmploymentInfoResponse> employments = List.of();
        try {
            employments = employmentInfoRepository.findAllByCustomerId(id).stream()
                    .map(this::toEmploymentResponse)
                    .toList();
        } catch (Exception e) {
            log.warn("Employment retrieval failed for customerId={}: {}", id, e.getMessage());
        }

        // Wallet IBAN
        String iban = null;
        try {
            iban = walletPort.getIbanByCustomerId(customer.getTenantId(), id).orElse(null);
        } catch (Exception e) {
            log.warn("Wallet IBAN retrieval failed for customerId={}: {}", id, e.getMessage());
        }

        return ResponseEntity.ok(new CustomerDetailResponse(
                toResponse(customer),
                piiData,
                bankAccounts,
                employments,
                iban
        ));
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/{id}/360")
    @Operation(summary = "Get Customer 360 view", description = "Returns comprehensive customer data aggregated from PII Vault, KYC Adapter, Risk Service, Lending Service, Onboarding Workflow, and Wallet Service")
    @ApiResponse(responseCode = "200", description = "Customer 360 view retrieved")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Customer360Response> getCustomer360(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        boolean superAdmin = isSuperAdmin(jwt);
        UUID tenantId = superAdmin ? null : extractTenantId(jwt);
        log.info("Getting Customer 360 view for ID: {} superAdmin: {}", id, superAdmin);

        Customer360Response response = getCustomer360Service.get360View(
                id, tenantId, superAdmin, jwt.getTokenValue());
        return ResponseEntity.ok(response);
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/{id}/profile")
    @Operation(summary = "Get customer profile summary", description = "Returns lightweight profile: firstName, lastName, email, and primary IBAN")
    @ApiResponse(responseCode = "200", description = "Profile found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Getting customer profile for ID: {} tenant: {}", id, tenantId);

        Customer customer = getCustomerUseCase.getById(id);
        String iban = walletPort.getIbanByCustomerId(customer.getTenantId(), id).orElse(null);

        String profilePictureUrl = customer.getProfilePicture() != null
                ? "/api/v1/customers/" + customer.getId() + "/profile-picture" : null;
        return ResponseEntity.ok(new CustomerProfileResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getNationalId(),
                customer.getMobileNumber(),
                customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null,
                iban,
                profilePictureUrl
        ));
    }

    @SecuredEndpoint(obj = "profiles.me", act = "read")
    @GetMapping("/my-profile")
    @Operation(summary = "Get my profile (mobile app)", description = "Returns the authenticated customer's own profile summary")
    @ApiResponse(responseCode = "200", description = "Profile found")
    public ResponseEntity<CustomerProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        Customer self = getCustomerUseCase.getByKeycloakUserId(keycloakUserId);

        String iban = null;
        try {
            iban = walletPort.getIbanByCustomerId(self.getTenantId(), self.getId()).orElse(null);
        } catch (Exception e) {
            log.warn("Wallet IBAN lookup failed for customerId={}: {}", self.getId(), e.getMessage());
        }

        String profilePictureUrl = self.getProfilePicture() != null
                ? "/api/v1/customers/" + self.getId() + "/profile-picture" : null;

        return ResponseEntity.ok(new CustomerProfileResponse(
                self.getId(),
                self.getFirstName(),
                self.getLastName(),
                self.getEmail(),
                self.getNationalId(),
                self.getMobileNumber(),
                self.getDateOfBirth() != null ? self.getDateOfBirth().toString() : null,
                iban,
                profilePictureUrl
        ));
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/{id}/profile-picture")
    @Operation(summary = "Get customer profile picture", description = "Returns the raw image bytes for a customer's profile picture")
    @ApiResponse(responseCode = "200", description = "Image returned")
    @ApiResponse(responseCode = "404", description = "Customer or picture not found")
    public ResponseEntity<byte[]> getProfilePicture(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        Customer customer = getCustomerUseCase.getById(id);
        String raw = customer.getProfilePicture();
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // Parse "data:{contentType};base64,{data}"
        String contentType = "image/jpeg";
        String base64Data = raw;
        if (raw.startsWith("data:")) {
            int semicolon = raw.indexOf(';');
            int comma = raw.indexOf(',');
            if (semicolon > 5 && comma > semicolon) {
                contentType = raw.substring(5, semicolon);
                base64Data = raw.substring(comma + 1);
            }
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .contentLength(imageBytes.length)
                .body(imageBytes);
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/by-cif/{cifNumber}")
    @Operation(summary = "Get customer by CIF number", description = "Retrieves customer details by CIF number")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> getCustomerByCifNumber(
            @PathVariable String cifNumber,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Getting customer by CIF: {} for tenant: {}", cifNumber, tenantId);

        Customer customer = getCustomerUseCase.getByCifNumber(tenantId, cifNumber);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping("/by-nid/{nationalId}")
    @Operation(summary = "Get customer by National ID", description = "Retrieves customer details by Saudi National ID")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> getCustomerByNationalId(
            @PathVariable String nationalId,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Getting customer by NID: ***{}", nationalId.substring(nationalId.length() - 4));

        Customer customer = getCustomerUseCase.getByNationalId(nationalId);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "customers", act = "read")
    @GetMapping
    @Operation(summary = "List all customers", description = "Retrieves customers. super_admin sees all tenants, other roles see only their tenant. Optionally filter by lifecycleStage or kycStatus.")
    @ApiResponse(responseCode = "200", description = "Customers retrieved")
    public ResponseEntity<List<CustomerResponse>> listCustomers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String lifecycleStage,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String kycStatus,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isSuperAdmin = isSuperAdmin(jwt);
        UUID tenantId = extractTenantId(jwt);
        log.info("Listing customers - superAdmin: {}, tenant: {}, lifecycleStage: {}, kycStatus: {}",
                isSuperAdmin, tenantId, lifecycleStage, kycStatus);

        List<Customer> customers;
        if (lifecycleStage != null && !lifecycleStage.isBlank()) {
            LifecycleStage stage = LifecycleStage.valueOf(lifecycleStage.toUpperCase());
            customers = getCustomerUseCase.getByLifecycleStage(tenantId, stage);
        } else if (kycStatus != null && !kycStatus.isBlank()) {
            KycStatus status = KycStatus.valueOf(kycStatus.toUpperCase());
            customers = getCustomerUseCase.getByKycStatus(tenantId, status);
        } else if (isSuperAdmin) {
            customers = getCustomerUseCase.getAll();
        } else {
            customers = getCustomerUseCase.getAllByTenant(tenantId);
        }

        List<CustomerResponse> responses = customers.stream()
                .sorted(java.util.Comparator.comparing(Customer::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "profiles.me", act = "update")
    @PatchMapping("/my-profile")
    @Operation(summary = "Update my profile (mobile app)",
            description = "Allows the authenticated customer to update their email and/or profile picture. "
                    + "Send as multipart/form-data: 'email' (text, optional) and 'profilePicture' (file, optional). "
                    + "Both fields are optional — send only what you want to update. "
                    + "Name, mobile, and address changes must be done via CSA.")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerProfileResponse> updateMyProfile(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) MultipartFile profilePicture,
            @AuthenticationPrincipal Jwt jwt) {

        // Resolve customer by keycloak user ID (JWT sub) — avoids tenant mismatch
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        Customer self = getCustomerUseCase.getByKeycloakUserId(keycloakUserId);
        UUID customerId = self.getId();
        UUID tenantId   = self.getTenantId();

        log.info("Updating own profile for customerId={} tenant={} hasEmail={} hasPicture={}",
                customerId, tenantId, email != null, profilePicture != null && !profilePicture.isEmpty());

        // Convert uploaded file → Base64 data URL for storage (only when file is present)
        String profilePictureData = null;
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                byte[] bytes = profilePicture.getBytes();
                String contentType = profilePicture.getContentType() != null
                        ? profilePicture.getContentType() : "image/jpeg";
                profilePictureData = "data:" + contentType + ";base64,"
                        + Base64.getEncoder().encodeToString(bytes);
                log.info("Profile picture uploaded: {} bytes, type={}", bytes.length, contentType);
            } catch (IOException e) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST, "Failed to process profile picture: " + e.getMessage());
            }
        }

        UpdateCustomerUseCase.UpdateCustomerCommand command = new UpdateCustomerUseCase.UpdateCustomerCommand(
                null, null, null, null,  // name fields — not updatable by customer
                email,
                null,  // mobileNumber — requires OTP verification
                null, null, null, null, null,  // address fields — not updatable by customer
                profilePictureData
        );

        Customer customer = updateCustomerUseCase.update(tenantId, customerId, command);

        String iban = null;
        try {
            iban = walletPort.getIbanByCustomerId(customer.getTenantId(), customer.getId()).orElse(null);
        } catch (Exception e) {
            log.warn("Wallet IBAN lookup failed for customerId={}: {}", customer.getId(), e.getMessage());
        }
        String profilePictureUrl = customer.getProfilePicture() != null
                ? "/api/v1/customers/" + customer.getId() + "/profile-picture" : null;

        return ResponseEntity.ok(new CustomerProfileResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getNationalId(),
                customer.getMobileNumber(),
                customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null,
                iban,
                profilePictureUrl
        ));
    }

    @SecuredEndpoint(obj = "customers", act = "update")
    @PatchMapping("/{id}")
    @Operation(summary = "Update customer details", description = "Updates mutable customer fields (contact info, address)")
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating customer: {} for tenant: {}", id, tenantId);

        UpdateCustomerUseCase.UpdateCustomerCommand command = new UpdateCustomerUseCase.UpdateCustomerCommand(
                request.firstName(),
                request.lastName(),
                request.firstNameAr(),
                request.lastNameAr(),
                request.email(),
                request.mobileNumber(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.region(),
                request.postalCode(),
                request.profilePicture()
        );

        Customer customer = updateCustomerUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "customers.kyc-status", act = "update")
    @PatchMapping("/{id}/kyc-status")
    @Operation(summary = "Update customer KYC status", description = "Updates the KYC verification status of a customer")
    @ApiResponse(responseCode = "200", description = "KYC status updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> updateKycStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKycStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Updating KYC status for customer: {} to {}", id, request.kycStatus());

        KycStatus kycStatus = KycStatus.valueOf(request.kycStatus());
        Customer customer = updateCustomerUseCase.updateKycStatus(tenantId, id, kycStatus);
        return ResponseEntity.ok(toResponse(customer));
    }

    @SecuredEndpoint(obj = "customers.bank-accounts", act = "create")
    @PostMapping("/{id}/bank-accounts")
    @Operation(summary = "Add bank account", description = "Adds a bank account to a customer")
    @ApiResponse(responseCode = "201", description = "Bank account added successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<BankAccountResponse> addBankAccount(
            @PathVariable UUID id,
            @Valid @RequestBody AddBankAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Adding bank account for customer: {}", id);

        ManageBankAccountsUseCase.AddBankAccountCommand command = new ManageBankAccountsUseCase.AddBankAccountCommand(
                request.bankName(),
                request.bankCode(),
                request.iban(),
                request.accountHolderName(),
                request.accountType(),
                request.isPrimary() != null ? request.isPrimary() : true,
                request.isSalaryAccount() != null ? request.isSalaryAccount() : false
        );

        BankAccount bankAccount = manageBankAccountsUseCase.addBankAccount(tenantId, id, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toBankAccountResponse(bankAccount));
    }

    @SecuredEndpoint(obj = "customers.bank-accounts", act = "read")
    @GetMapping("/{id}/bank-accounts")
    @Operation(summary = "List bank accounts", description = "Lists all bank accounts for a customer")
    @ApiResponse(responseCode = "200", description = "Bank accounts retrieved")
    public ResponseEntity<List<BankAccountResponse>> listBankAccounts(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Listing bank accounts for customer: {}", id);

        List<BankAccount> accounts = manageBankAccountsUseCase.getBankAccounts(tenantId, id);
        List<BankAccountResponse> responses = accounts.stream()
                .map(this::toBankAccountResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // TODO: Refactor to use AddEmploymentUseCase instead of direct repository access (hexagonal violation)
    @SecuredEndpoint(obj = "customers.employment", act = "create")
    @PostMapping("/{id}/employment")
    @Operation(summary = "Add employment info", description = "Adds employment information for a customer")
    @ApiResponse(responseCode = "201", description = "Employment info added successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<EmploymentInfoResponse> addEmployment(
            @PathVariable UUID id,
            @Valid @RequestBody AddEmploymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Adding employment info for customer: {}", id);

        EmploymentInfo employment = new EmploymentInfo();
        employment.setTenantId(tenantId);
        employment.setCustomerId(id);
        employment.setEmployerName(request.employerName());
        employment.setEmployerCrNumber(request.employerCrNumber());
        employment.setEmployerSector(request.employerSector());
        employment.setEmploymentType(parseEmploymentType(request.employmentType()));
        employment.setJobTitle(request.jobTitle());
        employment.setStartDate(request.startDate());
        employment.setBasicSalary(request.basicSalary());
        employment.setHousingAllowance(request.housingAllowance());
        employment.setOtherAllowances(request.otherAllowances());
        employment.setGrossSalary(request.grossSalary());
        employment.setDeductions(request.deductions());
        employment.setNetSalary(request.netSalary() != null ? request.netSalary() : BigDecimal.ZERO);
        employment.setCurrency("SAR");
        employment.setSalaryBankName(request.salaryBankName());
        employment.setSalaryIban(request.salaryIban());
        employment.setCurrent(true);
        java.time.Instant now = java.time.Instant.now();
        employment.setCreatedAt(now);
        employment.setUpdatedAt(now);

        EmploymentInfo saved = employmentInfoRepository.save(employment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toEmploymentResponse(saved));
    }

    // ---- Helper methods ----

    private static final java.util.Map<String, EmploymentType> EMPLOYMENT_TYPE_ALIASES = java.util.Map.of(
        "FULL_TIME", EmploymentType.PRIVATE,
        "PART_TIME", EmploymentType.PRIVATE,
        "SALARIED", EmploymentType.PRIVATE,
        "MILITARY", EmploymentType.GOVERNMENT,
        "FREELANCE", EmploymentType.SELF_EMPLOYED
    );

    private EmploymentType parseEmploymentType(String value) {
        if (value == null || value.isBlank()) {
            return EmploymentType.PRIVATE;
        }
        try {
            return EmploymentType.valueOf(value);
        } catch (IllegalArgumentException e) {
            EmploymentType mapped = EMPLOYMENT_TYPE_ALIASES.get(value);
            if (mapped != null) {
                log.info("Mapped employment type '{}' -> {}", value, mapped);
                return mapped;
            }
            log.warn("Unknown employment type '{}', defaulting to PRIVATE", value);
            return EmploymentType.PRIVATE;
        }
    }

    private UUID parseTenantIdOrDefault(String tenantIdStr, Jwt jwt) {
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            try {
                return UUID.fromString(tenantIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Non-UUID tenantId '{}', falling back to JWT claim", tenantIdStr);
            }
        }
        return extractTenantId(jwt);
    }

    @SuppressWarnings("unchecked")
    private boolean isSuperAdmin(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        var roles = (java.util.Collection<String>) realmAccess.get("roles");
        return roles != null && roles.contains("super_admin");
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private CustomerResponse toResponse(Customer customer) {
        String profilePictureUrl = customer.getProfilePicture() != null
                ? "/api/v1/customers/" + customer.getId() + "/profile-picture"
                : null;
        return new CustomerResponse(
                customer.getId(),
                customer.getCifNumber(),
                customer.getCustomerType() != null ? customer.getCustomerType().name() : null,
                customer.getNationalId(),
                customer.getNationalIdType(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFirstNameAr(),
                customer.getLastNameAr(),
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getGender() != null ? customer.getGender().name() : null,
                customer.getNationality(),
                customer.getResidencyType() != null ? customer.getResidencyType().name() : null,
                customer.getMobileNumber(),
                customer.getEmail(),
                customer.getKycStatus() != null ? customer.getKycStatus().name() : null,
                customer.getLifecycleStage() != null ? customer.getLifecycleStage().name() : null,
                customer.getRiskGrade() != null ? customer.getRiskGrade().name() : null,
                customer.isPepFlag(),
                customer.isSanctionsFlag(),
                customer.getGlobalUid(),
                profilePictureUrl,
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private BankAccountResponse toBankAccountResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getBankCode(),
                account.getIban(),
                account.getAccountHolderName(),
                account.getAccountType(),
                account.isPrimary(),
                account.isSalaryAccount(),
                account.getStatus() != null ? account.getStatus().name() : null,
                account.getVerifiedAt(),
                account.getCreatedAt()
        );
    }

    private EmploymentInfoResponse toEmploymentResponse(EmploymentInfo info) {
        return new EmploymentInfoResponse(
                info.getId(),
                info.getEmployerName(),
                info.getEmployerCrNumber(),
                info.getEmployerSector(),
                info.getEmploymentType() != null ? info.getEmploymentType().name() : null,
                info.getJobTitle(),
                info.getNetSalary(),
                info.getCurrency(),
                info.isVerified(),
                info.isCurrent(),
                info.getCreatedAt()
        );
    }

    // ---- Inner DTOs for endpoints without dedicated DTO files ----

    public record UpdateKycStatusRequest(
            @jakarta.validation.constraints.NotBlank String kycStatus
    ) {}

    public record BankAccountResponse(
            UUID id,
            String bankName,
            String bankCode,
            String iban,
            String accountHolderName,
            String accountType,
            boolean isPrimary,
            boolean isSalaryAccount,
            String status,
            java.time.Instant verifiedAt,
            java.time.Instant createdAt
    ) {}

    public record CustomerProfileResponse(
            UUID customerId,
            String firstName,
            String lastName,
            String email,
            String nationalId,
            String mobileNumber,
            String dateOfBirth,
            String iban,
            String profilePicture
    ) {}

    public record EmploymentInfoResponse(
            UUID id,
            String employerName,
            String employerCrNumber,
            String employerSector,
            String employmentType,
            String jobTitle,
            BigDecimal netSalary,
            String currency,
            boolean verified,
            boolean isCurrent,
            java.time.Instant createdAt
    ) {}

    public record CustomerDetailResponse(
            CustomerResponse customer,
            java.util.Map<String, String> piiVault,
            List<BankAccountResponse> bankAccounts,
            List<EmploymentInfoResponse> employments,
            String walletIban
    ) {}
}
