package org.example.homnayangi.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.FriendshipResponse;
import org.example.homnayangi.dto.response.UserResponse;
import org.example.homnayangi.service.FriendshipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friendships")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendshipController {

    FriendshipService friendshipService;

    @PostMapping("/request/{addresseeId}")
    public ApiResponse<FriendshipResponse> sendFriendRequest(@PathVariable UUID addresseeId) {
        return ApiResponse.<FriendshipResponse>builder()
                .code(1000)
                .message("Gửi lời mời kết bạn thành công")
                .result(friendshipService.sendFriendRequest(addresseeId))
                .build();
    }

    @PutMapping("/accept/{requesterId}")
    public ApiResponse<FriendshipResponse> acceptFriendRequest(@PathVariable UUID requesterId) {
        return ApiResponse.<FriendshipResponse>builder()
                .code(1000)
                .message("Chấp nhận lời mời kết bạn thành công")
                .result(friendshipService.acceptFriendRequest(requesterId))
                .build();
    }

    @DeleteMapping("/{targetUserId}")
    public ApiResponse<String> declineOrRemoveFriendship(@PathVariable UUID targetUserId) {
        friendshipService.declineOrRemoveFriendship(targetUserId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xử lý hủy kết bạn/hủy yêu cầu thành công")
                .result("Hủy kết bạn hoặc từ chối yêu cầu thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getFriends() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .message("Lấy danh sách bạn bè thành công")
                .result(friendshipService.getFriends())
                .build();
    }

    @GetMapping("/pending/incoming")
    public ApiResponse<List<FriendshipResponse>> getPendingIncomingRequests() {
        return ApiResponse.<List<FriendshipResponse>>builder()
                .code(1000)
                .message("Lấy danh sách lời mời kết bạn đến thành công")
                .result(friendshipService.getPendingIncomingRequests())
                .build();
    }

    @GetMapping("/pending/outgoing")
    public ApiResponse<List<FriendshipResponse>> getPendingOutgoingRequests() {
        return ApiResponse.<List<FriendshipResponse>>builder()
                .code(1000)
                .message("Lấy danh sách lời mời kết bạn đi thành công")
                .result(friendshipService.getPendingOutgoingRequests())
                .build();
    }
}
