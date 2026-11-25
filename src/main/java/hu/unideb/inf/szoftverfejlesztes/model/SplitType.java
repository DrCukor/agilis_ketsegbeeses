package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// Kiadás megosztási módja a résztvevők között
public enum SplitType {
    EQUAL,   // egyenlő részek
    PERCENT, // százalékos arány (összesen 100%)
    AMOUNT,  // pontos összegek (összesen a kiadás összege)
    SHARES;  // részarány, pl. 2 : 1 : 1

    // Üres értéknél egyenlő megosztás; ismeretlen értéknél hibát dob
    public static SplitType parse(String value) {
        if (value == null || value.isBlank()) {
            return EQUAL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ismeretlen megosztási mód: " + value);
        }
    }

    // Egyszerű szöveges oszlopként tároljuk; a régi (üres) sorok egyenlő megosztást jelentenek
    @Converter
    public static class JpaConverter implements AttributeConverter<SplitType, String> {
        @Override
        public String convertToDatabaseColumn(SplitType splitType) {
            return splitType == null ? null : splitType.name();
        }

        @Override
        public SplitType convertToEntityAttribute(String value) {
            return value == null ? EQUAL : SplitType.valueOf(value);
        }
    }
}
