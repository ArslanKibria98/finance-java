package com.ksa.financing.product.application.mapper;

import com.ksa.financing.product.adapter.rest.response.CountryResponse;
import com.ksa.financing.product.domain.model.Country;
import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

    private CountryMapper() {}

    public static CountryResponse toResponse(Country country) {
        if (country == null) return null;

        return new CountryResponse(
                country.getId(),
                country.getCode(),
                country.getNameEn(),
                country.getNameAr(),
                country.getDialCode(),
                country.getCurrencyCode(),
                country.isGcc(),
                country.isActive(),
                country.getSortOrder()
        );
    }
}
