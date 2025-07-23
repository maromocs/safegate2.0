package com.SafeGate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Entity
@Table(name = "signature_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 512)
    private String pattern;

    @Column(nullable = false)
    private String description;

    @Transient
    private Pattern compiledPattern;

    public SignatureRule(String ruleId, String name, String pattern, String description) {
        this.ruleId = ruleId;
        this.name = name;
        this.pattern = pattern;
        this.description = description;
    }

    public Pattern getCompiledPattern() {
        if (compiledPattern == null) {
            compiledPattern = Pattern.compile(pattern);
        }
        return compiledPattern;
    }
}