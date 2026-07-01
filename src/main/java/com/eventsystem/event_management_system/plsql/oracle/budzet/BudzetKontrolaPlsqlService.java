package com.eventsystem.event_management_system.plsql.oracle.budzet;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.oracle.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BudzetKontrolaPlsqlService {

    private final BudzetKontrolaPlsqlDao budzetKontrolaPlsqlDao;

    public BudzetKontrolaPlsqlDao.ObradiRezultat obradiBudzet(Long budzetId) {
        return budzetKontrolaPlsqlDao.obradiBudzet(budzetId);
    }
}
