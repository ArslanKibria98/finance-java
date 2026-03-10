package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.kycadapter.domain.port.in.VerifyOtpUseCase;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity.OtpStatus;
import com.ksa.financing.kycadapter.infrastructure.persistence.repository.JpaOtpVerificationRepository;
import com.ksa.financing.infra.exception.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class VerifyOtpService implements VerifyOtpUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyOtpService.class);

    private final JpaOtpVerificationRepository otpRepository;

    public VerifyOtpService(JpaOtpVerificationRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Override
    @Transactional
    public VerifyOtpResult verify(VerifyOtpCommand command) {
        log.info("OTP verification requested for otpRequestId={}", command.otpRequestId());

        Optional<OtpVerificationJpaEntity> optEntity = otpRepository.findByOtpRequestId(command.otpRequestId());

        if (optEntity.isEmpty()) {
            log.warn("OTP request not found: otpRequestId={}", command.otpRequestId());
            return new VerifyOtpResult(false, ErrorCodes.Kyc.OTP_REQUEST_NOT_FOUND);
        }

        OtpVerificationJpaEntity entity = optEntity.get();

        // If already verified, return success (idempotent)
        if (entity.getStatus() == OtpStatus.VERIFIED) {
            log.info("OTP already verified: otpRequestId={}", command.otpRequestId());
            return new VerifyOtpResult(true, null);
        }

        // Check if expired or max attempts exceeded
        if (entity.getStatus() != OtpStatus.SENT) {
            log.warn("OTP already processed: otpRequestId={}, status={}", command.otpRequestId(), entity.getStatus());
            return new VerifyOtpResult(false, ErrorCodes.Kyc.OTP_ALREADY_PROCESSED);
        }

        // Check expiry
        if (OffsetDateTime.now().isAfter(entity.getExpiresAt())) {
            entity.setStatus(OtpStatus.EXPIRED);
            entity.setFailureReason(ErrorCodes.Kyc.OTP_EXPIRED);
            otpRepository.save(entity);
            log.warn("OTP expired: otpRequestId={}", command.otpRequestId());
            return new VerifyOtpResult(false, ErrorCodes.Kyc.OTP_EXPIRED);
        }

        // Increment attempt count
        entity.setVerifyAttempts(entity.getVerifyAttempts() + 1);

        // Check max attempts
        if (entity.getVerifyAttempts() > entity.getMaxVerifyAttempts()) {
            entity.setStatus(OtpStatus.MAX_ATTEMPTS_EXCEEDED);
            entity.setFailureReason(ErrorCodes.Kyc.OTP_MAX_ATTEMPTS);
            otpRepository.save(entity);
            log.warn("Max OTP verify attempts exceeded: otpRequestId={}", command.otpRequestId());
            return new VerifyOtpResult(false, ErrorCodes.Kyc.OTP_MAX_ATTEMPTS);
        }

        // Verify OTP code
        if (entity.getOtpCode().equals(command.otpCode())) {
            entity.setStatus(OtpStatus.VERIFIED);
            entity.setVerifiedAt(OffsetDateTime.now());
            otpRepository.save(entity);
            log.info("OTP verified successfully: otpRequestId={}", command.otpRequestId());
            return new VerifyOtpResult(true, null);
        }

        // Wrong code
        otpRepository.save(entity);
        log.warn("Invalid OTP code: otpRequestId={}, attempt {}/{}",
                command.otpRequestId(), entity.getVerifyAttempts(), entity.getMaxVerifyAttempts());
        return new VerifyOtpResult(false, ErrorCodes.Kyc.OTP_INVALID);
    }
}
