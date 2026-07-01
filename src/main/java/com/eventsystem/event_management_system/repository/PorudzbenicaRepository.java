package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Porudzbenica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PorudzbenicaRepository extends JpaRepository<Porudzbenica, Long> {

    Optional<Porudzbenica> findByNabavkaNabavkaId(Long nabavkaId);

    boolean existsByNabavkaNabavkaId(Long nabavkaId);

    @Query("SELECT p FROM Porudzbenica p JOIN FETCH p.nabavka WHERE p.porudzbenicaId = :id")
    Optional<Porudzbenica> findByIdWithNabavka(@Param("id") Long id);

    @Query("""
            SELECT p FROM Porudzbenica p
            JOIN FETCH p.nabavka n
            JOIN FETCH n.dogadjaj
            JOIN FETCH p.dobavljac
            LEFT JOIN FETCH p.stavke
            WHERE p.porudzbenicaId = :id
            """)
    Optional<Porudzbenica> findByIdWithDetalji(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM Porudzbenica p
            JOIN FETCH p.nabavka n
            JOIN FETCH n.dogadjaj
            JOIN FETCH p.dobavljac
            ORDER BY p.kreiranAt DESC
            """)
    List<Porudzbenica> findAllWithDetalji();

    @Query("""
            SELECT DISTINCT p FROM Porudzbenica p
            JOIN FETCH p.nabavka n
            JOIN FETCH n.dogadjaj d
            JOIN FETCH p.dobavljac
            WHERE d.dogadjajId = :dogadjajId
            ORDER BY p.kreiranAt DESC
            """)
    List<Porudzbenica> findByDogadjajId(@Param("dogadjajId") Long dogadjajId);

    long countByBrojPorudzbeniceStartingWith(String prefix);
}
