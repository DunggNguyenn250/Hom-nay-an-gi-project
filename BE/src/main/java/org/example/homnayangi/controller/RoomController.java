package org.example.homnayangi.controller;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.CreateRoomRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.RoomResponse;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.mapper.RoomMapper;
import org.example.homnayangi.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @PostMapping
    public ApiResponse<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request);
        return ApiResponse.<RoomResponse>builder()
                .result(roomMapper.toRoomResponse(room))
                .build();
    }

    @PostMapping("/join/{roomCode}")
    public ApiResponse<RoomResponse> joinRoom(@PathVariable String roomCode) {
        Room room = roomService.joinRoom(roomCode);
        return ApiResponse.<RoomResponse>builder()
                .result(roomMapper.toRoomResponse(room))
                .build();
    }

    @PostMapping("/{roomId}/start")
    public ApiResponse<Void> startSwiping(@PathVariable UUID roomId) {
        roomService.startSwiping(roomId);
        return ApiResponse.<Void>builder().build();
    }
}
