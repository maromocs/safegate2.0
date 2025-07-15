package com.SafeGate.service;

import com.SafeGate.model.SignatureRule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SignatureRulesEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SignatureRulesEngine.class);
    private List<SignatureRule> rules = new ArrayList<>();
    
    @PostConstruct
    public void initializeRules() {
        logger.info("Initializing signature rules...");
        
        rules.add(new SignatureRule("XSS-001", "XSS", 
            "(?i)<script[^>]*>.*?</script>|<script[^>]*>|javascript:|on\\w+\\s*=", 
            "Detects XSS patterns"));
        
        // Replace the old SQLi rule with this one
        rules.add(new SignatureRule("SQLI-001", "SQLi", 
            "(?i)(union\\s+select|or\\s+1\\s*=\\s*1|'|%27|drop\\s+table|--|;|/\\*.*\\*/)",
        "Detects common SQL injection patterns"));
        
        rules.add(new SignatureRule("CMDI-001", "Command Injection", 
            "(?i)(;\\s*ls|;\\s*cat|\\|\\s*ls|`.*`|\\$\\(.*\\))", 
            "Detects command injection"));
        
        rules.add(new SignatureRule("TRAV-001", "Directory Traversal", 
            "(?i)(\\.\\.[\\/\\\\]|[\\/\\\\]etc[\\/\\\\]passwd)", 
            "Detects directory traversal"));
        
        logger.info("Loaded {} signature rules", rules.size());
    }
    
    public Optional<SignatureRule> getMatchingRule(String payload) {
        if (payload == null || payload.isEmpty()) {
            return Optional.empty();
        }
        
        for (SignatureRule rule : rules) {
            if (rule.getCompiledPattern().matcher(payload).find()) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }
    
    public List<SignatureRule> getAllRules() {
        return new ArrayList<>(rules);
    }
}