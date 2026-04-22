package com.ksa.financing.collections.adapter.rest.response;

import com.ksa.financing.collections.domain.model.DunningStage;

import java.math.BigDecimal;

public record DelinquencySimulationResponse(
        String productCode,
        int dpd,
        DunningStage stage,
        String policySource,
        boolean reportToSimah,
        boolean simahDefaultFlag,
        boolean assignAgent,
        boolean freezeWallet,
        BigDecimal lateFeeOnOutstanding,
        String charityFundAccount
) {}
