package com.malllease.controller;

import com.malllease.dao.ContractDao;
import com.malllease.model.ClientContractCard;
import com.malllease.model.User;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ClientContractsController {

    private static final Logger log = LoggerFactory.getLogger(ClientContractsController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.of("ru", "RU"));
    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.of("ru", "RU"));

    @FXML
    private Label activeCountLabel;
    @FXML
    private Label pointCountLabel;
    @FXML
    private Label paidTotalLabel;
    @FXML
    private Label expectedTotalLabel;
    @FXML
    private VBox contractsBox;
    @FXML
    private Label emptyLabel;

    private java.util.function.IntConsumer mapNavigator = id -> {
    };

    public void setMapNavigator(java.util.function.IntConsumer navigator) {
        this.mapNavigator = navigator == null ? id -> {
        } : navigator;
    }

    private final ContractDao contractDao = new ContractDao();

    public void initUser(User user) {
        if (user == null) {
            showEmpty();
            return;
        }
        try {
            List<ClientContractCard> cards = contractDao.findCardsByClientUser(user.getUserId());
            render(cards);
        } catch (Exception e) {
            log.error("Failed to load client contracts for user {}", user.getUserId(), e);
            showError("Не удалось загрузить договоры. Проверьте подключение к БД.");
        }
    }

    private void render(List<ClientContractCard> cards) {
        contractsBox.getChildren().clear();
        if (cards.isEmpty()) {
            showEmpty();
            updateSummary(cards);
            return;
        }
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);
        for (ClientContractCard card : cards) {
            contractsBox.getChildren().add(buildCard(card));
        }
        updateSummary(cards);
    }

    private void updateSummary(List<ClientContractCard> cards) {
        long active = cards.stream().filter(c -> "active".equals(c.getStatus())).count();
        long pointCount = cards.stream().mapToInt(c -> c.getRentals().size()).sum();
        BigDecimal paid = cards.stream().map(ClientContractCard::totalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = cards.stream().map(ClientContractCard::totalExpected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        activeCountLabel.setText(Long.toString(active));
        pointCountLabel.setText(Long.toString(pointCount));
        paidTotalLabel.setText(money(paid));
        expectedTotalLabel.setText(money(expected));
    }

    private VBox buildCard(ClientContractCard card) {
        VBox box = new VBox(12);
        box.getStyleClass().addAll("contract-card", "contract-card-" + safeStatus(card.getStatus()));
        box.setCache(true);
        box.setCacheHint(javafx.scene.CacheHint.SPEED);

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label no = new Label(card.getContractNo());
        no.getStyleClass().add("contract-no");
        Label status = new Label(statusText(card.getStatus()));
        status.getStyleClass().addAll("contract-status-badge", "contract-status-" + safeStatus(card.getStatus()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label signed = new Label("Подписан " + (card.getSignedAt() == null ? "—" : DATE_FMT.format(card.getSignedAt())));
        signed.getStyleClass().add("text-secondary");
        header.getChildren().addAll(no, status, spacer, signed);

        box.getChildren().add(header);
        if (card.getComment() != null && !card.getComment().isBlank()) {
            Label comment = new Label(card.getComment());
            comment.setWrapText(true);
            comment.getStyleClass().add("contract-comment");
            box.getChildren().add(comment);
        }

        VBox rentalsBox = new VBox(8);
        rentalsBox.getStyleClass().add("contract-rentals");
        for (ClientContractCard.Rental r : card.getRentals()) {
            rentalsBox.getChildren().add(buildRentalRow(r));
        }
        box.getChildren().add(rentalsBox);

        HBox totals = new HBox(20);
        totals.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        totals.getStyleClass().add("contract-totals");
        totals.getChildren().addAll(
                metric("Оплачено", money(card.totalPaid())),
                metric("К оплате всего", money(card.totalExpected())),
                metric("Точек по договору", Integer.toString(card.getRentals().size())));
        box.getChildren().add(totals);
        return box;
    }

    private VBox buildRentalRow(ClientContractCard.Rental r) {
        VBox container = new VBox(10);
        container.getStyleClass().add("contract-rental-row");

        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label code = new Label(r.pointCode);
        code.getStyleClass().add("rental-code");
        Label where = new Label(r.shoppingCenterName + " · этаж " + r.floor);
        where.getStyleClass().add("text-secondary");

        VBox left = new VBox(2);
        left.getChildren().addAll(code, where);

        Region grow = new Region();
        HBox.setHgrow(grow, javafx.scene.layout.Priority.ALWAYS);

        Label period = new Label(DATE_FMT.format(r.dateFrom) + " → " + DATE_FMT.format(r.dateTo));
        period.getStyleClass().add("rental-period");

        Label rate = new Label(money(r.dailyRate) + " ₽/день");
        rate.getStyleClass().add("rental-rate");

        Label paid = new Label("Оплачено " + money(r.paidTotal) + " из " + money(r.expectedTotal));
        paid.getStyleClass().add("rental-paid");

        VBox right = new VBox(2);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        right.getChildren().addAll(period, rate, paid);

        row.getChildren().addAll(left, grow, right, buildMapButton(r.pointId));
        container.getChildren().add(row);
        container.getChildren().add(buildHistorySection(r));
        return container;
    }

    private VBox buildHistorySection(ClientContractCard.Rental r) {
        VBox history = new VBox(6);
        history.getStyleClass().add("rental-history");
        history.setVisible(false);
        history.setManaged(false);

        if (r.payments.isEmpty()) {
            Label none = new Label("По этой точке платежей пока не было");
            none.getStyleClass().add("rental-history-empty");
            history.getChildren().add(none);
        } else {
            for (ClientContractCard.PaymentEntry p : r.payments) {
                history.getChildren().add(buildHistoryLine(p));
            }
        }

        javafx.scene.control.Button toggle =
                new javafx.scene.control.Button(toggleText(false, r.payments.size()));
        toggle.getStyleClass().add("rental-history-toggle");
        toggle.setOnAction(e -> {
            boolean show = !history.isVisible();
            history.setVisible(show);
            history.setManaged(show);
            toggle.setText(toggleText(show, r.payments.size()));
        });

        VBox section = new VBox(6);
        section.getChildren().addAll(toggle, history);
        return section;
    }

    private String toggleText(boolean expanded, int count) {
        return (expanded ? "Скрыть историю платежей" : "История платежей") + " (" + count + ")";
    }

    private HBox buildHistoryLine(ClientContractCard.PaymentEntry p) {
        HBox line = new HBox(10);
        line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        line.getStyleClass().add("rental-history-line");

        Label date = new Label(p.paidAt() == null ? "—" : DATE_FMT.format(p.paidAt()));
        date.getStyleClass().add("rental-history-date");

        Label month = new Label(p.month() == null ? "" : "за " + MONTH_FMT.format(p.month()));
        month.getStyleClass().add("text-secondary");

        Region grow = new Region();
        HBox.setHgrow(grow, javafx.scene.layout.Priority.ALWAYS);

        Label amount = new Label(money(p.amount()));
        amount.getStyleClass().add("rental-history-amount");

        line.getChildren().addAll(date, month, grow, amount);
        return line;
    }

    private javafx.scene.control.Button buildMapButton(int pointId) {
        javafx.scene.control.Button btn = new javafx.scene.control.Button("На карте");
        btn.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fth-map-pin"));
        btn.getStyleClass().add("open-on-map-button");
        btn.setOnAction(e -> mapNavigator.accept(pointId));
        return btn;
    }

    private VBox metric(String caption, String value) {
        VBox v = new VBox(2);
        Label c = new Label(caption);
        c.getStyleClass().add("metric-caption");
        Label val = new Label(value);
        val.getStyleClass().add("metric-value-sm");
        v.getChildren().addAll(c, val);
        return v;
    }

    private void showEmpty() {
        contractsBox.getChildren().clear();
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    private void showError(String message) {
        contractsBox.getChildren().clear();
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    private static String money(BigDecimal v) {
        return v == null ? "0 ₽" : MONEY.format(v) + " ₽";
    }

    private static String statusText(String status) {
        return switch (status == null ? "" : status) {
            case "active" -> "Активен";
            case "terminated" -> "Расторгнут";
            case "expired" -> "Истёк";
            default -> status == null ? "—" : status;
        };
    }

    private static String safeStatus(String s) {
        return s == null ? "unknown" : s;
    }
}
