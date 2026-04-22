package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.CustomerPepAnswer;
import com.ksa.financing.customer.domain.model.PepStatus;
import com.ksa.financing.customer.domain.port.in.SubmitPepAnswerUseCase;
import com.ksa.financing.customer.domain.port.out.CustomerPepAnswerRepository;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitPepAnswerService implements SubmitPepAnswerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerPepAnswerRepository customerPepAnswerRepository;

    @Override
    @Transactional
    public CustomerPepAnswer submit(UUID tenantId, UUID customerId, SubmitPepAnswerCommand command) {
        var customer = customerRepository.findById(tenantId, customerId)
                .orElseThrow(() -> NotFoundException.forEntity("Customer", customerId.toString()));

        var answer = new CustomerPepAnswer(
                null,
                tenantId,
                customerId,
                command.isPep(),
                command.politicalPosition(),
                command.governmentBody(),
                command.countryOfInfluence(),
                command.positionStartDate(),
                command.positionEndDate(),
                command.primarySourceOfWealth(),
                command.estimatedNetWorth(),
                command.sourceOfWealthDescription(),
                command.sourceOfFunds(),
                command.sourceOfFundsDetails(),
                command.relatedPersons(),
                command.additionalNotes(),
                CustomerPepAnswer.SubmittedVia.POST_LOGIN,
                Instant.now(),
                command.submittedBy()
        );

        var saved = customerPepAnswerRepository.save(answer);

        customer.setPepFlag(command.isPep());
        customer.setPepStatus(PepStatus.COMPLETED);
        customerRepository.save(customer);

        log.info("PEP answers submitted for customerId={} tenantId={}", customerId, tenantId);
        return saved;
    }
}
