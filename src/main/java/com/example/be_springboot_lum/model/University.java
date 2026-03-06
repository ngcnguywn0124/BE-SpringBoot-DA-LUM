package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "universities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "university_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID universityId;

    @Column(name = "university_name", nullable = false, unique = true, length = 255)
    private String universityName;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "slug", unique = true, length = 255)
    private String slug;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "university", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Campus> campuses = new ArrayList<>();
}
