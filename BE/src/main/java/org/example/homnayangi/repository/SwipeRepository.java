package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Place;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.entity.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {
    long countByRoomAndPlaceAndLiked(Room room, Place place, boolean liked);
}
