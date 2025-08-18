package com.SafeGate.controller;

import com.SafeGate.model.LLMConfig;
import com.SafeGate.service.LLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
public class LLMController {

    @Autowired
    private LLMService llmService;

    @GetMapping("/config")
    public ResponseEntity<LLMConfig> getConfig() {
        return llmService.getConfig()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new LLMConfig()));
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveConfig(@RequestBody LLMConfig config) {
        try {
            // Validate analyzer URL
            String url = config.getLlmApiUrl();
            if (url == null || url.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Analyzer URL is required and must be a valid URL."));
            }
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Analyzer URL is required and must be a valid URL."));
            }
            // Validate model when provider is ollama
            String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "";
            if ("ollama".equals(provider)) {
                String model = config.getModel();
                if (model == null || model.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "message",
                            "Model is required when provider is ‘ollama’. Please select a model from the dropdown."
                    ));
                }
            }
            // Default gpuEnabled if null to avoid null persistence
            if (config.getGpuEnabled() == null) { config.setGpuEnabled(Boolean.FALSE); }
            // Save configuration
            LLMConfig saved = llmService.saveConfig(config);
            // Best-effort pull after save if provider is ollama (do not fail save on errors)
            try {
                if ("ollama".equals(provider)) {
                    String model = config.getModel();
                    if (model != null && !model.isBlank()) {
                        llmService.pullModel(model);
                    }
                }
            } catch (Exception ignored) {}
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            // Defensive catch to avoid 500s from bubbling up
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to save configuration: " + e.getMessage()));
        }
    }

    @GetMapping("/models")
    public ResponseEntity<?> getModels() {
        return ResponseEntity.ok(llmService.getModels());
    }

    @PostMapping("/models/pull")
    public ResponseEntity<?> pullModel(@RequestBody java.util.Map<String, String> req) {
        String model = req.get("model");
        if (model == null || model.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("status", "error", "message", "model is required"));
        }
        return ResponseEntity.ok(llmService.pullModel(model));
    }

    @GetMapping("/models/progress")
    public ResponseEntity<?> modelProgress(@RequestParam("model") String model) {
        if (model == null || model.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("status", "unknown", "message", "model is required"));
        }
        return ResponseEntity.ok(llmService.getModelPullProgress(model));
    }
}