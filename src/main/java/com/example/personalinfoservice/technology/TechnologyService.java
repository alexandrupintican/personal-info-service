package com.example.personalinfoservice.technology;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.personalinfoservice.technology.dto.TechnologyResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TechnologyService {

    private final TechnologyRepository technologyRepository;

    public List<TechnologyResponse> getAllTechnologies() {
        return technologyRepository.findAllByOrderByCategoryAscConfidenceDesc().stream()
                .map(TechnologyResponse::from)
                .toList();
    }
}
