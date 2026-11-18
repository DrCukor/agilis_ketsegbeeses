package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// Kiadás kategóriája (a megjelenített nevet és ikont a frontend adja: common.js CATEGORIES)
public enum Category {
    ETEL, UTAZAS, LAKAS, SZORAKOZAS, EGYEB;

    // Üres értéknél "Egyéb"; ismeretlen értéknél hibát dob
    public static Category parse(String value) {
        if (value == null || value.isBlank()) {
            return EGYEB;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ismeretlen kategória: " + value);
        }
    }

    // Egyszerű szöveges oszlopként tároljuk; a régi (üres) sorok "Egyéb" kategóriát kapnak
    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<Category, String> {
        @Override
        public String convertToDatabaseColumn(Category category) {
            return category == null ? null : category.name();
        }

        @Override
        public Category convertToEntityAttribute(String value) {
            return value == null ? EGYEB : Category.valueOf(value);
        }
    }
}
