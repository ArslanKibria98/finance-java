package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.kycadapter.domain.port.in.ResendOtpUseCase;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity.OtpStatus;
import com.ksa.financing.kycadapter.infrastructure.persistence.repository.JpaOtpVerificationRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.UnifonicStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ResendOtpService implements ResendOtpUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendOtpService.class);
    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes

    private final JpaOtpVerificationRepository otpRepository;
    private final UnifonicStubAdapter unifonicStubAdapter;

    public ResendOtpService(JpaOtpVerificationRepository otpRepository,
                            UnifonicStubAdapter unifonicStubAdapter) {
        this.otpRepository = otpRepository;
        this.unifonicStubAdapter = unifonicStubAdapter;
    }

    @Override
    @Transactional
    public ResendOtpResult resend(ResendOtpCommand command) {
        log.info("OTP resend requested for otpRequestId={}", command.otpRequestId());

        Optional<OtpVerificationJpaEntity> optEntity = otpRepository.findByOtpRequestId(command.otpRequestId());

        if (optEntity.isEmpty()) {
            log.warn("OTP request not found for resend: otpRequestId={}", command.otpRequestId());
            return new ResendOtpResult(command.otpRequestId(), false, 0);
        }

        OtpVerificationJpaEntity entity = optEntity.get();

        // Check resend limit
        if (entity.getResendCount() >= entity.getMaxResendCount()) {
            log.warn("Max resend attempts exceeded: otpRequestId={}", command.otpRequestId());
            return new ResendOtpResult(command.otpRequestId(), false, 0);
        }

        // Generate new OTP code and reset state
        String newOtpCode = generateOtp();
        entity.setOtpCode(newOtpCode);
        entity.setResendCount(entity.getResendCount() + 1);
        entity.setVerifyAttempts(0);
        entity.setStatus(OtpStatus.SENT);
        entity.setFailureReason(null);
        entity.setExpiresAt(OffsetDateTime.now().plusSeconds(OTP_EXPIRY_SECONDS));
        otpRepository.save(entity);

        // Send new OTP via SMS provider
        boolean sent = unifonicStubAdapter.sendSms(command.mobileNumber(), newOtpCode);

        if (!sent) {
            entity.setStatus(OtpStatus.FAILED);
            entity.setFailureReason("SMS_DELIVERY_FAILED");
            otpRepository.save(entity);
        }

        int remainingAttempts = entity.getMaxResendCount() - entity.getResendCount();
        log.info("OTP resend completed: otpRequestId={}, sent={}, remaining={}",
                command.otpRequestId(), sent, remainingAttempts);
        return new ResendOtpResult(command.otpRequestId(), sent, remainingAttempts);
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
