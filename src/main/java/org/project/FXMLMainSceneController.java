package org.project;

import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class FXMLMainSceneController {

    private static final String MODE_EVERYONE = "Mindenki egyenlően (4 fő)";
    private static final String MODE_TWO_USERS = "Két személy között (2 fő)";

    // A véglegesített 4 fős felhasználói lista
    private final List<String> allUsers = List.of("Ádám", "Dávid", "Levente", "Zoltán");
    private boolean isUpdatingCombos = false;

    @FXML
    private TextField titleField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> payerComboBox;

    @FXML
    private ComboBox<String> splitModeComboBox;

    @FXML
    private HBox customSplitBox;

    @FXML
    private ComboBox<String> splitPerson1ComboBox;

    @FXML
    private ComboBox<String> splitPerson2ComboBox;

    @FXML
    private Button saveButton;

    @FXML
    private HBox balancesContainer;

    @FXML
    private VBox recentExpensesBox;

    @FXML
    private void handleSaveExpense() {
        // 1. Kiolvesszük az adatokat a felületről
        String title = titleField.getText();
        String amountText = amountField.getText();
        String payer = payerComboBox.getValue();

        // Egyszerű ellenőrzés, hogy az összeg ne legyen üres
        if (amountText == null || amountText.trim().isEmpty()) {
            System.out.println("Hiba: Az összeg mező nem lehet üres!");
            return;
        }

        try {
            // Vessző cseréje pontra, hogy a Double.parseDouble ne dobjon hibát magyar formátumnál
            double amount = Double.parseDouble(amountText.trim().replace(',', '.'));

            // 2. JPA EntityManager létrehozása
            javax.persistence.EntityManagerFactory emf = javax.persistence.Persistence.createEntityManagerFactory("BudgetTrackerPU");
            javax.persistence.EntityManager em = emf.createEntityManager();

            // 3. Adatbázis tranzakció indítása és mentés
            em.getTransaction().begin();

            org.project.model.Expense expense = new org.project.model.Expense(title, amount, payer);
            em.persist(expense);

            em.getTransaction().commit();

            // 4. Erőforrások lezárása
            em.close();
            emf.close();

            System.out.println("Sikeresen elmentve az adatbázisba: " + title + " - " + amount + " Ft (" + payer + ")");

            // Mezők kiürítése és lista újratöltése
            titleField.clear();
            amountField.clear();
            loadExpenses();

        } catch (NumberFormatException e) {
            System.out.println("Hiba: Az összeg csak érvényes szám lehet!");
        }
    }

    @FXML
    private void loadExpenses() {
        System.out.println("--- loadExpenses() elindult ---");

        if (recentExpensesBox == null) {
            System.out.println("HIBA: a recentExpensesBox értéke NULL! (Nincs összekötve az FXML-lel)");
            return;
        }

        recentExpensesBox.getChildren().clear();

        javax.persistence.EntityManagerFactory emf = javax.persistence.Persistence.createEntityManagerFactory("BudgetTrackerPU");
        javax.persistence.EntityManager em = emf.createEntityManager();

        try {
            java.util.List<org.project.model.Expense> expenses = em.createQuery("SELECT e FROM Expense e", org.project.model.Expense.class).getResultList();
            System.out.println("Adatbázisból betöltve: " + expenses.size() + " db kiadás.");

            for (org.project.model.Expense expense : expenses) {
                String displayText = expense.getTitle() + " - " + expense.getAmount() + " Ft (" + expense.getPayer() + ")";

                javafx.scene.control.Label expenseLabel = new javafx.scene.control.Label(displayText);
                expenseLabel.setStyle("-fx-text-fill: black; -fx-font-size: 14px; -fx-padding: 4px;");

                recentExpensesBox.getChildren().add(expenseLabel);
            }
        } catch (Exception e) {
            System.out.println("Hiba a lekérdezésnél: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    @FXML
    public void initialize() {

        // 1. "Ki fizette?" legördülő menü feltöltése a 4 fővel
        payerComboBox.setItems(FXCollections.observableArrayList(allUsers));
        payerComboBox.setValue("Zoltán");

        // 2. Megosztás módja választó ("Mindenki (4 fő)" vs "2 lenyitható fül")
        splitModeComboBox.setItems(FXCollections.observableArrayList(MODE_EVERYONE, MODE_TWO_USERS));
        splitModeComboBox.getSelectionModel().selectFirst();

        splitModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCustom = MODE_TWO_USERS.equals(newVal);
            customSplitBox.setVisible(isCustom);
            customSplitBox.setManaged(isCustom);
        });

        // 3. Kétfős megosztás legördülő menük inicializálása és kölcsönös kizárása
        setupMutuallyExclusiveComboBoxes();

        // 4. Számok és tizedesjegyek szűrése az összeg mezőben
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*([.,]\\d*)?")) {
                return change;
            }
            return null;
        };
        amountField.setTextFormatter(new javafx.scene.control.TextFormatter<>(filter));

        // 5. Kiadások betöltése az adatbázisból induláskor

        loadExpenses(); // Frissíti a listát mentés után
    }

    private void setupMutuallyExclusiveComboBoxes() {
        // Kezdeti állapot: 1. fülön Ádám, 2. fülön Dávid
        String initialUser1 = allUsers.get(0);
        String initialUser2 = allUsers.get(1);

        updateComboItems(splitPerson1ComboBox, initialUser2, initialUser1);
        updateComboItems(splitPerson2ComboBox, initialUser1, initialUser2);

        // Ha az 1. fülön változik a kiválasztott személy -> frissítjük a 2. fület (csak a másik 3 fő marad benne)
        splitPerson1ComboBox.valueProperty().addListener((obs, oldVal, newSelected1) -> {
            if (isUpdatingCombos || newSelected1 == null) {
                return;
            }
            isUpdatingCombos = true;
            String currentSelected2 = splitPerson2ComboBox.getValue();
            updateComboItems(splitPerson2ComboBox, newSelected1, currentSelected2);
            updateComboItems(splitPerson1ComboBox, splitPerson2ComboBox.getValue(), newSelected1);
            isUpdatingCombos = false;
        });

        // Ha a 2. fülön változik a kiválasztott személy -> frissítjük az 1. fület (csak a másik 3 fő marad benne)
        splitPerson2ComboBox.valueProperty().addListener((obs, oldVal, newSelected2) -> {
            if (isUpdatingCombos || newSelected2 == null) {
                return;
            }
            isUpdatingCombos = true;
            String currentSelected1 = splitPerson1ComboBox.getValue();
            updateComboItems(splitPerson1ComboBox, newSelected2, currentSelected1);
            updateComboItems(splitPerson2ComboBox, splitPerson1ComboBox.getValue(), newSelected2);
            isUpdatingCombos = false;
        });
    }

    /**
     * Frissíti a megadott ComboBox elemeit úgy, hogy kizárja a másik fülön kiválasztott személyt.
     */
    private void updateComboItems(ComboBox<String> targetCombo, String excludedUser, String preferredSelection) {
        List<String> availableUsers = allUsers.stream()
                .filter(user -> !user.equals(excludedUser))
                .collect(Collectors.toList());

        targetCombo.setItems(FXCollections.observableArrayList(availableUsers));

        if (preferredSelection != null && availableUsers.contains(preferredSelection)) {
            targetCombo.setValue(preferredSelection);
        } else if (!availableUsers.isEmpty()) {
            targetCombo.setValue(availableUsers.get(0));
        }
    }

    /**
     * Dinamikusan legenerál egy Egyenleg (Tartozás) kártyát a Styles.css osztályaival.
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
     * Dinamikusan legenerál egy sort a Legutóbbi kiadások listába.
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