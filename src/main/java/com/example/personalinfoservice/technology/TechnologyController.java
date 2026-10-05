package com.example.personalinfoservice.technology;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personalinfoservice.technology.dto.TechnologyResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/technologies")
@RequiredArgsConstructor
public class TechnologyController {

    private final TechnologyService technologyService;

    /**
     * Returns the full technologies list as a plain JSON array (no
     * pagination envelope) — this is small, bounded reference data, not a
     * growing collection. See docs/api/contracts.md.
     */
    @GetMapping
    public List<TechnologyResponse> getTechnologies() {
        return technologyService.getAllTechnologies();
    }
}
