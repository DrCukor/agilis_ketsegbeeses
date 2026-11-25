package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.Currency;
import hu.unideb.inf.szoftverfejlesztes.model.ExpenseShare;
import hu.unideb.inf.szoftverfejlesztes.model.SplitType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

// Egy kiadás résztvevőnkénti részeinek kiszámítása a megosztási mód szerint.
// Tiszta logika, nincs adatbázis-hozzáférés.
public final class ShareCalculator {

    private static final double PERCENT_TOLERANCE = 0.01;

    private ShareCalculator() {
    }

    /**
     * @param type         megosztási mód
     * @param participants a résztvevők (ismétlődés nélkül, legalább egy)
     * @param values       résztvevőnként a megadott érték: százalék / összeg / részarány (egyenlőnél nem kell)
     * @param total        a kiadás teljes összege
     * @param currency     pénznem (ennek tizedesjegyeire kerekítünk)
     * @return a részek; a 0 részű résztvevők kimaradnak. A részek összege pontosan a teljes összeg.
     */
    public static List<ExpenseShare> calculate(SplitType type, List<String> participants, Map<String, Double> values,
                                               double total, Currency currency) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Válassz ki legalább egy résztvevőt!");
        }
        int decimals = currency.getDecimals();
        double[] inputs = new double[participants.size()];
        if (type != SplitType.EQUAL) {
            for (int i = 0; i < participants.size(); i++) {
                Double value = values == null ? null : values.get(participants.get(i));
                if (value == null || value.isNaN() || value < 0) {
                    throw new IllegalArgumentException("Adj meg nem negatív értéket minden résztvevőnek (" + participants.get(i) + ")!");
                }
                inputs[i] = value;
            }
        }

        double[] amounts = switch (type) {
            case EQUAL -> distribute(total, ones(participants.size()), decimals);
            case PERCENT -> {
                double sum = sum(inputs);
                if (Math.abs(sum - 100) > PERCENT_TOLERANCE) {
                    throw new IllegalArgumentException("A százalékok összegének 100%-nak kell lennie (most: "
                            + format(sum, 2) + "%)!");
                }
                yield distribute(total, inputs, decimals);
            }
            case SHARES -> {
                if (sum(inputs) <= 0) {
                    throw new IllegalArgumentException("Legalább egy résztvevőnek pozitív részarányt adj meg!");
                }
                yield distribute(total, inputs, decimals);
            }
            case AMOUNT -> {
                double sum = sum(inputs);
                if (Math.abs(sum - total) > currency.getEpsilon()) {
                    throw new IllegalArgumentException("A részek összege (" + format(sum, decimals) + ") nem egyezik a kiadás összegével ("
                            + format(total, decimals) + ")!");
                }
                // Megadott összegek: a tizedesjegyekre kerekítve, az esetleges maradék a legnagyobb részhez kerül
                yield distribute(total, inputs, decimals);
            }
        };

        List<ExpenseShare> shares = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            if (amounts[i] > 0) {
                Double inputValue = type == SplitType.EQUAL ? null : inputs[i];
                shares.add(new ExpenseShare(participants.get(i), inputValue, amounts[i]));
            }
        }
        if (shares.isEmpty()) {
            throw new IllegalArgumentException("Legalább egy résztvevőnek pozitív részt kell kapnia!");
        }
        return shares;
    }

    // A teljes összeg felosztása a súlyok arányában, a pénznem legkisebb egységére kerekítve
    // (legnagyobb maradék módszer): a részek összege mindig pontosan a teljes összeg.
    static double[] distribute(double total, double[] weights, int decimals) {
        double scale = Math.pow(10, decimals);
        long totalUnits = Math.round(total * scale);
        double weightSum = sum(weights);

        long[] units = new long[weights.length];
        double[] remainders = new double[weights.length];
        long assigned = 0;
        for (int i = 0; i < weights.length; i++) {
            double exact = weightSum == 0 ? 0 : totalUnits * weights[i] / weightSum;
            units[i] = (long) Math.floor(exact);
            remainders[i] = exact - units[i];
            assigned += units[i];
        }

        // A kerekítésből megmaradt egységeket a legnagyobb törtrészűek kapják
        long leftover = totalUnits - assigned;
        List<Integer> order = IntStream.range(0, weights.length).boxed()
                .filter(i -> weights[i] > 0)
                .sorted(Comparator.comparingDouble((Integer i) -> remainders[i]).reversed())
                .toList();
        for (int k = 0; k < leftover && !order.isEmpty(); k++) {
            units[order.get(k % order.size())]++;
        }

        double[] result = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            result[i] = units[i] / scale;
        }
        return result;
    }

    // Összeg kerekítése a pénznem legkisebb egységére (pl. egész forint, cent)
    public static double round(double value, Currency currency) {
        double scale = Math.pow(10, currency.getDecimals());
        return Math.round(value * scale) / scale;
    }

    private static double[] ones(int n) {
        double[] result = new double[n];
        java.util.Arrays.fill(result, 1);
        return result;
    }

    private static double sum(double[] values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum;
    }

    private static String format(double value, int decimals) {
        return String.format(java.util.Locale.forLanguageTag("hu-HU"), "%." + decimals + "f", value);
    }
}
