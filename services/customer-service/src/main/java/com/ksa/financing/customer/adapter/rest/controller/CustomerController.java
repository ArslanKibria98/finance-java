package com.ksa.financing.customer.adapter.rest.controller;

import com.ksa.financing.customer.application.dto.AddBankAccountRequest;
import com.ksa.financing.customer.application.dto.AddEmploymentRequest;
import com.ksa.financing.customer.application.dto.CreateCustomerRequest;
import com.ksa.financing.customer.application.dto.CustomerResponse;
import com.ksa.financing.customer.application.dto.UpdateCustomerRequest;
import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.EmploymentInfo;
import com.ksa.financing.customer.domain.model.EmploymentType;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.port.in.CreateCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.port.in.UpdateCustomerUseCase;
import com.ksa.financing.customer.domain.port.out.EmploymentInfoRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

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

        UUID tenantId = extractTenantId(jwt);
        log.info("Getting customer by ID: {} for tenant: {}", id, tenantId);

        Customer customer = getCustomerUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(customer));
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
                request.email(),
                request.mobileNumber(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.region(),
                request.postalCode()
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
        return new CustomerResponse(
                customer.getId(),
                customer.getCifNumber(),
                customer.getCustomerType() != null ? customer.getCustomerType().name() : null,
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
}
