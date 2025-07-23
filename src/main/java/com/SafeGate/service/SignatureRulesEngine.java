package com.SafeGate.service;

import com.SafeGate.model.SignatureRule;
import com.SafeGate.repository.SignatureRuleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SignatureRulesEngine {

    private static final Logger logger = LoggerFactory.getLogger(SignatureRulesEngine.class);

    @Autowired
    private SignatureRuleRepository ruleRepository;

    private List<SignatureRule> rules = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    @Transactional
    public void initializeRules() {
        logger.info("Initializing signature rules...");
        if (ruleRepository.count() == 0) {
            logger.info("No rules found in the database, populating with default rules.");
            populateDefaultRules();
        }
        loadRulesFromDatabase();
    }

    private void populateDefaultRules() {
        List<SignatureRule> defaultRules = new ArrayList<>();
        defaultRules.add(new SignatureRule("XSS-001", "XSS", "(?i)<script[^>]*>.*?</script>|<script[^>]*>|javascript:|on\\w+\\s*=", "Detects XSS patterns"));
        defaultRules.add(new SignatureRule("SQLI-001", "SQLi", "(?i)(union\\s+select|or\\s+1\\s*=\\s*1|'|%27|drop\\s+table|--|;|/\\*.*\\*/)", "Detects common SQL injection patterns"));
        defaultRules.add(new SignatureRule("CMDI-001", "Command Injection", "(?i)(;\\s*ls|;\\s*cat|\\|\\s*ls|`.*`|\\$\\(.*\\))", "Detects command injection"));
        defaultRules.add(new SignatureRule("TRAV-001", "Directory Traversal", "(?i)(\\.\\.[\\/\\\\]|[\\/\\\\]etc[\\/\\\\]passwd)", "Detects directory traversal"));
        ruleRepository.saveAll(defaultRules);
    }

    private void loadRulesFromDatabase() {
        lock.writeLock().lock();
        try {
            rules = ruleRepository.findAllByEnabledTrue();
            logger.info("Loaded {} active signature rules from the database.", rules.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<SignatureRule> getMatchingRule(String payload) {
        if (payload == null || payload.isEmpty()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            for (SignatureRule rule : rules) {
                if (rule.getCompiledPattern().matcher(payload).find()) {
                    return Optional.of(rule);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return Optional.empty();
    }

    public List<SignatureRule> getAllRules() {
        // Return all rules from the repository, including disabled ones
        return ruleRepository.findAll();
    }

    @Transactional
    public SignatureRule addRule(SignatureRule rule) {
        SignatureRule savedRule = ruleRepository.save(rule);
        loadRulesFromDatabase(); // Refresh the rules list
        return savedRule;
    }

    @Transactional
    public Optional<SignatureRule> updateRule(Long id, SignatureRule ruleDetails) {
        return ruleRepository.findById(id).map(rule -> {
            rule.setRuleId(ruleDetails.getRuleId());
            rule.setName(ruleDetails.getName());
            rule.setPattern(ruleDetails.getPattern());
            rule.setDescription(ruleDetails.getDescription());
            rule.setEnabled(ruleDetails.isEnabled());
            SignatureRule updatedRule = ruleRepository.save(rule);
            loadRulesFromDatabase(); // Refresh the rules list
            return updatedRule;
        });
    }

    @Transactional
    public boolean deleteRule(Long id) {
        if (ruleRepository.existsById(id)) {
            ruleRepository.deleteById(id);
            loadRulesFromDatabase(); // Refresh the rules list
            return true;
        }
        return false;
    }
    
    @Transactional
    public void reloadRulesFromDatabase() {
        loadRulesFromDatabase();
    }
}