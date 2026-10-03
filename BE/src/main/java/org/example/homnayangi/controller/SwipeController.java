package org.example.homnayangi.controller;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.SwipeRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.service.SwipeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/swipes")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @PostMapping
    public ApiResponse<Void> swipe(@RequestBody SwipeRequest request) {
        swipeService.swipe(request);
        return ApiResponse.<Void>builder().build();
    }
}
