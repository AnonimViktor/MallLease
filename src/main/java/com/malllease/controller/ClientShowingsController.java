package com.malllease.controller;

import com.malllease.dao.ClientDao;
import com.malllease.dao.ShowingDao;
import com.malllease.model.ShowingRequestView;
import com.malllease.model.User;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientShowingsController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Label emptyLabel;
    @FXML private VBox showingListBox;
    @FXML private Label totalCountLabel;

    private final ShowingDao showingDao = new ShowingDao();
    private final ClientDao clientDao = new ClientDao();

    private User currentUser;

    public void initUser(User user) {
        this.currentUser = user;
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    private void refresh() {
        showingListBox.getChildren().clear();
        if (currentUser == null) return;

        try {
            int clientId = clientDao.findByUserId(currentUser.getUserId())
                    .map(c -> c.getClientId())
                    .orElse(-1);

            if (clientId == -1) {
                showEmpty("Профиль клиента не найден. Обратитесь к администратору.");
                return;
            }

            List<ShowingRequestView> showings = showingDao.findRequestViewsByClient(clientId);

            if (showings.isEmpty()) {
                showEmpty("У вас пока нет запрошенных показов. Выберите свободную точку на карте и нажмите «Запросить показ».");
                totalCountLabel.setText("0");
                return;
            }

            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            totalCountLabel.setText(Integer.toString(showings.size()));

            for (ShowingRequestView sv : showings) {
                showingListBox.getChildren().add(buildCard(sv));
            }
        } catch (Exception e) {
            showEmpty("Не удалось загрузить показы. Проверьте подключение к БД.");
        }
    }

    private void showEmpty(String text) {
        emptyLabel.setText(text);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
        totalCountLabel.setText("0");
    }

    private VBox buildCard(ShowingRequestView sv) {
        VBox card = new VBox(8);
        card.getStyleClass().add("client-showing-card");
        card.setPadding(new Insets(14));

        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String pointText = sv.getPointCodes() == null || sv.getPointCodes().isBlank()
                ? "Точка не указана" : sv.getPointCodes();
        Label pointLabel = new Label(pointText);
        pointLabel.getStyleClass().add("showing-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(resultLabel(sv.getResult()));
        statusBadge.getStyleClass().addAll("showing-status-badge", resultCss(sv.getResult()));

        header.getChildren().addAll(pointLabel, spacer, statusBadge);

        Label dateLabel = new Label(sv.getShownAt() != null ? sv.getShownAt().format(DT_FMT) : "—");
        dateLabel.getStyleClass().add("text-secondary");

        card.getChildren().addAll(header, dateLabel);

        if (sv.getPointLocations() != null && !sv.getPointLocations().isBlank()) {
            Label locLabel = new Label(sv.getPointLocations());
            locLabel.getStyleClass().add("text-secondary");
            locLabel.setWrapText(true);
            card.getChildren().add(locLabel);
        }

        if (sv.getComment() != null && !sv.getComment().isBlank()) {
            Label commentLabel = new Label(sv.getComment());
            commentLabel.getStyleClass().add("slot-hint");
            commentLabel.setWrapText(true);
            card.getChildren().add(commentLabel);
        }

        return card;
    }

    private String resultLabel(String result) {
        return switch (result == null ? "requested" : result) {
            case "interested" -> "Интерес есть";
            case "refused" -> "Отказ";
            case "contract_signed" -> "Договор подписан";
            default -> "Запрошен";
        };
    }

    private String resultCss(String result) {
        return switch (result == null ? "requested" : result) {
            case "interested" -> "showing-status-interested";
            case "refused" -> "showing-status-refused";
            case "contract_signed" -> "showing-status-contract-signed";
            default -> "showing-status-requested";
        };
    }

    private void showAlert(Alert.AlertType type, String text) {
        Alert alert = new Alert(type, text);
        alert.setTitle("Mall Lease");
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm());
        alert.showAndWait();
    }
}
