package com.ksa.financing.piivault.application.usecase;

import com.ksa.financing.piivault.domain.port.in.DeletePiiUseCase;
import com.ksa.financing.piivault.domain.port.out.PiiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeletePiiService implements DeletePiiUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeletePiiService.class);

    private final PiiRepository piiRepository;

    public DeletePiiService(PiiRepository piiRepository) {
        this.piiRepository = piiRepository;
    }

    @Override
    @Transactional
    public void delete(UUID globalUid, UUID deletedBy, String reason) {
        log.info("Deleting PII for globalUid={} by={} reason={}", globalUid, deletedBy, reason);
        piiRepository.deleteByGlobalUid(globalUid);
    }
}
