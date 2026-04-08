package com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository;

import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DinosaurMessageRepository extends JpaRepository<DinosaurMessageEntity, Long> {}
