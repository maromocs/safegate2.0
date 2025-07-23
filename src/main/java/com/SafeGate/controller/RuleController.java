package com.SafeGate.controller;

import com.SafeGate.model.SignatureRule;
import com.SafeGate.repository.SignatureRuleRepository;
import com.SafeGate.service.SignatureRulesEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    @Autowired
    private SignatureRulesEngine signatureRulesEngine;
    
    @Autowired
    private SignatureRuleRepository signatureRuleRepository;

    @GetMapping
    public List<SignatureRule> getAllRules() {
        return signatureRulesEngine.getAllRules();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SignatureRule> getRuleById(@PathVariable Long id) {
        Optional<SignatureRule> rule = signatureRuleRepository.findById(id);
        return rule.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public SignatureRule addRule(@RequestBody SignatureRule rule) {
        return signatureRulesEngine.addRule(rule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SignatureRule> updateRule(@PathVariable Long id, @RequestBody SignatureRule ruleDetails) {
        return signatureRulesEngine.updateRule(id, ruleDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        if (signatureRulesEngine.deleteRule(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}/toggle")
    public ResponseEntity<SignatureRule> toggleRuleStatus(@PathVariable Long id) {
        Optional<SignatureRule> optionalRule = signatureRuleRepository.findById(id);
        if (optionalRule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SignatureRule rule = optionalRule.get();
        rule.setEnabled(!rule.isEnabled());
        signatureRuleRepository.save(rule);
        signatureRulesEngine.reloadRulesFromDatabase();
        return ResponseEntity.ok(rule);
    }
}