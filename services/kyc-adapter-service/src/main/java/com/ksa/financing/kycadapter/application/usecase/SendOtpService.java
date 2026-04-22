package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.kycadapter.domain.port.in.SendOtpUseCase;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity.OtpStatus;
import com.ksa.financing.kycadapter.infrastructure.persistence.repository.JpaOtpVerificationRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.UnifonicStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SendOtpService implements SendOtpUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendOtpService.class);
    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes

    private final JpaOtpVerificationRepository otpRepository;
    private final UnifonicStubAdapter unifonicStubAdapter;

    public SendOtpService(JpaOtpVerificationRepository otpRepository,
                          UnifonicStubAdapter unifonicStubAdapter) {
        this.otpRepository = otpRepository;
        this.unifonicStubAdapter = unifonicStubAdapter;
    }

    @Override
    @Transactional
    public SendOtpResult send(SendOtpCommand command) {
        String maskedNationalId = maskLast4(command.nationalId());
        String maskedMobile = maskLast4(command.mobileNumber());
        log.info("OTP send requested for nationalId={}, mobile={}", maskedNationalId, maskedMobile);

        String otpCode = generateOtp();
        String otpRequestId = UUID.randomUUID().toString();

        // Persist OTP to database
        OtpVerificationJpaEntity entity = new OtpVerificationJpaEntity();
        entity.setOtpRequestId(otpRequestId);
        entity.setNationalId(command.nationalId());
        entity.setMobileNumber(command.mobileNumber());
        entity.setOtpCode(otpCode);
        entity.setStatus(OtpStatus.SENT);
        entity.setExpiresAt(OffsetDateTime.now().plusSeconds(OTP_EXPIRY_SECONDS));
        otpRepository.save(entity);

        // Send OTP via SMS provider (stub in dev)
        boolean sent = unifonicStubAdapter.sendSms(command.mobileNumber(), otpCode);

        if (!sent) {
            entity.setStatus(OtpStatus.FAILED);
            entity.setFailureReason("SMS_DELIVERY_FAILED");
            otpRepository.save(entity);
            log.warn("OTP SMS delivery failed for nationalId={}", maskedNationalId);
            return new SendOtpResult(otpRequestId, false, maskedMobile);
        }

        log.info("OTP sent successfully: otpRequestId={}", otpRequestId);
        return new SendOtpResult(otpRequestId, true, maskedMobile);
    }

    private String generateOtp() {
        // TODO: Temporary static OTP for development/testing — revert to random before production
        return "123456";
    }

    private String maskLast4(String value) {
        if (value == null || value.length() < 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
}
