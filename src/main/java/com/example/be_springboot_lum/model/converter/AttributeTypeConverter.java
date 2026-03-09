package com.example.be_springboot_lum.model.converter;

import com.example.be_springboot_lum.model.AttributeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Chuyển đổi giữa enum AttributeType và chuỗi chữ thường lưu trong DB.
 * DB  : 'text' | 'number' | 'boolean' | 'select'
 * Java: AttributeType.TEXT | NUMBER | BOOLEAN | SELECT
 */
@Converter(autoApply = true)
public class AttributeTypeConverter implements AttributeConverter<AttributeType, String> {

    @Override
    public String convertToDatabaseColumn(AttributeType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public AttributeType convertToEntityAttribute(String dbData) {
        return AttributeType.fromDbValue(dbData);
    }
}
