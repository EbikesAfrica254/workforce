package com.ebikes.workforce.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.Inbox;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, String> {}
