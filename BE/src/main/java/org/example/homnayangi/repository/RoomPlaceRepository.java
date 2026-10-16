package org.example.homnayangi.repository;

import org.example.homnayangi.entity.RoomPlace;
import org.example.homnayangi.entity.RoomPlaceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoomPlaceRepository extends JpaRepository<RoomPlace, RoomPlaceId> {

    List<RoomPlace> findAllById_RoomId(UUID roomId);

    @Query("SELECT rp FROM RoomPlace rp JOIN FETCH rp.place WHERE rp.room.id = :roomId")
    List<RoomPlace> findAllByRoomIdWithPlace(@Param("roomId") UUID roomId);

    boolean existsById_RoomIdAndId_PlaceId(UUID roomId, UUID placeId);

    void deleteAllById_RoomId(UUID roomId);

    @Modifying
    @Query(value = "DELETE FROM room_places WHERE room_id IN (SELECT id FROM rooms WHERE created_at < :cutoffTime)", nativeQuery = true)
    int deleteOldRoomPlaces(@Param("cutoffTime") LocalDateTime cutoffTime);
}
