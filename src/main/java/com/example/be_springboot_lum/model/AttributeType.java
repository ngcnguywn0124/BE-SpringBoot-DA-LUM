package com.example.be_springboot_lum.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kiểu dữ liệu của thuộc tính sản phẩm.
 * Các giá trị trong DB: 'text', 'number', 'boolean', 'select'.
 * Enum dùng tên viết hoa để tránh xung đột với từ khóa Java (boolean).
 */
public enum AttributeType {

    TEXT("text"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    SELECT("select");

    private final String dbValue;

    AttributeType(String dbValue) {
        this.dbValue = dbValue;
    }

    /** Giá trị lưu xuống DB (chữ thường). */
    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    /** Chuyển chuỗi DB → enum (không phân biệt hoa / thường). */
    @JsonCreator
    public static AttributeType fromDbValue(String value) {
        if (value == null) return null;
        for (AttributeType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value)) return type;
        }
        throw new IllegalArgumentException("Unknown attribute type: " + value);
    }
}
