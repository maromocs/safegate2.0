package com.SafeGate.model;

import com.SafeGate.enums.LLMMode;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "llm_config")
@Data
public class LLMConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LLMMode llmMode = LLMMode.DISABLED;

    // Analyzer URL, e.g., http://analyzer:5000/analyze
    @Column
    private String llmApiUrl;

    // Optional API key if ever needed
    @Column
    private String llmApiKey;

    // LLM provider (e.g., "ollama")
    @Column
    private String provider = "ollama";

    // Model name for the provider (e.g., tinyllama, phi, phi3:mini, mistral, llama2, llama3.2:3b-instruct)
    @Column
    private String model = "tinyllama";

    // GPU acceleration flag (nullable in DB; coerced to false on save if null)
    @Column
    private Boolean gpuEnabled = Boolean.FALSE;
}