package org.example.homnayangi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // === 1. LỖI HỆ THỐNG CHUNG (9999) ===
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Mã lỗi không hợp lệ", HttpStatus.BAD_REQUEST),

    // === 2. LỖI XÁC THỰC & PHÂN QUYỀN (1000 - 1099) ===
    UNAUTHENTICATED(1002, "Chưa đăng nhập hoặc phiên làm việc hết hạn", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    USER_NOT_EXISTED(1004, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(1005, "Tên đăng nhập hoặc email đã tồn tại", HttpStatus.BAD_REQUEST),
    LOGIN_FAILED(1006, "Tài khoản hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),

    // === 3. LỖI VALIDATION DỮ LIỆU ĐẦU VÀO (1100 - 1199) ===
    USERNAME_INVALID(1101, "Tên đăng nhập phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1102, "Mật khẩu phải có ít nhất 6 ký tự", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1103, "Email không đúng định dạng", HttpStatus.BAD_REQUEST),

    // === 4. LỖI QUẢN LÝ PHÒNG & GHÉP CẶP (2000 - 2099) ===
    ROOM_NOT_FOUND(2001, "Không tìm thấy phòng ăn", HttpStatus.NOT_FOUND),
    ROOM_FULL(2002, "Phòng ăn đã đủ số lượng thành viên", HttpStatus.BAD_REQUEST),
    ROOM_CLOSED(2003, "Phòng ăn này đã đóng hoặc đã chốt đơn", HttpStatus.BAD_REQUEST),
    NOT_ROOM_HOST(2004, "Chỉ có chủ phòng mới có quyền thao tác này", HttpStatus.FORBIDDEN),
    ROOM_NOT_OPEN(2005, "Phòng này không còn mở để tham gia", HttpStatus.BAD_REQUEST),
    USER_ALREADY_IN_ROOM(2006, "Bạn đã ở trong phòng này rồi", HttpStatus.BAD_REQUEST),
    ROOM_NOT_SWIPING(2008, "Phòng này chưa bắt đầu hoặc đã kết thúc quẹt", HttpStatus.BAD_REQUEST),

    // === 5. LỖI TÀI CHÍNH & CHIA TIỀN (3000 - 3099) ===
    EXPENSE_NOT_FOUND(3001, "Không tìm thấy hóa đơn", HttpStatus.NOT_FOUND),
    BILL_AMOUNT_MISMATCH(3002, "Tổng số tiền các cá nhân chia không khớp với Tổng hóa đơn", HttpStatus.BAD_REQUEST),
    ROOM_NOT_CLOSED(3003, "Phòng ăn phải ở trạng thái CLOSED mới có thể tạo hóa đơn", HttpStatus.BAD_REQUEST),
    EXPENSE_ALREADY_EXISTS(3004, "Phòng này đã có hóa đơn được tạo rồi", HttpStatus.BAD_REQUEST),
    SPLIT_NOT_FOUND(3005, "Không tìm thấy khoản chia tiền này", HttpStatus.NOT_FOUND),
    ALREADY_PAID(3006, "Khoản tiền này đã được thanh toán trước đó", HttpStatus.BAD_REQUEST),
    NOT_EXPENSE_PAYER(3007, "Chỉ người trả tiền mới có thể thực hiện thao tác này", HttpStatus.FORBIDDEN),
    INVALID_SPLIT_TYPE(3008, "Kiểu chia tiền không hợp lệ, chỉ chấp nhận EVENLY hoặc BY_ITEM", HttpStatus.BAD_REQUEST),
    SHARER_NOT_IN_ROOM(3009, "Người chia sẻ món ăn phải là thành viên trong phòng", HttpStatus.BAD_REQUEST),

    // === 6. LỖI DỊCH VỤ BÊN NGOÀI (4000 - 4099) ===
    EXTERNAL_SERVICE_ERROR(4001, "Dịch vụ bên thứ ba (Google Maps/VietQR) gặp sự cố", HttpStatus.SERVICE_UNAVAILABLE),

    // === 7. LỖI QUẢN LÝ ĐỊA ĐIỂM (1200 - 1299) ===
    PLACE_NOT_FOUND(1201, "Địa điểm không tồn tại", HttpStatus.NOT_FOUND),
    PLACE_NAME_INVALID(1202, "Tên địa điểm không được để trống", HttpStatus.BAD_REQUEST),
    PLACE_LATITUDE_INVALID(1203, "Vĩ độ không hợp lệ", HttpStatus.BAD_REQUEST),
    PLACE_LONGITUDE_INVALID(1204, "Kinh độ không hợp lệ", HttpStatus.BAD_REQUEST),

    // === 8. LỖI BẠN BÈ & SỞ THÍCH (1300 - 1399) ===
    FRIENDSHIP_ALREADY_EXISTS(1301, "Yêu cầu kết bạn hoặc quan hệ bạn bè đã tồn tại", HttpStatus.BAD_REQUEST),
    FRIENDSHIP_NOT_FOUND(1302, "Không tìm thấy quan hệ bạn bè", HttpStatus.NOT_FOUND),
    CANNOT_FRIEND_SELF(1303, "Không thể kết bạn với chính mình", HttpStatus.BAD_REQUEST),
    TAG_ALREADY_EXISTS(1304, "Tag đã tồn tại", HttpStatus.BAD_REQUEST),
    TAG_NOT_FOUND(1305, "Tag không tồn tại", HttpStatus.NOT_FOUND),
    CANNOT_BLOCK_SELF(1306, "Không thể chặn chính mình", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}