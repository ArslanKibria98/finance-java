package com.ksa.financing.kycadapter.infrastructure.persistence.repository;

import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.OtpVerificationJpaEntity.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaOtpVerificationRepository extends JpaRepository<OtpVerificationJpaEntity, UUID> {

    Optional<OtpVerificationJpaEntity> findByOtpRequestId(String otpRequestId);

    Optional<OtpVerificationJpaEntity> findByOtpRequestIdAndStatus(String otpRequestId, OtpStatus status);
}
