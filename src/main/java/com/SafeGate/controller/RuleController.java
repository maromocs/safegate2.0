package com.SafeGate.controller;

import com.SafeGate.model.SignatureRule;
import com.SafeGate.service.SignatureRulesEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    @Autowired
    private SignatureRulesEngine signatureRulesEngine;

    @GetMapping
    public List<SignatureRule> getAllRules() {
        return signatureRulesEngine.getAllRules();
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
}