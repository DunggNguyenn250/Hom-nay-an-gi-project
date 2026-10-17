package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.CreateRoomRequest;
import org.example.homnayangi.dto.request.SelectPlaceRequest;
import org.example.homnayangi.dto.response.PlaceResponse;
import org.example.homnayangi.dto.response.RoomResponse;
import org.example.homnayangi.dto.response.UserResponse;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.PlaceMapper;
import org.example.homnayangi.mapper.RoomMapper;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

    static final int MIN_DECK_SIZE = 3;

    RoomRepository roomRepository;
    RoomMemberRepository roomMemberRepository;
    UserRepository userRepository;
    RoomPlaceRepository roomPlaceRepository;
    PlaceRepository placeRepository;
    ExpenseRepository expenseRepository;
    FoursquareService foursquareService;
    UserMapper userMapper;
    RoomMapper roomMapper;
    PlaceMapper placeMapper;

    // 1. Tạo phòng mới
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        User host = getCurrentUser();

        Room newRoom = Room.builder()
                .name(request.getName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radius(request.getRadius() != null ? request.getRadius() : 5.0)
                .deckSize(request.getDeckSize() != null ? request.getDeckSize() : 15)
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

    // 3. Lấy danh sách phòng của tôi (Không cần danh sách thành viên chi tiết)
    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms() {
        User currentUser = getCurrentUser();

        List<RoomMember> myMemberships = roomMemberRepository.findAllByUserId(currentUser.getId());

        List<Room> rooms = myMemberships.stream()
                .map(RoomMember::getRoom)
                .filter(room -> room.getStatus() != RoomStatus.CLOSED)
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .toList();

        // Map trực tiếp từ Room sang RoomResponse (members sẽ là null/rỗng)
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

        // Tự động sinh và khóa Deck nếu phòng chưa có bộ bài (Tránh desync giữa start và deck)
        List<RoomPlace> existingDeck = roomPlaceRepository.findAllByRoomIdWithPlace(roomId);
        if (existingDeck.isEmpty()) {
            generateOrGetDeck(roomId);
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

        // 3. Xóa các bản ghi liên quan trong phòng trước (tránh lỗi Foreign Key)
        roomPlaceRepository.deleteAllById_RoomId(roomId);
        List<RoomMember> roomMembers = roomMemberRepository.findAllByRoomId(roomId);
        roomMemberRepository.deleteAll(roomMembers);

        // 4. Xóa phòng khỏi Database
        roomRepository.delete(room);
    }

    // 9. Khởi tạo hoặc lấy lại bộ bài quẹt cho phòng (Smart Hybrid Fill + Idempotency)
    @Transactional
    public List<PlaceResponse> generateOrGetDeck(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        User currentUser = getCurrentUser();
        RoomMemberId memberId = new RoomMemberId(roomId, currentUser.getId());
        if (!roomMemberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // a) Kiểm tra phòng đã thiết lập tọa độ chưa
        if (room.getLatitude() == null || room.getLongitude() == null) {
            throw new AppException(ErrorCode.ROOM_LOCATION_NOT_SET);
        }

        // b) Tính lũy đẳng (Idempotency): Kiểm tra trong room_places xem phòng đã có danh sách quán chưa
        List<RoomPlace> existingRoomPlaces = roomPlaceRepository.findAllByRoomIdWithPlace(roomId);
        if (!existingRoomPlaces.isEmpty()) {
            return existingRoomPlaces.stream()
                    .map(RoomPlace::getPlace)
                    .map(placeMapper::toPlaceResponse)
                    .toList();
        }

        // c) Xác định số lượng quán mục tiêu cho Deck
        int targetSize = (room.getDeckSize() != null && room.getDeckSize() > 0) ? room.getDeckSize() : 15;

        double lat = room.getLatitude().doubleValue();
        double lon = room.getLongitude().doubleValue();
        double radius = room.getRadius() != null ? room.getRadius() : 5.0;

        // d) Ưu tiên quét quán ăn từ Database nội bộ (tái sử dụng query Haversine)
        List<Place> pool = new ArrayList<>(placeRepository.findNearbyPlaces(lat, lon, radius));

        // e) Nếu DB thiếu quán (< targetSize), gọi Foursquare Places API để bù đắp
        if (pool.size() < targetSize) {
            List<Place> fsqPlaces = foursquareService.fetchNearbyRestaurants(lat, lon, radius, targetSize * 2);

            for (Place fsqPlace : fsqPlaces) {
                // Chỉ lưu vào DB nếu quán chưa tồn tại
                if (!placeRepository.existsByName(fsqPlace.getName())) {
                    Place saved = placeRepository.save(fsqPlace);
                    pool.add(saved);
                }
            }
        }

        // Chốt chặn bảo vệ: Nếu sau cả DB lẫn External API mà vẫn không đủ số quán tối thiểu
        if (pool.size() < MIN_DECK_SIZE) {
            throw new AppException(ErrorCode.NO_PLACES_NEARBY);
        }

        // f) Chọn quán từ pool: Xáo trộn ngẫu nhiên để bộ bài luôn phong phú, lấy tối đa targetSize quán
        List<Place> selectedPlaces;
        if (pool.size() > targetSize) {
            Collections.shuffle(pool);
            selectedPlaces = pool.subList(0, targetSize);
        } else {
            selectedPlaces = pool;
        }

        // g) Tạo các bản ghi RoomPlace gắn kết roomId với các Place vừa chọn và lưu vào room_places
        List<RoomPlace> newRoomPlaces = selectedPlaces.stream()
                .map(place -> RoomPlace.builder()
                        .id(new RoomPlaceId(room.getId(), place.getId()))
                        .room(room)
                        .place(place)
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();

        roomPlaceRepository.saveAll(newRoomPlaces);

        // h) Map danh sách Place sang DTO phản hồi chuẩn (List<PlaceResponse>)
        return selectedPlaces.stream()
                .map(placeMapper::toPlaceResponse)
                .toList();
    }

    // 10. Lấy danh sách bộ bài quẹt đã khóa của phòng (dành cho thành viên xem lại/lấy bài)
    @Transactional(readOnly = true)
    public List<PlaceResponse> getDeck(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        User currentUser = getCurrentUser();
        RoomMemberId memberId = new RoomMemberId(roomId, currentUser.getId());
        if (!roomMemberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        List<RoomPlace> roomPlaces = roomPlaceRepository.findAllByRoomIdWithPlace(roomId);
        return roomPlaces.stream()
                .map(RoomPlace::getPlace)
                .map(placeMapper::toPlaceResponse)
                .toList();
    }

    // 11. Host chủ động đóng phòng để kết thúc phiên quẹt / chuyển sang chia tiền
    @Transactional
    public void closeRoom(UUID roomId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 1. Chỉ Host mới có quyền đóng phòng
        if (!room.getHost().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_ROOM_HOST);
        }

        // 2. Kiểm tra nếu phòng đã đóng rồi thì báo lỗi
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new AppException(ErrorCode.ROOM_ALREADY_CLOSED);
        }

        // 3. Chuyển trạng thái phòng thành CLOSED
        room.setStatus(RoomStatus.CLOSED);
        roomRepository.save(room);
    }

    // 12. Host chủ động chọn/chốt quán ăn cho phòng (khi hòa điểm hoặc chốt sớm)
    @Transactional
    public RoomResponse selectPlace(UUID roomId, SelectPlaceRequest request) {
        User currentUser = getCurrentUser();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 1. Chỉ Host mới có quyền chọn quán ăn cho phòng
        if (!room.getHost().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.NOT_ROOM_HOST);
        }

        // 2. Nếu phòng đã tạo hóa đơn chia tiền (Expense), không cho phép đổi quán nữa
        if (expenseRepository.existsByRoomId(roomId)) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_PLACE_AFTER_EXPENSE);
        }

        // 3. Tìm quán ăn và kiểm tra xem có thuộc bộ bài (Deck) của phòng không
        Place place = placeRepository.findById(request.getPlaceId())
                .orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND));

        boolean isPlaceInDeck = roomPlaceRepository.existsById_RoomIdAndId_PlaceId(room.getId(), place.getId());
        if (!isPlaceInDeck) {
            throw new AppException(ErrorCode.PLACE_NOT_IN_ROOM_DECK);
        }

        // 4. Gán quán đã chọn và đóng phiên quẹt (chuyển sang CLOSED)
        room.setMatchedPlace(place);
        room.setStatus(RoomStatus.CLOSED);
        roomRepository.save(room);

        return getRoomDetails(room.getId());
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
}
