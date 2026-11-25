package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// Pénznem. A tartozásokat pénznemenként külön vezetjük, nincs átváltás közöttük.
public enum Currency {
    HUF(0.5, 0),   // fél forint alatti maradék rendezettnek számít; egész forintra kerekítünk
    EUR(0.005, 2), // fél cent alatti maradék rendezettnek számít; centre kerekítünk
    USD(0.005, 2),
    GBP(0.005, 2);

    private final double epsilon;
    // Hány tizedesjegyre kerekítjük a megosztott részeket
    private final int decimals;

    Currency(double epsilon, int decimals) {
        this.epsilon = epsilon;
        this.decimals = decimals;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public int getDecimals() {
        return decimals;
    }

    // Üres értéknél forint; ismeretlen értéknél hibát dob
    public static Currency parse(String value) {
        if (value == null || value.isBlank()) {
            return HUF;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ismeretlen pénznem: " + value);
        }
    }

    // Egyszerű szöveges oszlopként tároljuk; a régi (üres) sorok forintot jelentenek
    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<Currency, String> {
        @Override
        public String convertToDatabaseColumn(Currency currency) {
            return currency == null ? null : currency.name();
        }

        @Override
        public Currency convertToEntityAttribute(String value) {
            return value == null ? HUF : Currency.valueOf(value);
        }
    }
}
