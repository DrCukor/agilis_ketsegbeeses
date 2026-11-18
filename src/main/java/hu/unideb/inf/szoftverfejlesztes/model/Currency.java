package hu.unideb.inf.szoftverfejlesztes.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// Pénznem. A tartozásokat pénznemenként külön vezetjük, nincs átváltás közöttük.
public enum Currency {
    HUF(0.5),   // fél forint alatti maradék rendezettnek számít
    EUR(0.005), // fél cent alatti maradék rendezettnek számít
    USD(0.005),
    GBP(0.005);

    private final double epsilon;

    Currency(double epsilon) {
        this.epsilon = epsilon;
    }

    public double getEpsilon() {
        return epsilon;
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
