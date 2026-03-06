package com.example.be_springboot_lum.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth errors
    EMAIL_ALREADY_EXISTS(1001, "Email đã được sử dụng", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(1002, "Số điện thoại đã được sử dụng", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(1003, "Email/Số điện thoại hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_INACTIVE(1004, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    TOKEN_INVALID(1005, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1006, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    PASSWORD_MISMATCH(1007, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INCORRECT(1008, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),

    // OAuth errors
    OAUTH_GOOGLE_INVALID_TOKEN(1009, "Google token không hợp lệ", HttpStatus.UNAUTHORIZED),
    OAUTH_ACCOUNT_NOT_LINKED(1010, "Tài khoản chưa liên kết với mạng xã hội", HttpStatus.BAD_REQUEST),
    OAUTH_STATE_MISMATCH(1011, "OAuth state không hợp lệ, vui lòng thử lại", HttpStatus.BAD_REQUEST),
    OAUTH_CODE_EXCHANGE_FAILED(1012, "Không thể đổi authorization code, vui lòng thử lại", HttpStatus.BAD_GATEWAY),

    // User errors
    USER_NOT_FOUND(2001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),

    // Permission errors
    ACCESS_DENIED(3001, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(3002, "Vui lòng đăng nhập để tiếp tục", HttpStatus.UNAUTHORIZED),

    // General errors
    VALIDATION_ERROR(4001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(5000, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    TERMS_NOT_ACCEPTED(4002, "Bạn phải đồng ý với điều khoản sử dụng", HttpStatus.BAD_REQUEST),
    SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED(4003, "Tài khoản đăng nhập bằng mạng xã hội không thể đặt lại mật khẩu", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
