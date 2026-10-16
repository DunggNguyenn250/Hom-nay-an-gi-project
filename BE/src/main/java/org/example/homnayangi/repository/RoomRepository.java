package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Room;
import org.example.homnayangi.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByRoomCode(String roomCode);

    @Modifying
    @Query("UPDATE Room r SET r.status = :closedStatus WHERE r.status != :closedStatus AND r.createdAt < :cutoffTime")
    int closeAbandonedRooms(@Param("closedStatus") RoomStatus closedStatus,
                            @Param("cutoffTime") LocalDateTime cutoffTime);
}
