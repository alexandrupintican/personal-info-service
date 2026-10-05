package com.example.personalinfoservice.technology.dto;

import com.example.personalinfoservice.technology.Technology;
import com.example.personalinfoservice.technology.TechnologyCategory;

/**
 * API-facing shape for a technology. Deliberately excludes the entity's
 * surrogate {@code id} — nothing on the frontend addresses a single
 * technology by id today, so it isn't part of the contract until something
 * needs it (see docs/api/contracts.md).
 *
 * <p>{@code category} is serialized as its raw enum value ({@code "stack"}
 * / {@code "in_progress"}) — the frontend's {@code Technology} TS model
 * types this field as a plain {@code string}, matching what the Hono
 * placeholder returned (node-postgres passes native enum columns through as
 * their text label), so no additional mapping is needed here beyond
 * {@link TechnologyCategory#name()}.
 */
public record TechnologyResponse(String name, String category, Integer confidence) {

    public static TechnologyResponse from(Technology technology) {
        return new TechnologyResponse(
                technology.getName(),
                technology.getCategory() == null ? null : technology.getCategory().name(),
                technology.getConfidence() == null ? null : technology.getConfidence().intValue()
        );
    }
}
