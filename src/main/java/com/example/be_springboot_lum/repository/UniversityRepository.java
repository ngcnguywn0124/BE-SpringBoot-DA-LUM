package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniversityRepository extends JpaRepository<University, UUID> {

    boolean existsByUniversityName(String universityName);

    Optional<University> findBySlug(String slug);

    List<University> findByCityContainingIgnoreCase(String city);

    List<University> findByUniversityNameContainingIgnoreCaseOrShortNameContainingIgnoreCase(
            String name, String shortName);
}
