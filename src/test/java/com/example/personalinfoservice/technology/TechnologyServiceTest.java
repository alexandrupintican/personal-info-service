package com.example.personalinfoservice.technology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personalinfoservice.technology.dto.TechnologyResponse;

@ExtendWith(MockitoExtension.class)
class TechnologyServiceTest {

    @Mock
    private TechnologyRepository technologyRepository;

    private TechnologyService technologyService;

    @BeforeEach
    void setUp() {
        technologyService = new TechnologyService(technologyRepository);
    }

    @Test
    void getAllTechnologies_mapsEntitiesToResponseDtosPreservingRepositoryOrder() {
        given(technologyRepository.findAllByOrderByCategoryAscConfidenceDesc()).willReturn(List.of(
                Technology.builder().id(1).name("Vue.js").category(TechnologyCategory.stack)
                        .confidence((short) 5).build(),
                Technology.builder().id(2).name("Docker").category(TechnologyCategory.in_progress)
                        .confidence((short) 2).build()
        ));

        List<TechnologyResponse> result = technologyService.getAllTechnologies();

        assertThat(result).containsExactly(
                new TechnologyResponse("Vue.js", "stack", 5),
                new TechnologyResponse("Docker", "in_progress", 2)
        );
    }

    @Test
    void getAllTechnologies_whenNoneExist_returnsEmptyList() {
        given(technologyRepository.findAllByOrderByCategoryAscConfidenceDesc()).willReturn(List.of());

        List<TechnologyResponse> result = technologyService.getAllTechnologies();

        assertThat(result).isEmpty();
    }
}
