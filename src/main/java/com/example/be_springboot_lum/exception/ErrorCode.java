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

    // University errors
    UNIVERSITY_NOT_FOUND(2010, "Không tìm thấy trường đại học", HttpStatus.NOT_FOUND),
    UNIVERSITY_ALREADY_EXISTS(2011, "Tên trường đại học đã tồn tại", HttpStatus.CONFLICT),

    // Campus errors
    CAMPUS_NOT_FOUND(2020, "Không tìm thấy cơ sở / campus", HttpStatus.NOT_FOUND),
    CAMPUS_ALREADY_EXISTS(2021, "Tên cơ sở đã tồn tại trong trường này", HttpStatus.CONFLICT),

    // Role errors
    ROLE_NOT_FOUND(2030, "Không tìm thấy role", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(2031, "Tên role đã tồn tại", HttpStatus.CONFLICT),

    // Permission errors (entity)
    PERMISSION_NOT_FOUND(2040, "Không tìm thấy permission", HttpStatus.NOT_FOUND),

    // Category errors
    CATEGORY_NOT_FOUND(2050, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS(2051, "Tên danh mục đã tồn tại trong cấp này", HttpStatus.CONFLICT),
    CATEGORY_CIRCULAR_REFERENCE(2052, "Không thể đặt danh mục con thành cha của chính nó", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_PRODUCTS(2053, "Danh mục đang có sản phẩm, không thể xóa", HttpStatus.CONFLICT),
    CATEGORY_HAS_CHILDREN(2054, "Danh mục đang có danh mục con, vui lòng xóa danh mục con trước", HttpStatus.CONFLICT),

    // Cloudinary errors
    CLOUDINARY_UPLOAD_FAILED(2060, "Tải ảnh lên thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),

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
