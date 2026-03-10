package com.ksa.financing.globalprofile.application.usecase;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import com.ksa.financing.globalprofile.domain.port.in.GetGlobalProfileUseCase;
import com.ksa.financing.globalprofile.domain.port.out.GlobalCustomerRepository;
import com.ksa.financing.globalprofile.domain.port.out.RegionalProfileRepository;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.exception.TechnicalException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
public class GetGlobalProfileService implements GetGlobalProfileUseCase {

    private final GlobalCustomerRepository globalCustomerRepository;
    private final RegionalProfileRepository regionalProfileRepository;

    public GetGlobalProfileService(GlobalCustomerRepository globalCustomerRepository,
                                    RegionalProfileRepository regionalProfileRepository) {
        this.globalCustomerRepository = globalCustomerRepository;
        this.regionalProfileRepository = regionalProfileRepository;
    }

    @Override
    public GlobalCustomer getByGlobalUid(UUID globalUid) {
        return globalCustomerRepository.findByGlobalUid(globalUid)
            .orElseThrow(() -> NotFoundException.forEntity("GlobalProfile", globalUid.toString()));
    }

    @Override
    public GlobalCustomer findByEmail(String email) {
        String emailHash = sha256Hash(email.toLowerCase());
        return globalCustomerRepository.findByEmailHash(emailHash)
            .orElseThrow(() -> NotFoundException.forEntity("GlobalProfile", "email"));
    }

    @Override
    public GlobalCustomer findByMobile(String mobile) {
        String mobileHash = sha256Hash(mobile);
        return globalCustomerRepository.findByMobileHash(mobileHash)
            .orElseThrow(() -> NotFoundException.forEntity("GlobalProfile", "mobile"));
    }

    @Override
    public List<RegionalProfile> getRegionalProfiles(UUID globalUid) {
        return regionalProfileRepository.findByGlobalUid(globalUid);
    }

    private String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "SHA-256 algorithm not available", e);
        }
    }
}
