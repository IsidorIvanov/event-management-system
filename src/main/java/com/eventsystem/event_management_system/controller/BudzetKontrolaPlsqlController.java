package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.plsql.oracle.budzet.BudzetKontrolaPlsqlDao;
import com.eventsystem.event_management_system.plsql.oracle.budzet.BudzetKontrolaPlsqlService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/budzet/oracle")
@ConditionalOnProperty(name = "app.oracle.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BudzetKontrolaPlsqlController {

    private final BudzetKontrolaPlsqlService budzetKontrolaPlsqlService;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @GetMapping("/upozorenja/{budzetId}")
    public ResponseEntity<Map<String, Object>> obradiUpozorenja(@PathVariable Long budzetId) {
        BudzetKontrolaPlsqlDao.ObradiRezultat rezultat = budzetKontrolaPlsqlService.obradiBudzet(budzetId);
        return ResponseEntity.ok(Map.of(
                "budzetId", budzetId,
                "brojAlerta", rezultat.brojAlerta(),
                "upozorenja", rezultat.upozorenja()
        ));
    }
}
