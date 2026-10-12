package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.CreateRoomRequest;
import org.example.homnayangi.dto.response.RoomResponse;
import org.example.homnayangi.dto.response.UserResponse;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.entity.RoomMember;
import org.example.homnayangi.entity.RoomMemberId;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.RoomMapper;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.RoomMemberRepository;
import org.example.homnayangi.repository.RoomRepository;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

    RoomRepository roomRepository;
    RoomMemberRepository roomMemberRepository;
    UserRepository userRepository;
    UserMapper userMapper;
    RoomMapper roomMapper;

    // 1. Tạo phòng mới
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        User host = getCurrentUser();

        Room newRoom = Room.builder()
                .name(request.getName()) // 👈 Set tên phòng từ Request
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
                .ready(true) // Host mặc định sẵn sàng
                .joinedAt(LocalDateTime.now())
                .build();

        roomMemberRepository.save(newMember);

        return getRoomDetails(savedRoom.getId()); // Trả về RoomResponse đầy đủ thông tin thành viên
    }

    // 2. Tham gia phòng bằng mã roomCode
    @Transactional
    public RoomResponse joinRoom(String roomCode) {
        User user = getCurrentUser();

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.OPEN) {
            throw new AppException(ErrorCode.ROOM_NOT_OPEN);
        }

        RoomMemberId roomMemberId = new RoomMemberId(room.getId(), user.getId());
        if (roomMemberRepository.existsById(roomMemberId)) {
            throw new AppException(ErrorCode.USER_ALREADY_IN_ROOM);
        }

        RoomMember newMember = RoomMember.builder()
                .id(roomMemberId)
                .room(room)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();

        roomMemberRepository.save(newMember);

        return getRoomDetails(room.getId()); // Trả về RoomResponse đầy đủ thông tin thành viên
    }

    // 3. Lấy danh sách phòng của tôi (Không lọc trạng thái, lấy tất cả phòng đã tham gia)
    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms() {
        User currentUser = getCurrentUser();

        List<RoomMember> myMemberships = roomMemberRepository.findAllByUserId(currentUser.getId());

        List<Room> rooms = myMemberships.stream()
                .map(RoomMember::getRoom)
                // Đã bỏ dòng filter CLOSED để lấy luôn phòng đã đóng
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .toList();

        return rooms.stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    // 4. Lấy thông tin chi tiết phòng & danh sách thành viên
    @Transactional(readOnly = true)
    public RoomResponse getRoomDetails(UUID roomId) {
        User currentUser = getCurrentUser();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        RoomMemberId memberId = new RoomMemberId(roomId, currentUser.getId());
        if (!roomMemberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        List<RoomMember> roomMembers = roomMemberRepository.findAllByRoomId(roomId);

        List<UserResponse> memberResponses = roomMembers.stream()
                .map(rm -> userMapper.toUserResponse(rm.getUser()))
                .toList();

        RoomResponse response = roomMapper.toRoomResponse(room);
        response.setMembers(memberResponses);

        return response;
    }

    // 5. Bắt đầu phiên quẹt chọn địa điểm (Chỉ Host)
    @Transactional
    public void startSwiping(UUID roomId) {
        User user = getCurrentUser();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.NOT_ROOM_HOST);
        }

        room.setStatus(RoomStatus.SWIPING);
        roomRepository.save(room);
    }

    // 6. Thành viên tự rời khỏi phòng
    @Transactional
    public void leaveRoom(UUID roomId) {
        User currentUser = getCurrentUser();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        RoomMemberId memberId = new RoomMemberId(roomId, currentUser.getId());
        RoomMember member = roomMemberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        if (room.getHost().getId().equals(currentUser.getId())) {
            room.setStatus(RoomStatus.CLOSED);
            roomRepository.save(room);
        }

        roomMemberRepository.delete(member);
    }

    // 7. Host xóa (kick) thành viên ra khỏi phòng
    @Transactional
    public void kickMember(UUID roomId, UUID memberUserId) {
        User currentUser = getCurrentUser();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_ROOM_HOST);
        }

        if (currentUser.getId().equals(memberUserId)) {
            throw new AppException(ErrorCode.CANNOT_KICK_SELF);
        }

        RoomMemberId memberId = new RoomMemberId(roomId, memberUserId);
        RoomMember member = roomMemberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        roomMemberRepository.delete(member);
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        return userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (roomRepository.findByRoomCode(roomCode).isPresent());
        return roomCode;
    }

    // 8. Host xóa hoàn toàn phòng ăn
    @Transactional
    public void deleteRoom(UUID roomId) {
        User currentUser = getCurrentUser();

        // 1. Kiểm tra phòng có tồn tại không
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 2. Chỉ mỗi Host mới có quyền xóa phòng
        if (!room.getHost().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_ROOM_HOST);
        }

        // 3. Xóa toàn bộ bản ghi thành viên trong phòng trước (tránh lỗi Foreign Key / Khóa ngoại DB)
        List<RoomMember> roomMembers = roomMemberRepository.findAllByRoomId(roomId);
        roomMemberRepository.deleteAll(roomMembers);

        // 4. Xóa phòng khỏi Database
        roomRepository.delete(room);
    }

    @Transactional
    public void closeRoom(UUID roomId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 1. Chỉ Host mới có quyền đóng phòng
        if (!room.getHost().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Kiểm tra nếu phòng đã đóng rồi thì báo lỗi
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new AppException(ErrorCode.ROOM_ALREADY_CLOSED);
        }

        // 3. Chuyển trạng thái phòng thành CLOSED
        room.setStatus(RoomStatus.CLOSED);
        roomRepository.save(room);
    }
}