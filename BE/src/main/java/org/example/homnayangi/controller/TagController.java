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

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa tag thành công")
                .result("Tag has been deleted successfully")
                .build();
    }

    @GetMapping("/user")
    public ApiResponse<List<UserTagResponse>> getUserTags() {
        return ApiResponse.<List<UserTagResponse>>builder()
                .code(1000)
                .message("Lấy danh sách tag sở thích của người dùng thành công")
                .result(tagService.getUserTags())
                .build();
    }

    @PostMapping("/user/{tagId}")
    public ApiResponse<UserTagResponse> addUserTag(
            @PathVariable UUID tagId,
            @RequestParam(defaultValue = "false") boolean temporary) {
        return ApiResponse.<UserTagResponse>builder()
                .code(1000)
                .message("Thêm sở thích thành công")
                .result(tagService.addUserTag(tagId, temporary))
                .build();
    }

    @DeleteMapping("/user/{tagId}")
    public ApiResponse<String> removeUserTag(@PathVariable UUID tagId) {
        tagService.removeUserTag(tagId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa sở thích thành công")
                .result("User tag has been removed successfully")
                .build();
    }
}
