package org.example.homnayangi.repository;

import org.example.homnayangi.entity.Friendship;
import org.example.homnayangi.entity.FriendshipId;
import org.example.homnayangi.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.id.requesterId = :user1 AND f.id.addresseeId = :user2) OR " +
           "(f.id.requesterId = :user2 AND f.id.addresseeId = :user1)")
    Optional<Friendship> findFriendshipBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

    @Query("SELECT f FROM Friendship f WHERE f.id.requesterId = :userId AND f.status = :status")
    List<Friendship> findAllByRequesterIdAndStatus(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.id.addresseeId = :userId AND f.status = :status")
    List<Friendship> findAllByAddresseeIdAndStatus(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.id.requesterId = :userId) OR (f.id.addresseeId = :userId)) AND " +
           "f.status = :status")
    List<Friendship> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);
}
