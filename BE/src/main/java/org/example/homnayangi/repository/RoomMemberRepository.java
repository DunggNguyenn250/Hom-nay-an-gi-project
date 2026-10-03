package org.example.homnayangi.repository;

import org.example.homnayangi.entity.RoomMember;
import org.example.homnayangi.entity.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {
    List<RoomMember> findAllById_RoomId(UUID roomId);
}
