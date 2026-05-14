package com.ksa.financing.ledger.application.service;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.ledger.application.dto.SimahReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SimahReportService {

    public SimahReportResponse generate(UUID tenantId, String period, PageQuery pageQuery) {
        List<SimahReportResponse.SimahReportItem> seeded = new ArrayList<>();

        if (period == null || period.isBlank()) {
            for (int i = 1; i <= 3; i++) {
                seeded.add(buildItem(tenantId, i));
            }
        } else {
            seeded.add(buildItem(tenantId, 1));
        }

        String searchTerm = lowerSearch(pageQuery.search());
        List<SimahReportResponse.SimahReportItem> all = new ArrayList<>();
        for (var it : seeded) {
            if (matchesSearch(searchTerm,
                    it.customerId() != null ? it.customerId().toString() : null,
                    it.loanId() != null ? it.loanId().toString() : null,
                    it.simahStatus(), it.facilityType(), it.paymentStatus())) {
                all.add(it);
            }
        }

        Pageable pageable = pageQuery.toPageable();
        int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), all.size());
        int to = (int) Math.min((long) from + pageable.getPageSize(), all.size());
        List<SimahReportResponse.SimahReportItem> pageContent = all.subList(from, to);
        Page<SimahReportResponse.SimahReportItem> page = new PageImpl<>(pageContent, pageable, all.size());

        return SimahReportResponse.builder()
                .period(period)
                .generatedDate(LocalDate.now())
                .items(pageContent)
                .pagination(PageMetadata.from(page))
                .build();
    }

    private SimahReportResponse.SimahReportItem buildItem(UUID tenantId, int seed) {
        return SimahReportResponse.SimahReportItem.builder()
                .customerId(UUID.nameUUIDFromBytes((tenantId + "-simah-customer-" + seed).getBytes(StandardCharsets.UTF_8)))
                .loanId(UUID.nameUUIDFromBytes((tenantId + "-simah-loan-" + seed).getBytes(StandardCharsets.UTF_8)))
                .simahStatus("REPORTED")
                .facilityType("MURABAHA")
                .paymentStatus("CURRENT")
                .build();
    }

    private String lowerSearch(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().toLowerCase();
    }

    private boolean matchesSearch(String term, String... fields) {
        if (term == null) return true;
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(term)) return true;
        }
        return false;
    }
}
