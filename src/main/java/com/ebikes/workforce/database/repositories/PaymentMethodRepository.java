package com.ebikes.workforce.database.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.PaymentMethod;

@Repository
public interface PaymentMethodRepository
    extends JpaRepository<PaymentMethod, UUID>, JpaSpecificationExecutor<PaymentMethod> {

  List<PaymentMethod> findByAgentId(UUID agentId);

  Optional<PaymentMethod> findByAgentIdAndIsPrimaryTrue(UUID agentId);
}
