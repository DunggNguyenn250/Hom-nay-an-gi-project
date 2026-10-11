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

    // 1. Gửi lời mời kết bạn
    @PostMapping("/requests/{addresseeId}")
    public ApiResponse<FriendshipResponse> sendFriendRequest(@PathVariable UUID addresseeId) {
        return ApiResponse.<FriendshipResponse>builder()
                .code(1000)
                .message("Gửi lời mời kết bạn thành công")
                .result(friendshipService.sendFriendRequest(addresseeId))
                .build();
    }

    // 2. Chấp nhận lời mời kết bạn từ người khác
    @PutMapping("/requests/{requesterId}/accept")
    public ApiResponse<FriendshipResponse> acceptFriendRequest(@PathVariable UUID requesterId) {
        return ApiResponse.<FriendshipResponse>builder()
                .code(1000)
                .message("Chấp nhận lời mời kết bạn thành công")
                .result(friendshipService.acceptFriendRequest(requesterId))
                .build();
    }

    // 3. Hủy kết bạn / Từ chối lời mời / Thu hồi lời mời đã gửi
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<String> declineOrRemoveFriendship(@PathVariable UUID targetUserId) {
        friendshipService.declineOrRemoveFriendship(targetUserId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xử lý hủy kết bạn hoặc từ chối yêu cầu thành công")
                .result("Hủy kết bạn hoặc từ chối yêu cầu thành công")
                .build();
    }

    // 4. Lấy danh sách bạn bè chính thức của người dùng hiện tại
    @GetMapping
    public ApiResponse<List<UserResponse>> getFriends() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .message("Lấy danh sách bạn bè thành công")
                .result(friendshipService.getFriends())
                .build();
    }

    // 5. Lấy danh sách lời mời kết bạn gửi ĐẾN mình (đang chờ xử lý)
    @GetMapping("/requests/incoming")
    public ApiResponse<List<FriendshipResponse>> getPendingIncomingRequests() {
        return ApiResponse.<List<FriendshipResponse>>builder()
                .code(1000)
                .message("Lấy danh sách lời mời kết bạn đến thành công")
                .result(friendshipService.getPendingIncomingRequests())
                .build();
    }

    // 6. Lấy danh sách lời mời kết bạn ĐÃ GỬI ĐI (đang chờ đối phương đồng ý)
    @GetMapping("/requests/outgoing")
    public ApiResponse<List<FriendshipResponse>> getPendingOutgoingRequests() {
        return ApiResponse.<List<FriendshipResponse>>builder()
                .code(1000)
                .message("Lấy danh sách lời mời kết bạn đi thành công")
                .result(friendshipService.getPendingOutgoingRequests())
                .build();
    }
}