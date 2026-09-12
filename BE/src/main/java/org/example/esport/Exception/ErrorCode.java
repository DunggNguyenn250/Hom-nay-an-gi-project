package org.example.esport.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định",HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Mã lỗi không hợp lệ",HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1010,"Unauthenticated", HttpStatus.UNAUTHORIZED), // chưa đăng nhập hoặc Token JWT không hợp lệ/hết hạn
    UNAUTHORIZED(1011,"You do not have permission", HttpStatus.FORBIDDEN), // không có quyền
    ;

    private int code;
    private String message;
    private HttpStatus statusCode;


    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
