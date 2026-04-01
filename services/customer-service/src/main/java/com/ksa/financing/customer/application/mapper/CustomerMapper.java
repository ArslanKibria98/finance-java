package com.ksa.financing.customer.application.mapper;

import com.ksa.financing.customer.application.dto.BankAccountResponse;
import com.ksa.financing.customer.application.dto.CustomerResponse;
import com.ksa.financing.customer.application.dto.EmploymentInfoResponse;
import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.EmploymentInfo;

public class CustomerMapper {

    private CustomerMapper() {}

    public static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
            c.getId(),
            c.getCifNumber(),
            c.getCustomerType() != null ? c.getCustomerType().name() : null,
            c.getNationalId(),
            c.getNationalIdType(),
            c.getFirstName(),
            c.getLastName(),
            c.getFirstNameAr(),
            c.getLastNameAr(),
            c.getFullName(),
            c.getDateOfBirth(),
            c.getGender() != null ? c.getGender().name() : null,
            c.getNationality(),
            c.getResidencyType() != null ? c.getResidencyType().name() : null,
            c.getMobileNumber(),
            c.getEmail(),
            c.getKycStatus() != null ? c.getKycStatus().name() : null,
            c.getLifecycleStage() != null ? c.getLifecycleStage().name() : null,
            c.getRiskGrade() != null ? c.getRiskGrade().name() : null,
            c.isPepFlag(),
            c.isSanctionsFlag(),
            c.getGlobalUid(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }

    public static BankAccountResponse toBankAccountResponse(BankAccount ba) {
        String maskedIban = ba.getIban() != null && ba.getIban().length() > 4
            ? "****" + ba.getIban().substring(ba.getIban().length() - 4)
            : ba.getIban();
        return new BankAccountResponse(
            ba.getId(),
            ba.getBankName(),
            ba.getBankCode(),
            maskedIban,
            ba.getAccountHolderName(),
            ba.getAccountType(),
            ba.isPrimary(),
            ba.isSalaryAccount(),
            ba.getStatus() != null ? ba.getStatus().name() : null,
            ba.getVerifiedAt(),
            ba.getCreatedAt()
        );
    }

    public static EmploymentInfoResponse toEmploymentResponse(EmploymentInfo ei) {
        return new EmploymentInfoResponse(
            ei.getId(),
            ei.getEmployerName(),
            ei.getEmploymentType() != null ? ei.getEmploymentType().name() : null,
            ei.getJobTitle(),
            ei.getStartDate(),
            ei.getNetSalary(),
            ei.getCurrency(),
            ei.isVerified(),
            ei.getVerifiedVia(),
            ei.isCurrent()
        );
    }
}
