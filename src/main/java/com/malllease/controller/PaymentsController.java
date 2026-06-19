package com.malllease.controller;

import com.malllease.dao.PaymentDao;
import com.malllease.model.ContractPaymentView;
import com.malllease.model.Payment;
import com.malllease.model.User;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PaymentsController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat MONEY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"));

    @FXML private TextField searchField;
    @FXML private ComboBox<PaymentFilter> paymentFilterBox;
    @FXML private VBox statAll;
    @FXML private VBox statDue;
    @FXML private VBox statDueAmount;
    @FXML private VBox statPaid;
    @FXML private Label contractsCountLabel;
    @FXML private Label dueCountLabel;
    @FXML private Label dueAmountLabel;
    @FXML private Label paidAmountLabel;
    @FXML private Label listCountLabel;
    @FXML private VBox paymentListBox;
    @FXML private Label detailSubtitleLabel;
    @FXML private Label detailStatusBadge;
    @FXML private Label detailEmptyLabel;
    @FXML private VBox detailContentBox;
    @FXML private Label contractNoLabel;
    @FXML private Label companyNameLabel;
    @FXML private Label pointCodeLabel;
    @FXML private Label rentalPeriodLabel;
    @FXML private Label dailyRateLabel;
    @FXML private Label expectedTotalLabel;
    @FXML private Label paidTotalLabel;
    @FXML private Label remainingTotalLabel;
    @FXML private Label paidPctLabel;
    @FXML private javafx.scene.control.ProgressBar paymentProgress;
    @FXML private Label nextPeriodLabel;
    @FXML private Label currentDueLabel;
    @FXML private Label paymentHintLabel;
    @FXML private Label actionStatusLabel;
    @FXML private Button payButton;
    @FXML private Button openOnMapButton;
    @FXML private TextField payAmountField;
    @FXML private VBox paymentHistoryBox;

    private final PaymentDao paymentDao = new PaymentDao();
    private final com.malllease.service.PaymentService paymentService = new com.malllease.service.PaymentService(paymentDao);
    private java.util.function.IntConsumer mapNavigator = id -> {};

    public void setMapNavigator(java.util.function.IntConsumer nav) {
        this.mapNavigator = nav == null ? id -> {} : nav;
    }
    private final List<ContractPaymentView> allRows = new ArrayList<>();

    private User currentUser;
    private ContractPaymentView selectedRow;
    private boolean clientMode;

    @FXML
    private void initialize() {
        paymentFilterBox.setItems(FXCollections.observableArrayList(
                new PaymentFilter("all", "Все договоры"),
                new PaymentFilter("due", "К оплате"),
                new PaymentFilter("paid", "Оплачено")
        ));
        paymentFilterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentFilter filter) {
                return filter == null ? "" : filter.label();
            }

            @Override
            public PaymentFilter fromString(String value) {
                return null;
            }
        });
        paymentFilterBox.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderList());
        paymentFilterBox.valueProperty().addListener((obs, oldValue, newValue) -> { renderList(); updateStatCardHighlights(); });
        hideDetails();
    }

    public void initUser(User user) {
        this.currentUser = user;
        this.clientMode = "client".equals(user.getRole().getCode());
        payButton.setText(clientMode ? "Оплатить" : "Зарегистрировать платёж");
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        paymentFilterBox.getSelectionModel().selectFirst();
        updateStatCardHighlights();
    }

    @FXML
    private void handleStatAll() {
        paymentFilterBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleStatDue() {
        boolean alreadyActive = isFilterActive("due");
        if (alreadyActive) {
            paymentFilterBox.getSelectionModel().selectFirst();
        } else {
            paymentFilterBox.getItems().stream()
                    .filter(f -> "due".equals(f.value()))
                    .findFirst().ifPresent(paymentFilterBox::setValue);
        }
    }

    @FXML
    private void handleStatPaid() {
        boolean alreadyActive = isFilterActive("paid");
        if (alreadyActive) {
            paymentFilterBox.getSelectionModel().selectFirst();
        } else {
            paymentFilterBox.getItems().stream()
                    .filter(f -> "paid".equals(f.value()))
                    .findFirst().ifPresent(paymentFilterBox::setValue);
        }
    }

    private boolean isFilterActive(String value) {
        PaymentFilter current = paymentFilterBox.getValue();
        return current != null && value.equals(current.value());
    }

    private void updateStatCardHighlights() {
        PaymentFilter current = paymentFilterBox.getValue();
        String active = current == null ? "all" : current.value();
        setStatCardActive(statAll, "all".equals(active));
        setStatCardActive(statDue, "due".equals(active));
        setStatCardActive(statDueAmount, "due".equals(active));
        setStatCardActive(statPaid, "paid".equals(active));
    }

    private void setStatCardActive(VBox card, boolean active) {
        if (card == null) return;
        card.getStyleClass().remove("payment-stat-card-active");
        if (active) card.getStyleClass().add("payment-stat-card-active");
    }

    @FXML
    private void handleOpenOnMap() {
        if (selectedRow != null) {
            mapNavigator.accept(selectedRow.getPointId());
        }
    }

    @FXML
    private void handleFillFullAmount() {
        if (selectedRow != null && payAmountField != null) {
            payAmountField.setText(selectedRow.getAmountDue().toPlainString());
        }
    }

    @FXML
    private void handlePay() {
        if (selectedRow == null || !selectedRow.isPayable()) {
            setActionStatus("По выбранному договору сейчас нечего оплачивать", false);
            return;
        }

        BigDecimal amount;
        try {
            amount = parsePartialAmount();
        } catch (IllegalArgumentException ex) {
            setActionStatus(ex.getMessage(), false);
            return;
        }
        boolean partial = amount.compareTo(selectedRow.getAmountDue()) < 0;

        payButton.setDisable(true);
        String idleText = clientMode ? "Оплатить" : "Зарегистрировать платеж";
        payButton.setText(clientMode ? "Оплачиваем..." : "Регистрируем...");
        setActionStatus((partial ? "Частичный платёж " : "Платёж ")
                + formatMoney(amount) + " за "
                + formatPeriod(selectedRow.getNextPeriodFrom(), selectedRow.getNextPeriodTo()), true);

        int contractId = selectedRow.getContractId();
        int pointId = selectedRow.getPointId();
        try {
            String comment = (clientMode
                    ? "Оплата клиентом через личный кабинет"
                    : "Платеж зарегистрирован менеджером")
                    + (partial ? " (частичный)" : "");
            paymentService.registerPayment(selectedRow, comment, amount);
            if (payAmountField != null) payAmountField.clear();
            refresh();
            selectRowByKey(contractId, pointId);
            setActionStatus(partial
                    ? "Частичный платёж записан. Остаток обновлён."
                    : "Платёж проведён. Следующий период пересчитан.", true);
        } catch (Exception e) {
            setActionStatus("Не удалось провести платеж: " + rootMessage(e), false);
        } finally {
            payButton.setText(idleText);
            updatePayButton();
        }
    }

    private BigDecimal parsePartialAmount() {
        String raw = payAmountField == null ? "" : payAmountField.getText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Укажите сумму платежа или нажмите «Вся сумма»");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(raw.trim().replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Введите сумму платежа числом");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма платежа должна быть больше нуля");
        }
        if (amount.compareTo(selectedRow.getAmountDue()) > 0) {
            throw new IllegalArgumentException(
                    "Сумма превышает остаток за период (" + formatMoney(selectedRow.getAmountDue()) + ")");
        }
        return amount;
    }

    private void refresh() {
        try {
            allRows.clear();
            if (clientMode) {
                allRows.addAll(paymentDao.findContractPaymentViewsForClientUser(currentUser.getUserId()));
            } else {
                allRows.addAll(paymentDao.findContractPaymentViewsForWorkplace());
            }
            renderStats();
            renderList();
            if (selectedRow == null) {
                filteredRows().stream().findFirst().ifPresent(this::selectRow);
            }
        } catch (Exception e) {
            paymentListBox.getChildren().setAll(emptyLabel("Не удалось загрузить платежи. Проверьте подключение к БД."));
            contractsCountLabel.setText("0");
            dueCountLabel.setText("0");
            dueAmountLabel.setText(formatMoney(BigDecimal.ZERO));
            paidAmountLabel.setText(formatMoney(BigDecimal.ZERO));
            listCountLabel.setText("0");
            hideDetails();
        }
    }

    private void renderStats() {
        BigDecimal dueTotal = allRows.stream()
                .map(ContractPaymentView::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidTotal = allRows.stream()
                .map(ContractPaymentView::getPaidTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long dueCount = allRows.stream().filter(ContractPaymentView::isPayable).count();

        contractsCountLabel.setText(Integer.toString(allRows.size()));
        dueCountLabel.setText(Long.toString(dueCount));
        dueAmountLabel.setText(formatMoney(dueTotal));
        paidAmountLabel.setText(formatMoney(paidTotal));
    }

    private void renderList() {
        paymentListBox.getChildren().clear();
        List<ContractPaymentView> rows = filteredRows();
        listCountLabel.setText(Integer.toString(rows.size()));

        if (rows.isEmpty()) {
            paymentListBox.getChildren().add(emptyLabel("По текущим фильтрам платежей нет."));
            hideDetails();
            return;
        }

        for (ContractPaymentView row : rows) {
            paymentListBox.getChildren().add(createPaymentCard(row));
        }
    }

    private List<ContractPaymentView> filteredRows() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        PaymentFilter filter = paymentFilterBox.getValue();

        return allRows.stream()
                .filter(row -> filter == null || "all".equals(filter.value())
                        || ("due".equals(filter.value()) && row.isPayable())
                        || ("paid".equals(filter.value()) && !row.isPayable()))
                .filter(row -> query.isBlank() || searchableText(row).contains(query))
                .sorted(Comparator.comparing(ContractPaymentView::isPayable).reversed()
                        .thenComparing(ContractPaymentView::getCompanyName)
                        .thenComparing(ContractPaymentView::getContractNo)
                        .thenComparing(ContractPaymentView::getPointCode))
                .toList();
    }

    private String searchableText(ContractPaymentView row) {
        return String.join(" ",
                safe(row.getCompanyName()),
                safe(row.getContractNo()),
                safe(row.getPointCode()))
                .toLowerCase(Locale.ROOT);
    }

    private Button createPaymentCard(ContractPaymentView row) {
        VBox content = new VBox(6);
        HBox top = new HBox(8);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(row.getContractNo() + " · " + row.getPointCode());
        title.getStyleClass().add("payment-card-title");
        Label status = new Label(statusBadgeText(row));
        status.getStyleClass().addAll("payment-card-status", statusBadgeClass(row));

        top.getChildren().addAll(title, new Region(), status);
        HBox.setHgrow(top.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        Label company = new Label(row.getCompanyName());
        company.getStyleClass().add("payment-card-company");

        Label amount = new Label(row.isPayable()
                ? formatMoney(row.getAmountDue()) + " · " + formatPeriod(row.getNextPeriodFrom(), row.getNextPeriodTo())
                : "Оплачено: " + formatMoney(row.getPaidTotal()));
        amount.getStyleClass().add("payment-card-meta");

        content.getChildren().addAll(top, company, amount);

        Button card = new Button();
        card.setMaxWidth(Double.MAX_VALUE);
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.getStyleClass().add("payment-card");
        if (selectedRow != null
                && selectedRow.getContractId() == row.getContractId()
                && selectedRow.getPointId() == row.getPointId()) {
            card.getStyleClass().add("payment-card-active");
        }
        card.setOnAction(event -> selectRow(row));
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);
        return card;
    }

    private void selectRow(ContractPaymentView row) {
        selectedRow = row;
        clearActionStatus();

        detailSubtitleLabel.setText(row.getCompanyName());
        detailStatusBadge.setText(statusBadgeText(row));
        detailStatusBadge.getStyleClass().removeAll("payment-status-due", "payment-status-paid", "payment-status-overdue");
        detailStatusBadge.getStyleClass().add(statusBadgeClass(row));

        contractNoLabel.setText(row.getContractNo());
        companyNameLabel.setText(row.getCompanyName());
        pointCodeLabel.setText(row.getPointCode());
        rentalPeriodLabel.setText(formatPeriod(row.getRentalFrom(), row.getRentalTo()) + " · " + daysText(row.getRentalFrom(), row.getRentalTo()));
        dailyRateLabel.setText(formatMoney(row.getDailyRate()) + " в день");

        BigDecimal expected = expectedTotal(row);
        BigDecimal paid = row.getPaidTotal() == null ? BigDecimal.ZERO : row.getPaidTotal();
        BigDecimal remaining = expected.subtract(paid).max(BigDecimal.ZERO);
        double pct = expected.signum() > 0
                ? Math.min(1.0, paid.doubleValue() / expected.doubleValue()) : 0.0;

        expectedTotalLabel.setText(formatMoney(expected));
        paidTotalLabel.setText(formatMoney(paid));
        remainingTotalLabel.setText(formatMoney(remaining));
        paidPctLabel.setText(Math.round(pct * 100) + "%");
        paymentProgress.setProgress(pct);

        nextPeriodLabel.setText(row.isPayable() ? formatPeriod(row.getNextPeriodFrom(), row.getNextPeriodTo())
                : row.hasCharges() ? "Всё оплачено" : "—");
        currentDueLabel.setText(formatMoney(row.getAmountDue()));
        if (isOverdue(row)) {
            paymentHintLabel.setText("⚠ Платёж за этот период просрочен — месяц уже закрыт. "
                    + "Внесите всю сумму или часть, остаток пересчитается.");
        } else if (!row.hasCharges()) {
            paymentHintLabel.setText("По этой аренде начисления не сгенерированы. "
                    + "Обратитесь к администратору.");
        } else {
            paymentHintLabel.setText(row.isPayable()
                    ? "Это ближайший месяц к оплате. Можно внести всю сумму или часть — остаток пересчитается."
                    : "Все месяцы по этой точке оплачены — задолженности нет.");
        }

        renderHistory(row);
        detailEmptyLabel.setVisible(false);
        detailEmptyLabel.setManaged(false);
        detailContentBox.setVisible(true);
        detailContentBox.setManaged(true);
        updatePayButton();
        renderList();
    }

    private void selectRowByKey(int contractId, int pointId) {
        allRows.stream()
                .filter(row -> row.getContractId() == contractId && row.getPointId() == pointId)
                .findFirst()
                .ifPresentOrElse(this::selectRow, this::hideDetails);
    }

    private void renderHistory(ContractPaymentView row) {
        paymentHistoryBox.getChildren().clear();
        List<Payment> payments = paymentDao.findByRental(row.getContractId(), row.getPointId());
        if (payments.isEmpty()) {
            paymentHistoryBox.getChildren().add(emptyLabel("Платежей по этой точке пока нет."));
            return;
        }

        for (Payment payment : payments.stream().limit(5).toList()) {
            VBox item = new VBox(4);
            item.getStyleClass().add("payment-history-item");
            String monthText = payment.getChargeMonth() == null
                    ? "" : " · " + formatMonth(payment.getChargeMonth());
            Label title = new Label(formatMoney(payment.getAmount()) + monthText);
            title.getStyleClass().add("payment-history-title");
            Label meta = new Label(formatDate(payment.getPaidAt()) + " · " + safe(payment.getDocumentNo()));
            meta.getStyleClass().add("text-secondary");
            item.getChildren().addAll(title, meta);
            item.setCache(true);
            item.setCacheHint(javafx.scene.CacheHint.SPEED);
            paymentHistoryBox.getChildren().add(item);
        }
    }

    private void updatePayButton() {
        if (payButton == null) {
            return;
        }
        payButton.setDisable(selectedRow == null || !selectedRow.isPayable());
    }

    private void hideDetails() {
        selectedRow = null;
        detailSubtitleLabel.setText("Выберите договор слева");
        detailStatusBadge.setText("—");
        detailStatusBadge.getStyleClass().removeAll("payment-status-due", "payment-status-paid", "payment-status-overdue");
        detailEmptyLabel.setVisible(true);
        detailEmptyLabel.setManaged(true);
        detailContentBox.setVisible(false);
        detailContentBox.setManaged(false);
        clearActionStatus();
        if (payButton != null) {
            payButton.setDisable(true);
        }
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("point-list-empty");
        label.setWrapText(true);
        return label;
    }

    private boolean isOverdue(ContractPaymentView row) {
        return row.isPayable()
                && row.getNextPeriodFrom() != null
                && YearMonth.from(row.getNextPeriodFrom()).isBefore(YearMonth.now());
    }

    private String statusBadgeText(ContractPaymentView row) {
        if (isOverdue(row)) return "Просрочено";
        if (row.isPayable()) return "К оплате";
        return row.hasCharges() ? "Оплачено" : "Нет начислений";
    }

    private String statusBadgeClass(ContractPaymentView row) {
        if (isOverdue(row)) return "payment-status-overdue";
        if (row.isPayable()) return "payment-status-due";
        return row.hasCharges() ? "payment-status-paid" : "payment-status-no-charges";
    }

    private BigDecimal expectedTotal(ContractPaymentView row) {
        if (row.getDailyRate() == null || row.getRentalFrom() == null || row.getRentalTo() == null) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(row.getRentalFrom(), row.getRentalTo()) + 1;
        return row.getDailyRate().multiply(BigDecimal.valueOf(days));
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMATTER.format(value == null ? BigDecimal.ZERO : value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMATTER);
    }

    private String formatMonth(LocalDate month) {
        return month == null ? "—" : month.format(DATE_FORMATTER);
    }

    private String formatPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return "—";
        }
        return formatDate(from) + " - " + formatDate(to);
    }

    private String daysText(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return "0 дней";
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return days + " дн.";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setActionStatus(String message, boolean success) {
        if (message == null || message.isBlank()) {
            clearActionStatus();
            return;
        }
        actionStatusLabel.setText(message);
        actionStatusLabel.setVisible(true);
        actionStatusLabel.setManaged(true);
        actionStatusLabel.getStyleClass().removeAll("profile-status-success", "profile-status-error");
        actionStatusLabel.getStyleClass().add(success ? "profile-status-success" : "profile-status-error");
    }

    private void clearActionStatus() {
        if (actionStatusLabel == null) {
            return;
        }
        actionStatusLabel.setText("");
        actionStatusLabel.setVisible(false);
        actionStatusLabel.setManaged(false);
        actionStatusLabel.getStyleClass().removeAll("profile-status-success", "profile-status-error");
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record PaymentFilter(String value, String label) {}
}
