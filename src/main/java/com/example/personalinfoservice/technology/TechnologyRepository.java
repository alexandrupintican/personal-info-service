package com.example.personalinfoservice.technology;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnologyRepository extends JpaRepository<Technology, Integer> {

    /**
     * Matches the read pattern of the endpoint this repository backs:
     * grouped by category, most-confident first within each category.
     */
    List<Technology> findAllByOrderByCategoryAscConfidenceDesc();
}
