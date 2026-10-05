package com.example.personalinfoservice.technology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the real V1 Flyway migration against an actual Postgres container —
 * the entity maps onto Postgres-specific schema (a native
 * {@code technology_category} enum type, a {@code (name, category)} unique
 * constraint), which an in-memory database could not faithfully exercise.
 * Against this fresh/empty container, baselining does not kick in (see
 * spring.flyway.baseline-* in application.yaml), so V1 runs for real here —
 * this is what keeps it an accurate replica of the pre-existing personaldb
 * schema (see ADR-0005) rather than a guessed one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TechnologyRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private TechnologyRepository technologyRepository;

    @Test
    void findAllByOrderByCategoryAscConfidenceDesc_ordersByCategoryThenConfidenceDescending() {
        technologyRepository.saveAll(List.of(
                Technology.builder().name("Terraform").category(TechnologyCategory.in_progress)
                        .confidence((short) 1).build(),
                Technology.builder().name("Docker").category(TechnologyCategory.in_progress)
                        .confidence((short) 3).build(),
                Technology.builder().name("Vue.js").category(TechnologyCategory.stack)
                        .confidence((short) 5).build(),
                Technology.builder().name("TypeScript").category(TechnologyCategory.stack)
                        .confidence((short) 4).build()
        ));

        List<Technology> result = technologyRepository.findAllByOrderByCategoryAscConfidenceDesc();

        // Postgres orders a native enum column by the type's declared label
        // order (stack, then in_progress) - not alphabetically. Confirmed
        // directly against the real personaldb instance.
        assertThat(result).extracting(Technology::getName)
                .containsExactly("Vue.js", "TypeScript", "Docker", "Terraform");
    }

    @Test
    void save_rejectsDuplicateNameAndCategory() {
        technologyRepository.saveAndFlush(
                Technology.builder().name("Vue.js").category(TechnologyCategory.stack)
                        .confidence((short) 5).build());

        Technology duplicate = Technology.builder()
                .name("Vue.js").category(TechnologyCategory.stack).confidence((short) 3).build();

        assertThatThrownBy(() -> technologyRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
