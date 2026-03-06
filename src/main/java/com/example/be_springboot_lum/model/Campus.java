package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "campuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campus_id")
    private Integer campusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "campus_name", nullable = false, length = 255)
    private String campusName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
