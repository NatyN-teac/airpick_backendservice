package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "modes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
