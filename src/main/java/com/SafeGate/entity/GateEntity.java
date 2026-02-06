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

    // Manual getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}