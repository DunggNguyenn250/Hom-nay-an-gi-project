package org.example.homnayangi.service;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.CreateRoomRequest;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.entity.RoomMember;
import org.example.homnayangi.entity.RoomMemberId;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.repository.RoomMemberRepository;
import org.example.homnayangi.repository.RoomRepository;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Room createRoom(CreateRoomRequest request) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User host = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room newRoom = Room.builder()
                .host(host)
                .status(RoomStatus.OPEN)
                .roomCode(generateUniqueRoomCode())
                .createdAt(LocalDateTime.now())
                .build();

        Room savedRoom = roomRepository.save(newRoom);

        RoomMember newMember = RoomMember.builder()
                .id(new RoomMemberId(savedRoom.getId(), host.getId()))
                .room(savedRoom)
                .user(host)
                .ready(true) // Host is always ready
                .joinedAt(LocalDateTime.now())
                .build();

        roomMemberRepository.save(newMember);

        return savedRoom;
    }

    @Transactional
    public Room joinRoom(String roomCode) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.OPEN) {
            throw new AppException(ErrorCode.ROOM_NOT_OPEN);
        }

        RoomMemberId roomMemberId = new RoomMemberId(room.getId(), user.getId());
        if(roomMemberRepository.existsById(roomMemberId)){
            throw new AppException(ErrorCode.USER_ALREADY_IN_ROOM);
        }

        RoomMember newMember = RoomMember.builder()
                .id(roomMemberId)
                .room(room)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();

        roomMemberRepository.save(newMember);

        return room;
    }

    @Transactional
    public void startSwiping(UUID roomId) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().equals(user)) {
            throw new AppException(ErrorCode.NOT_ROOM_HOST);
        }

        room.setStatus(RoomStatus.SWIPING);
        roomRepository.save(room);
    }


    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (roomRepository.findByRoomCode(roomCode).isPresent());
        return roomCode;
    }
}
