// src/main/java/com/SafeGate/entity/Gate.java
package com.SafeGate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gates")
@Data @NoArgsConstructor @AllArgsConstructor
public class GateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String location;
}