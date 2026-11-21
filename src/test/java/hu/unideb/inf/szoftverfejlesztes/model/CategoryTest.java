package hu.unideb.inf.szoftverfejlesztes.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void parse_shouldReturnCorrectCategory() {
        assertEquals(Category.ETEL, Category.parse("ETEL"));
        assertEquals(Category.UTAZAS, Category.parse("UTAZAS"));
        assertEquals(Category.LAKAS, Category.parse("LAKAS"));
        assertEquals(Category.SZORAKOZAS,
                Category.parse("SZORAKOZAS"));
        assertEquals(Category.EGYEB, Category.parse("EGYEB"));
    }

    @Test
    void parse_shouldIgnoreCase() {
        assertEquals(Category.ETEL, Category.parse("etel"));
        assertEquals(Category.UTAZAS, Category.parse("utazas"));
        assertEquals(Category.LAKAS, Category.parse("lakas"));
    }

    @Test
    void parse_shouldIgnoreWhitespace() {
        assertEquals(Category.ETEL, Category.parse("  ETEL  "));
        assertEquals(Category.UTAZAS,
                Category.parse(" utazas "));
    }

    @Test
    void parse_shouldReturnOtherForNullOrBlank() {
        assertEquals(Category.EGYEB, Category.parse(null));
        assertEquals(Category.EGYEB, Category.parse(""));
        assertEquals(Category.EGYEB, Category.parse("   "));
    }

    @Test
    void parse_shouldThrowExceptionForInvalidCategory() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> Category.parse("NEMLETEZO"));

        assertEquals(
                "Ismeretlen kategória: NEMLETEZO",
                exception.getMessage()
        );
    }

    @Test
    void converter_shouldConvertCategoryToDatabaseValue() {
        Category.JpaConverter converter =
                new Category.JpaConverter();

        assertEquals("ETEL",
                converter.convertToDatabaseColumn(Category.ETEL));

        assertEquals("UTAZAS",
                converter.convertToDatabaseColumn(Category.UTAZAS));

        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void converter_shouldConvertDatabaseValueToCategory() {
        Category.JpaConverter converter =
                new Category.JpaConverter();

        assertEquals(Category.ETEL,
                converter.convertToEntityAttribute("ETEL"));

        assertEquals(Category.LAKAS,
                converter.convertToEntityAttribute("LAKAS"));

        assertEquals(Category.EGYEB,
                converter.convertToEntityAttribute(null));
    }
}