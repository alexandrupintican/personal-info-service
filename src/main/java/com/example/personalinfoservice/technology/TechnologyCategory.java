package com.example.personalinfoservice.technology;

/**
 * Mirrors the native Postgres enum type {@code technology_category} in the
 * external {@code personaldb} database (see ADR-0005). Confirmed values, in
 * declared order: {@code stack}, {@code in_progress}.
 *
 * <p>Constant names intentionally match the Postgres enum labels exactly
 * (lowercase, not the usual {@code UPPER_SNAKE_CASE} Java convention).
 * Hibernate's {@code @Enumerated(STRING)} + {@code @JdbcTypeCode(NAMED_ENUM)}
 * mapping (see {@link Technology#getCategory()}) binds using {@link #name()}
 * as the wire value sent to the native enum column - a mismatched case would
 * fail to bind rather than coerce.
 */
public enum TechnologyCategory {
    stack,
    in_progress
}
