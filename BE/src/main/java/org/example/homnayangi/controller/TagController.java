package org.example.homnayangi.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.TagRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.TagResponse;
import org.example.homnayangi.dto.response.UserTagResponse;
import org.example.homnayangi.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TagController {

    TagService tagService;

    // ==========================================
    // 1. DÀNH CHO ADMIN & PUBLIC (Tag hệ thống)
    // ==========================================

    @PostMapping
    public ApiResponse<TagResponse> createTag(@RequestBody @Valid TagRequest request) {
        return ApiResponse.<TagResponse>builder()
                .code(1000)
                .message("Tạo tag sở thích thành công")
                .result(tagService.createTag(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.<List<TagResponse>>builder()
                .code(1000)
                .message("Lấy danh sách các tag sở thích thành công")
                .result(tagService.getAllTags())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> getTag(@PathVariable UUID id) {
        return ApiResponse.<TagResponse>builder()
                .code(1000)
                .message("Lấy thông tin tag thành công")
                .result(tagService.getTag(id))
                .build();
    }

    // --- Bổ sung PUT cho ADMIN sửa thông tin Tag hệ thống ---
    @PutMapping("/{id}")
    public ApiResponse<TagResponse> updateTag(
            @PathVariable UUID id,
            @RequestBody @Valid TagRequest request) {
        return ApiResponse.<TagResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin tag thành công")
                .result(tagService.updateTag(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa tag thành công")
                .result("Tag has been deleted successfully")
                .build();
    }

    // ==========================================
    // 2. DÀNH CHO USER CÁ NHÂN (User Tag - me)
    // ==========================================

    @GetMapping("/user/me")
    public ApiResponse<List<UserTagResponse>> getUserTags() {
        return ApiResponse.<List<UserTagResponse>>builder()
                .code(1000)
                .message("Lấy danh sách tag sở thích thành công")
                .result(tagService.getUserTags())
                .build();
    }

    @PostMapping("/user/me/{tagId}")
    public ApiResponse<UserTagResponse> addUserTag(
            @PathVariable UUID tagId,
            @RequestParam(defaultValue = "false") boolean temporary) {
        return ApiResponse.<UserTagResponse>builder()
                .code(1000)
                .message("Thêm sở thích thành công")
                .result(tagService.addUserTag(tagId, temporary))
                .build();
    }

    // --- Bổ sung PUT cho USER cập nhật trạng thái Tag cá nhân ---
    @PutMapping("/user/me/{tagId}")
    public ApiResponse<UserTagResponse> updateUserTag(
            @PathVariable UUID tagId,
            @RequestParam boolean temporary) {
        return ApiResponse.<UserTagResponse>builder()
                .code(1000)
                .message("Cập nhật trạng thái sở thích thành công")
                .result(tagService.updateUserTag(tagId, temporary))
                .build();
    }

    @DeleteMapping("/user/me/{tagId}")
    public ApiResponse<String> removeUserTag(@PathVariable UUID tagId) {
        tagService.removeUserTag(tagId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa sở thích thành công")
                .result("Tag người dùng đã được xóa thành công.")
                .build();
    }
}