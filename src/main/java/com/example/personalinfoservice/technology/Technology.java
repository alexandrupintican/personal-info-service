package com.example.personalinfoservice.technology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A technology/skill shown on the "about me" page, with a self-rated
 * confidence level. Maps onto the pre-existing {@code technologies} table
 * in the external {@code personaldb} database (see ADR-0005) - not a table
 * this service created for itself, so the mapping must match that real
 * schema exactly rather than an idealized one.
 */
@Entity
@Table(
        name = "technologies",
        uniqueConstraints = @UniqueConstraint(
                name = "technologies_name_category_key", columnNames = {"name", "category"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    /**
     * Native Postgres enum column ({@code technology_category}), not a
     * plain varchar - see {@link TechnologyCategory}.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "technology_category")
    private TechnologyCategory category;

    /** 1-5 star rating, enforced at the database level (see V1 migration). */
    @Column(nullable = false)
    private Short confidence;
}
