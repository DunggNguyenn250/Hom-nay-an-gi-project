package org.example.homnayangi.controller;

import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.*;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.AuthenticationResponse;
import org.example.homnayangi.dto.response.IntrospectResponse;
import org.example.homnayangi.dto.response.VerifyOtpResponse;
import org.example.homnayangi.service.AuthenticationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> authentication(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authentication(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công")
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .code(1000)
                .message("Kiểm tra Token thành công")
                .result(result)
                .build();
    }

    @PostMapping("/send-otp")
    public ApiResponse<String> sendOtp(@RequestBody SendOtpRequest request) {
        authenticationService.sendOtp(request);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Mã OTP đã được gửi đến email của bạn")
                .result("Success")
                .build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        var result = authenticationService.verifyOtp(request);
        return ApiResponse.<VerifyOtpResponse>builder()
                .code(1000)
                .message("Xác thực OTP thành công")
                .result(result)
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestBody SendOtpRequest request) {
        authenticationService.forgotPassword(request);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Yêu cầu khôi phục mật khẩu thành công. Vui lòng kiểm tra email.")
                .result("Success")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Đặt lại mật khẩu thành công")
                .result("Success")
                .build();
    }
}
