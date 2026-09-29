package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    List<Place> findByCategoryContainingIgnoreCase(String category);

    List<Place> findByNameContainingIgnoreCase(String name);

    @Query(value = "SELECT * FROM (" +
            "  SELECT *, (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * " +
            "  cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "  sin(radians(latitude)))) AS distance " +
            "  FROM places" +
            ") d " +
            "WHERE d.distance <= :radius " +
            "ORDER BY d.distance", nativeQuery = true)
    List<Place> findNearbyPlaces(@Param("latitude") double latitude,
                                 @Param("longitude") double longitude,
                                 @Param("radius") double radius);

    @Query(value = "SELECT * FROM (" +
            "  SELECT *, (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * " +
            "  cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "  sin(radians(latitude)))) AS distance " +
            "  FROM places" +
            ") d " +
            "WHERE d.distance <= :radius " +
            "AND (:category IS NULL OR LOWER(d.category) = LOWER(:category)) " +
            "ORDER BY d.distance", nativeQuery = true)
    List<Place> findNearbyPlacesByCategory(@Param("latitude") double latitude,
                                           @Param("longitude") double longitude,
                                           @Param("radius") double radius,
                                           @Param("category") String category);
}
