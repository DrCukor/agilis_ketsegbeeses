package hu.unideb.inf.szoftverfejlesztes.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyTest {

    @Test
    void parse_shouldReturnCorrectCurrency() {
        assertEquals(Currency.HUF, Currency.parse("HUF"));
        assertEquals(Currency.EUR, Currency.parse("EUR"));
        assertEquals(Currency.USD, Currency.parse("USD"));
        assertEquals(Currency.GBP, Currency.parse("GBP"));
    }

    @Test
    void parse_shouldIgnoreCase() {
        assertEquals(Currency.HUF, Currency.parse("huf"));
        assertEquals(Currency.EUR, Currency.parse("eur"));
        assertEquals(Currency.USD, Currency.parse("usd"));
    }

    @Test
    void parse_shouldIgnoreWhitespace() {
        assertEquals(Currency.HUF, Currency.parse(" HUF "));
        assertEquals(Currency.EUR, Currency.parse("  eur  "));
    }

    @Test
    void parse_shouldReturnHufForNullOrBlank() {
        assertEquals(Currency.HUF, Currency.parse(null));
        assertEquals(Currency.HUF, Currency.parse(""));
        assertEquals(Currency.HUF, Currency.parse("   "));
    }

    @Test
    void parse_shouldThrowExceptionForInvalidCurrency() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> Currency.parse("XYZ"));

        assertEquals(
                "Ismeretlen pénznem: XYZ",
                exception.getMessage()
        );
    }

    @Test
    void getEpsilon_shouldReturnCorrectHufValue() {
        assertEquals(0.5, Currency.HUF.getEpsilon(), 0.000001);
    }

    @Test
    void getEpsilon_shouldReturnCorrectForeignCurrencyValues() {
        assertEquals(0.005, Currency.EUR.getEpsilon(), 0.000001);
        assertEquals(0.005, Currency.USD.getEpsilon(), 0.000001);
        assertEquals(0.005, Currency.GBP.getEpsilon(), 0.000001);
    }

    @Test
    void converter_shouldConvertCurrencyToDatabaseValue() {
        Currency.JpaConverter converter =
                new Currency.JpaConverter();

        assertEquals("HUF",
                converter.convertToDatabaseColumn(Currency.HUF));

        assertEquals("EUR",
                converter.convertToDatabaseColumn(Currency.EUR));

        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void converter_shouldConvertDatabaseValueToCurrency() {
        Currency.JpaConverter converter =
                new Currency.JpaConverter();

        assertEquals(Currency.HUF,
                converter.convertToEntityAttribute("HUF"));

        assertEquals(Currency.EUR,
                converter.convertToEntityAttribute("EUR"));

        assertEquals(Currency.HUF,
                converter.convertToEntityAttribute(null));
    }
}