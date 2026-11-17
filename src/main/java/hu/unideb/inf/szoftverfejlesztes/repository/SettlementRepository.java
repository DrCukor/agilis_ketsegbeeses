package hu.unideb.inf.szoftverfejlesztes.repository;

import hu.unideb.inf.szoftverfejlesztes.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}
