package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByTagName(String tagName);
    boolean existsByTagName(String tagName);
}
