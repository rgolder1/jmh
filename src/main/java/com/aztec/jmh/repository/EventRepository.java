package com.aztec.jmh.repository;

import java.util.List;
import java.util.UUID;

import com.aztec.jmh.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
}
