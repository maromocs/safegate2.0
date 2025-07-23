package com.SafeGate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureRule {
    private String id;
    private String name;
    private String regexPattern;
    private Pattern compiledPattern;
    private String description;
    
    public SignatureRule(String id, String name, String regexPattern, String description) {
        this.id = id;
        this.name = name;
        this.regexPattern = regexPattern;
        this.description = description;
        this.compiledPattern = Pattern.compile(regexPattern);
    }
}