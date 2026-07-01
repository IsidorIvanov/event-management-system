package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BudzetRepository extends JpaRepository<Budzet, Long> {

    boolean existsByDogadjajDogadjajIdAndStatusIn(Long dogadjajId, Collection<BudzetStatus> statuses);

    @Query("""
            SELECT DISTINCT b FROM Budzet b
            JOIN FETCH b.dogadjaj
            JOIN FETCH b.kreirao
            LEFT JOIN FETCH b.stavke s
            LEFT JOIN FETCH s.kategorija
            ORDER BY b.createdAt DESC
            """)
    List<Budzet> findAllWithDetalji();

    @Query("""
            SELECT DISTINCT b FROM Budzet b
            JOIN FETCH b.dogadjaj
            JOIN FETCH b.kreirao
            LEFT JOIN FETCH b.stavke s
            LEFT JOIN FETCH s.kategorija
            WHERE b.budzetId = :id
            """)
    Optional<Budzet> findByIdWithDetalji(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT b FROM Budzet b
            JOIN FETCH b.dogadjaj
            JOIN FETCH b.kreirao
            LEFT JOIN FETCH b.stavke s
            LEFT JOIN FETCH s.kategorija
            WHERE b.dogadjaj.dogadjajId = :dogadjajId
            ORDER BY b.createdAt DESC
            """)
    List<Budzet> findByDogadjajIdWithDetalji(@Param("dogadjajId") Long dogadjajId);
}
