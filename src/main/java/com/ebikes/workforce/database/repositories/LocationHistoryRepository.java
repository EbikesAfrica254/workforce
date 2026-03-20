package com.ebikes.workforce.database.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.LocationHistory;

@Repository
public interface LocationHistoryRepository
    extends JpaRepository<LocationHistory, UUID>, JpaSpecificationExecutor<LocationHistory> {}
