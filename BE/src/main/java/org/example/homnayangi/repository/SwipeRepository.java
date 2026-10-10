package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Place;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.entity.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SwipeRepository extends JpaRepository<Swipe, UUID> {
    // 🌟 Sửa Object -> Room và Place
    long countByRoomAndPlaceAndLiked(Room room, Place place, boolean liked);

    // Lấy danh sách lượt quẹt của 1 User trong 1 Phòng
    List<Swipe> findAllByUserIdAndRoomId(UUID userId, UUID roomId);

    // Lấy tất cả lượt quẹt của mọi người trong 1 Phòng
    List<Swipe> findAllByRoomId(UUID roomId);
}
