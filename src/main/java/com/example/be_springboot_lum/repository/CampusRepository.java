package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Integer> {

    List<Campus> findByUniversity_UniversityId(Integer universityId);

    boolean existsByCampusNameAndUniversity_UniversityId(String campusName, Integer universityId);
}
