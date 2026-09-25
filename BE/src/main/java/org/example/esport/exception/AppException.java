package org.example.esport.exception;

import lombok.Data;

@Data

public class AppException extends RuntimeException {

    private ErrorCode errorCode;
//     3. Bạn (hoặc Lombok @Data) đã tạo ra hàm này để sau này ai cần thì lấy ra
//    public ErrorCode getErrorCode() {
//        return this.errorCode;
//    }

    public AppException(ErrorCode errorCode) {
        // hiển thị lỗi chi tiết lên console
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
