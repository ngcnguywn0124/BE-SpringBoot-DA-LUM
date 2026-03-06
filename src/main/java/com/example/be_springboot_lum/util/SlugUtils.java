package com.example.be_springboot_lum.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class để tạo slug từ chuỗi văn bản tiếng Việt hoặc tiếng Anh.
 * Ví dụ: "Đại học Bách Khoa Hà Nội" -> "dai-hoc-bach-khoa-ha-noi"
 */
public final class SlugUtils {

    private static final Pattern NON_LATIN      = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE     = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN   = Pattern.compile("-{2,}");
    private static final Pattern EDGE_HYPHEN    = Pattern.compile("(^-|-$)");

    private SlugUtils() {}

    /**
     * Chuyển đổi chuỗi sang slug URL-friendly.
     * Hỗ trợ tiếng Việt có dấu.
     *
     * @param input chuỗi đầu vào
     * @return slug tương ứng, ví dụ: "dai-hoc-bach-khoa-ha-noi"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // 1. Chuẩn hóa Việt Nam: Xử lý các ký tự đ, Đ đặc biệt trước khi Normalizer
        String processed = input.trim()
                .replace("đ", "d")
                .replace("Đ", "d");

        // 2. Chuẩn hóa Unicode về dạng NFD để tách dấu ra khỏi ký tự gốc
        String normalized = Normalizer.normalize(processed, Normalizer.Form.NFD);

        // 3. Loại bỏ các combining mark (dấu tiếng Việt)
        String withoutAccents = normalized.replaceAll("\\p{M}", "");

        // 4. Chuyển thành chữ thường
        String lower = withoutAccents.toLowerCase(Locale.ROOT);

        // 5. Thay khoảng trắng bằng dấu gạch ngang
        String hyphenated = WHITESPACE.matcher(lower).replaceAll("-");

        // 6. Loại bỏ ký tự không hợp lệ (giữ lại a-z, 0-9, -)
        String slug = NON_LATIN.matcher(hyphenated).replaceAll("");

        // 7. Xử lý nhiều dấu gạch ngang liên tiếp
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");

        // 8. Loại bỏ dấu gạch ngang ở đầu/cuối
        slug = EDGE_HYPHEN.matcher(slug).replaceAll("");

        return slug;
    }
}
