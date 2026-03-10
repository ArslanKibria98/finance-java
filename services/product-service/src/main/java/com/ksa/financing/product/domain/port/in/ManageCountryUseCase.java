package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.Country;
import java.util.List;
import java.util.UUID;

public interface ManageCountryUseCase {

    List<Country> listCountries(UUID tenantId);
    List<Country> listGccCountries(UUID tenantId);
    Country getCountry(UUID tenantId, UUID id);
    Country createCountry(UUID tenantId, String code, String nameEn, String nameAr,
                          String dialCode, String currencyCode, boolean gcc, int sortOrder);
    Country updateCountry(UUID tenantId, UUID id, String nameEn, String nameAr,
                          String dialCode, String currencyCode, boolean gcc,
                          int sortOrder, boolean active);
    void deleteCountry(UUID tenantId, UUID id);
}
