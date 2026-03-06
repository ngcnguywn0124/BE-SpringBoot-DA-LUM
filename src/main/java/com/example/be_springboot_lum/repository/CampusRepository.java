package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampusRepository extends JpaRepository<Campus, UUID> {

    List<Campus> findByUniversity_UniversityId(UUID universityId);

    Optional<Campus> findBySlug(String slug);

    boolean existsByCampusNameAndUniversity_UniversityId(String campusName, UUID universityId);
}
