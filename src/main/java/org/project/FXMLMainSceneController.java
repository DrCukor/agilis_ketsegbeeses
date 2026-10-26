package org.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.project.model.Debt;
import org.project.services.DebtService;

public class FXMLMainSceneController {

    private static final String SPLIT_EQUAL = "Egyenlően";
    private static final String SPLIT_FIXED = "Fix összegek (Ft)";
    private static final String SPLIT_PERCENTAGE = "Százalékos arány (%)";

    // A véglegesített 4 fős felhasználói lista[cite: 1]
    private final List<String> allUsers = List.of("Ádám", "Dávid", "Levente", "Zoltán");

    // UI elemek nyilvántartása a dinamikus generáláshoz
    private final Map<String, CheckBox> userCheckBoxes = new HashMap<>();
    private final Map<String, TextField> userValueFields = new HashMap<>();

    @FXML
    private TextField titleField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> payerComboBox;

    @FXML
    private ComboBox<String> splitTypeComboBox;

    @FXML
    private VBox participantsContainer;

    @FXML
    private Button saveButton;

    @FXML
    private HBox balancesContainer;

    @FXML
    private VBox recentExpensesBox;

    @FXML
    public void initialize() {
        // 1. "Ki fizette?" legördülő menü feltöltése a 4 fővel[cite: 1]
        payerComboBox.setItems(FXCollections.observableArrayList(allUsers));
        payerComboBox.setValue("Zoltán");

        // 2. Elosztási módok beállítása
        splitTypeComboBox.setItems(FXCollections.observableArrayList(SPLIT_EQUAL, SPLIT_FIXED, SPLIT_PERCENTAGE));
        splitTypeComboBox.setValue(SPLIT_EQUAL);

        // 3. Résztvevők sorainak dinamikus felépítése (CheckBox + TextField)
        buildParticipantsUI();

        // 4. Módváltás eseménykezelője a mezők láthatóságához
        splitTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFieldsVisibility());
    }

    /**
     * Dinamikusan legenerálja minden csapattagnak a CheckBox-ot és a mellette lévő értékmezőt.
     */
    private void buildParticipantsUI() {
        participantsContainer.getChildren().clear();
        userCheckBoxes.clear();
        userValueFields.clear();

        for (String user : allUsers) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            CheckBox checkBox = new CheckBox(user);
            checkBox.setSelected(true); // Alapértelmezetten mindenki ki van választva
            userCheckBoxes.put(user, checkBox);

            TextField valueField = new TextField();
            valueField.setPromptText("0");
            valueField.setPrefWidth(100);
            valueField.setVisible(false); // Alapértelmezetten rejtve ("Egyenlően" mód miatt)
            userValueFields.put(user, valueField);

            // Ha kiveszzük a pipát, letiltjuk és kiürítjük a hozzá tartozó mezőt
            checkBox.selectedProperty().addListener((obs, oldVal, isSelected) -> {
                valueField.setDisable(!isSelected);
                if (!isSelected) {
                    valueField.clear();
                }
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(checkBox, spacer, valueField);
            participantsContainer.getChildren().add(row);
        }
    }

    /**
     * Frissíti a beviteli mezők láthatóságát és prompt szövegeit a kiválasztott megosztási mód alapján.
     */
    private void updateFieldsVisibility() {
        String mode = splitTypeComboBox.getValue();
        boolean showFields = !SPLIT_EQUAL.equals(mode);

        for (String user : allUsers) {
            TextField field = userValueFields.get(user);
            field.setVisible(showFields);
            field.setManaged(showFields);

            if (SPLIT_FIXED.equals(mode)) {
                field.setPromptText("Ft");
            } else if (SPLIT_PERCENTAGE.equals(mode)) {
                field.setPromptText("%");
            }
        }
    }

    @FXML
    private void handleSaveExpense() {
        String title = titleField.getText();
        String amountText = amountField.getText();
        String payer = payerComboBox.getValue();
        String splitMode = splitTypeComboBox.getValue();

        // Egyszerű ellenőrzés, hogy az összeg ne legyen üres[cite: 1]
        if (amountText == null || amountText.isEmpty()) {
            System.out.println("Hiba: Az összeg mező nem lehet üres!");
            return;
        }

        try {
            double totalAmount = Double.parseDouble(amountText);
            if (totalAmount <= 0 || !Double.isFinite(totalAmount)) {
                throw new IllegalArgumentException("Az összeg érvénytelen.");[cite: 1]
            }

            // Kijelölt résztvevők kigyűjtése a CheckBox-ok alapján
            List<String> selectedUsers = allUsers.stream()
                    .filter(user -> userCheckBoxes.get(user).isSelected())
                    .collect(Collectors.toList());

            if (selectedUsers.isEmpty()) {
                throw new IllegalArgumentException("Legalább egy résztvevőt ki kell választani!");
            }

            // Részesedések kiszámítása és validálása mód szerint
            Map<String, Double> userShares = new HashMap<>();

            if (SPLIT_EQUAL.equals(splitMode)) {
                double share = totalAmount / selectedUsers.size();
                for (String user : selectedUsers) {
                    userShares.put(user, share);
                }
            } else if (SPLIT_FIXED.equals(splitMode)) {
                double currentSum = 0;
                for (String user : selectedUsers) {
                    String text = userValueFields.get(user).getText();
                    double val = (text == null || text.trim().isEmpty()) ? 0.0 : Double.parseDouble(text);
                    if (val < 0) {
                        throw new IllegalArgumentException("A megadott összeg nem lehet negatív!");
                    }
                    userShares.put(user, val);
                    currentSum += val;
                }

                if (Math.abs(currentSum - totalAmount) > 0.01) {
                    throw new IllegalArgumentException(
                            String.format("A megadott fix összegek összege (%.2f Ft) nem egyezik a teljes összeggel (%.2f Ft)!", currentSum, totalAmount)
                    );
                }
            } else if (SPLIT_PERCENTAGE.equals(splitMode)) {
                double totalPercent = 0;
                for (String user : selectedUsers) {
                    String text = userValueFields.get(user).getText();
                    double pct = (text == null || text.trim().isEmpty()) ? 0.0 : Double.parseDouble(text);
                    if (pct < 0) {
                        throw new IllegalArgumentException("A százalék nem lehet negatív!");
                    }
                    userShares.put(user, totalAmount * (pct / 100.0));
                    totalPercent += pct;
                }

                if (Math.abs(totalPercent - 100.0) > 0.01) {
                    throw new IllegalArgumentException(
                            String.format("A megadott százalékok összege (%.2f%%) nem 100%%!", totalPercent)
                    );
                }
            }

            // JPA EntityManager létrehozása[cite: 1]
            javax.persistence.EntityManagerFactory emf = javax.persistence.Persistence.createEntityManagerFactory("BudgetTrackerPU");
            javax.persistence.EntityManager em = emf.createEntityManager();

            org.project.model.Expense expense = new org.project.model.Expense(title, totalAmount, payer, selectedUsers);
            DebtService debtService = new DebtService();
            List<Debt> debts = debtService.calculateDebts(expense);

            try {
                em.getTransaction().begin();
                em.persist(expense);
                for (Debt debt : debts) {
                    em.persist(debt);
                }
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            } finally {
                if (em != null) em.close();
                if (emf != null) emf.close();
            }

            System.out.println("Sikeresen elmentve az adatbázisba: " + title + " - " + totalAmount + " Ft (" + payer + ")");[cite: 1]
            System.out.println("Sikeresen felosztva.");
            for (Debt debt : debts) {
                System.out.println(debt.getDebtor() + " tartozik neki: " + debt.getCreditor() + " ennyivel: " + debt.getAmount());[cite: 1]
            }

            // Mezők kiürítése és alaphelyzetbe állítása mentés után[cite: 1]
            titleField.clear();
            amountField.clear();
            buildParticipantsUI();

        } catch (NumberFormatException e) {
            System.out.println("Hiba: Az összeg vagy érték csak érvényes szám lehet!");[cite: 1]
        } catch (IllegalArgumentException e) {
            System.out.println("Hiba: " + e.getMessage());
        }
    }

    /**
     * Dinamikusan legenerál egy Egyenleg (Tartozás) kártyát a Styles.css osztályaival.[cite: 1]
     */
    public HBox createDebtCard(String subtitle, String amountText, boolean isGreen) {
        HBox card = new HBox(14.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add(isGreen ? "debt-card-green" : "debt-card-orange");
        HBox.setHgrow(card, Priority.ALWAYS);

        VBox textBox = new VBox(2.0);
        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("debt-card-subtitle");

        Label amountLabel = new Label(amountText);
        amountLabel.getStyleClass().add(isGreen ? "debt-amount-green" : "debt-amount-orange");

        textBox.getChildren().addAll(subLabel, amountLabel);
        card.getChildren().add(textBox);

        return card;
    }

    /**
     * Dinamikusan legenerál egy sort a Legutóbbi kiadások listába.[cite: 1]
     */
    public HBox createExpenseRow(String monogram, String title, String meta, String amountText, String colorTheme) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("expense-row");

        StackPane avatar = new StackPane();
        avatar.setPrefSize(40.0, 40.0);
        avatar.getStyleClass().add("avatar-badge-" + colorTheme);

        Label avatarText = new Label(monogram);
        avatarText.getStyleClass().add("avatar-text-" + colorTheme);
        avatar.getChildren().add(avatarText);

        VBox detailsBox = new VBox(2.0);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("expense-title");

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("expense-meta");
        detailsBox.getChildren().addAll(titleLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(amountText);
        amountLabel.getStyleClass().add("expense-amount");

        row.getChildren().addAll(avatar, detailsBox, spacer, amountLabel);
        return row;
    }
}