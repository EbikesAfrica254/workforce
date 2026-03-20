package com.ebikes.workforce.database.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.Suspension;

@Repository
public interface SuspensionRepository
    extends JpaRepository<Suspension, UUID>, JpaSpecificationExecutor<Suspension> {

  Optional<Suspension> findByAgentIdAndLiftedAtIsNull(UUID agentId);
}
