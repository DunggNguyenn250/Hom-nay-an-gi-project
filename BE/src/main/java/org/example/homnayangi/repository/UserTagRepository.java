package org.example.homnayangi.repository;

import org.example.homnayangi.entity.UserTag;
import org.example.homnayangi.entity.UserTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, UserTagId> {
    List<UserTag> findAllByUserId(UUID userId);
    
    @Query("SELECT ut FROM UserTag ut JOIN FETCH ut.tag WHERE ut.id.userId = :userId")
    List<UserTag> findAllByUserIdWithTag(@Param("userId") UUID userId);
}
