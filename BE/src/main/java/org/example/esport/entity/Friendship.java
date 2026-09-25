package org.example.esport.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.esport.enums.FriendshipStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Friendship {

    @EmbeddedId
    FriendshipId id;

    @ManyToOne
    @MapsId("requesterId")
    @JoinColumn(name = "requester_id")
    User requester;

    @ManyToOne
    @MapsId("addresseeId")
    @JoinColumn(name = "addressee_id")
    User addressee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    FriendshipStatus status = FriendshipStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
