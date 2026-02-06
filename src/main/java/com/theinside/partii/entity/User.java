package com.theinside.partii.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    private String legalName;

    private String bio;

    @Column(nullable = false)
    private String generalLocation;

    @Column(nullable = false)
    private String primaryAddress;

    @Column(nullable = false)
    private String phoneNumber;

    private LocalDate dob;

    @Column(nullable = false)
    private AccountStatus accountStatus;

    private boolean isVerified = false;
    private boolean isEnabled = true;
    
    @Builder.Default
    private boolean isAdmin = false;
    
    @Builder.Default
    private boolean profileCompleted = false;

    private int totalRatings = 0;
    private int averageRating;

    private int eventsAttended = 0;
    private int eventsOrganized = 0;
    private int activeEventsCount = 0;

    private String profilePictureUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
