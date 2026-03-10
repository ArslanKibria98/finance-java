package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to submit a loan application for processing")
public record SubmitLoanApplicationRequest() {}
