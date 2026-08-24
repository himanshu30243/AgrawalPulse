package com.agrawalpulse.matrimony.dto;

import com.agrawalpulse.common.model.Gender;

// All fields optional (null = unfiltered) - mirrors the query params on GET /matrimony/eligible
// in docs/api-specifications.md.
public record EligibleSearchCriteria(
        String district,
        String education,
        String profession,
        Gender gender
) {
}
