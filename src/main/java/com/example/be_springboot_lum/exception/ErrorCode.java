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

    // Product Attribute errors
    PRODUCT_ATTRIBUTE_NOT_FOUND(2060, "Không tìm thấy thuộc tính sản phẩm", HttpStatus.NOT_FOUND),
    PRODUCT_ATTRIBUTE_ALREADY_EXISTS(2061, "Tên thuộc tính đã tồn tại trong danh mục này", HttpStatus.CONFLICT),
    PRODUCT_ATTRIBUTE_SELECT_OPTIONS_REQUIRED(2062, "Thuộc tính kiểu 'select' phải có ít nhất 2 lựa chọn", HttpStatus.BAD_REQUEST),

    // Tag errors
    TAG_NOT_FOUND(2070, "Không tìm thấy tag", HttpStatus.NOT_FOUND),
    TAG_ALREADY_EXISTS(2071, "Tên tag đã tồn tại", HttpStatus.CONFLICT),

    // Product errors
    PRODUCT_NOT_FOUND(2090, "Không tìm thấy tin đăng", HttpStatus.NOT_FOUND),
    PRODUCT_FORBIDDEN(2091, "Bạn không có quyền thao tác trên tin đăng này", HttpStatus.FORBIDDEN),
    PRODUCT_IMAGE_REQUIRED(2092, "Tin đăng cần ít nhất 1 ảnh", HttpStatus.BAD_REQUEST),
    PRODUCT_IMAGE_LIMIT_EXCEEDED(2093, "Tin đăng không được có quá 10 ảnh", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_OR_FREE_REQUIRED(2094, "Vui lòng nhập giá hoặc chọn 'Miễn phí'", HttpStatus.BAD_REQUEST),
    PRODUCT_STATUS_INVALID_TRANSITION(2095, "Không thể chuyển trạng thái tin đăng sang trạng thái này", HttpStatus.BAD_REQUEST),
    PRODUCT_ALREADY_DELETED(2096, "Tin đăng đã bị xóa", HttpStatus.GONE),
    PRODUCT_RENEWAL_LIMIT_EXCEEDED(2097, "Tin đăng chỉ được gia hạn tối đa 3 lần", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_EXPIRED(2098, "Chỉ có thể gia hạn khi tin đăng đã hết hạn", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_FOUND(2100, "Không tìm thấy hình ảnh", HttpStatus.NOT_FOUND),
    IMAGE_NOT_BELONG_TO_PRODUCT(2101, "Hình ảnh không thuộc về sản phẩm này", HttpStatus.BAD_REQUEST),
    FAVORITE_ALREADY_EXISTS(2102, "Tin đăng đã được lưu trước đó", HttpStatus.CONFLICT),
    FAVORITE_NOT_FOUND(2103, "Tin đăng chưa được lưu", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE_FOR_FAVORITE(2104, "Không thể lưu tin đăng ở trạng thái hiện tại", HttpStatus.BAD_REQUEST),
    VERIFICATION_TYPE_INVALID(2105, "Loại xác thực không hợp lệ", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_INVALID(2106, "Mã xác thực không hợp lệ", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED(2107, "Mã xác thực đã hết hạn", HttpStatus.BAD_REQUEST),
    VERIFICATION_REQUEST_NOT_FOUND(2108, "Không tìm thấy yêu cầu xác thực", HttpStatus.NOT_FOUND),
    VERIFICATION_CONTACT_MISSING(2109, "Thiếu thông tin liên hệ để xác thực", HttpStatus.BAD_REQUEST),
    CAMPUS_UNIVERSITY_MISMATCH(2110, "Campus không thuộc trường đại học đã chọn", HttpStatus.BAD_REQUEST),

    // Cloudinary errors
    CLOUDINARY_UPLOAD_FAILED(2080, "Tải ảnh lên thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),

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
