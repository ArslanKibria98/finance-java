package com.ksa.financing.middleware.adapter.mock.provider;

import com.ksa.financing.middleware.adapter.mock.MockResponseResult;

public interface MockResponseProvider {

    String getProviderCode();

    MockResponseResult getMockResponse(String apiCode, String requestBody);
}
