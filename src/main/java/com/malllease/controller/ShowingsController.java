package com.malllease.controller;

import com.malllease.dao.ContractDao;
import com.malllease.dao.ShowingDao;
import com.malllease.model.Contract;
import com.malllease.model.ShowingRequestView;
import com.malllease.model.User;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ShowingsController {

    private static final LocalTime DAY_START = LocalTime.of(10, 0);
    private static final LocalTime DAY_END = LocalTime.of(18, 0);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private TextField searchField;
    @FXML private ComboBox<ResultOption> statusFilterBox;
    @FXML private DatePicker dateFilterPicker;
    @FXML private VBox statRequested;
    @FXML private VBox statToday;
    @FXML private VBox statInterested;
    @FXML private VBox statSigned;
    @FXML private Label requestedCountLabel;
    @FXML private Label todayCountLabel;
    @FXML private Label interestedCountLabel;
    @FXML private Label signedCountLabel;
    @FXML private Label queueCountLabel;
    @FXML private VBox showingListBox;
    @FXML private Label detailSubtitleLabel;
    @FXML private Label detailStatusBadge;
    @FXML private Label detailEmptyLabel;
    @FXML private VBox detailContentBox;
    @FXML private Label clientNameLabel;
    @FXML private Label contactLabel;
    @FXML private Label pointsLabel;
    @FXML private Label locationLabel;
    @FXML private DatePicker editDatePicker;
    @FXML private ComboBox<LocalTime> editTimeBox;
    @FXML private Label slotStateLabel;
    @FXML private ComboBox<ResultOption> resultComboBox;
    @FXML private TextArea managerCommentArea;
    @FXML private DatePicker contractFromPicker;
    @FXML private DatePicker contractToPicker;
    @FXML private Label contractHintLabel;
    @FXML private Label actionStatusLabel;
    @FXML private Button createContractButton;
    @FXML private Button terminateContractButton;

    private final ShowingDao showingDao = new ShowingDao();
    private final ContractDao contractDao = new ContractDao();
    private final com.malllease.service.ContractService contractService =
            new com.malllease.service.ContractService(contractDao);
    private final List<ShowingRequestView> allShowings = new ArrayList<>();

    private User currentUser;
    private ShowingRequestView selectedShowing;

    @FXML
    private void initialize() {
        setupCombos();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderQueue());
        statusFilterBox.valueProperty().addListener((obs, oldValue, newValue) -> { renderQueue(); updateStatCardHighlights(); });
        dateFilterPicker.valueProperty().addListener((obs, oldValue, newValue) -> { renderQueue(); updateStatCardHighlights(); });
        editDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> refreshEditableSlots());
        contractFromPicker.valueProperty().addListener((obs, oldValue, newValue) -> updateContractControls());
        contractToPicker.valueProperty().addListener((obs, oldValue, newValue) -> updateContractControls());
        hideDetails();
    }

    public void initUser(User user) {
        this.currentUser = user;
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleTodayFilter() {
        dateFilterPicker.setValue(LocalDate.now());
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        dateFilterPicker.setValue(null);
        statusFilterBox.getSelectionModel().selectFirst();
        updateStatCardHighlights();
    }

    @FXML
    private void handleFilterRequested() {
        boolean alreadyActive = isStatusActive("requested") && dateFilterPicker.getValue() == null;
        if (alreadyActive) {
            statusFilterBox.getSelectionModel().selectFirst();
        } else {
            dateFilterPicker.setValue(null);
            statusFilterBox.setValue(findStatusOption("requested"));
        }
    }

    @FXML
    private void handleFilterTodayCard() {
        boolean alreadyActive = LocalDate.now().equals(dateFilterPicker.getValue());
        if (alreadyActive) {
            dateFilterPicker.setValue(null);
        } else {
            statusFilterBox.getSelectionModel().selectFirst();
            dateFilterPicker.setValue(LocalDate.now());
        }
    }

    @FXML
    private void handleFilterInterested() {
        boolean alreadyActive = isStatusActive("interested") && dateFilterPicker.getValue() == null;
        if (alreadyActive) {
            statusFilterBox.getSelectionModel().selectFirst();
        } else {
            dateFilterPicker.setValue(null);
            statusFilterBox.setValue(findStatusOption("interested"));
        }
    }

    @FXML
    private void handleFilterSigned() {
        boolean alreadyActive = isStatusActive("contract_signed") && dateFilterPicker.getValue() == null;
        if (alreadyActive) {
            statusFilterBox.getSelectionModel().selectFirst();
        } else {
            dateFilterPicker.setValue(null);
            statusFilterBox.setValue(findStatusOption("contract_signed"));
        }
    }

    private boolean isStatusActive(String value) {
        ResultOption current = statusFilterBox.getValue();
        return current != null && value.equals(current.value());
    }

    private ResultOption findStatusOption(String value) {
        return statusFilterBox.getItems().stream()
                .filter(opt -> opt.value() != null && opt.value().equals(value))
                .findFirst().orElse(null);
    }

    private void updateStatCardHighlights() {
        String status = statusFilterBox.getValue() == null ? null : statusFilterBox.getValue().value();
        LocalDate date = dateFilterPicker.getValue();
        setStatCardActive(statRequested, "requested".equals(status) && date == null);
        setStatCardActive(statToday, LocalDate.now().equals(date));
        setStatCardActive(statInterested, "interested".equals(status) && date == null);
        setStatCardActive(statSigned, "contract_signed".equals(status) && date == null);
    }

    private void setStatCardActive(VBox card, boolean active) {
        if (card == null) return;
        card.getStyleClass().remove("showing-stat-card-active");
        if (active) card.getStyleClass().add("showing-stat-card-active");
    }

    @FXML
    private void handleSave() {
        if (selectedShowing == null) {
            return;
        }
        if (editDatePicker.getValue() == null || editTimeBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Выберите дату и время показа");
            return;
        }

        LocalDateTime nextSlot = LocalDateTime.of(editDatePicker.getValue(), editTimeBox.getValue());
        if (showingDao.isManagerSlotBusy(selectedShowing.getManagerUserId(), nextSlot, selectedShowing.getShowingId())) {
            refreshEditableSlots();
            showAlert(Alert.AlertType.WARNING, "Этот слот уже занят. Выберите другое время.");
            return;
        }

        ResultOption result = resultComboBox.getValue();
        String resultCode = result == null ? "requested" : result.value();
        try {
            showingDao.updateScheduleAndResult(
                    selectedShowing.getShowingId(),
                    nextSlot,
                    resultCode,
                    managerCommentArea.getText());
            refresh();
            selectShowingById(selectedShowing.getShowingId());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Не удалось сохранить показ. Проверьте подключение к БД.");
        }
    }

    @FXML
    private void handleCreateContract() {
        if (selectedShowing == null) {
            return;
        }

        LocalDate dateFrom = contractFromPicker.getValue();
        LocalDate dateTo = contractToPicker.getValue();
        if (dateFrom == null || dateTo == null) {
            setActionStatus("Выберите период аренды", false);
            return;
        }
        if (dateFrom.isAfter(dateTo)) {
            setActionStatus("Дата начала не может быть позже окончания", false);
            return;
        }

        int showingId = selectedShowing.getShowingId();
        createContractButton.setDisable(true);
        createContractButton.setText("Создаем...");
        setActionStatus("Создаем договор по показу #" + showingId, true);

        try {
            int contractId = contractService.signContractFromShowing(
                    showingId, dateFrom, dateTo, managerCommentArea.getText());
            refresh();
            selectShowingById(showingId);
            setActionStatus("Договор #" + contractId + " создан, точки переведены в занятые.", true);
        } catch (Exception e) {
            setActionStatus("Не удалось заключить договор: " + rootMessage(e), false);
        } finally {
            createContractButton.setText("Заключить договор");
            updateContractControls();
        }
    }

    @FXML
    private void handleTerminateContract() {
        if (selectedShowing == null) {
            return;
        }
        int showingId = selectedShowing.getShowingId();
        Optional<Contract> contractOpt;
        try {
            contractOpt = contractDao.findByShowing(showingId);
        } catch (Exception e) {
            setActionStatus("Не удалось найти договор: " + rootMessage(e), false);
            return;
        }
        if (contractOpt.isEmpty()) {
            setActionStatus("Договор по этому показу не найден", false);
            return;
        }
        Contract contract = contractOpt.get();
        if (!"active".equals(contract.getStatus())) {
            setActionStatus("Договор " + contract.getContractNo() + " уже не активен", false);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Расторгнуть договор " + contract.getContractNo()
                        + "? Все точки по нему снова станут свободными.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Mall Lease");
        confirm.setHeaderText(null);
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        terminateContractButton.setDisable(true);
        try {
            contractService.terminateContract(contract.getContractId());
            refresh();
            selectShowingById(showingId);
            setActionStatus("Договор " + contract.getContractNo()
                    + " расторгнут, точки освобождены.", true);
        } catch (Exception e) {
            setActionStatus("Не удалось расторгнуть договор: " + rootMessage(e), false);
        } finally {
            terminateContractButton.setDisable(false);
        }
    }

    private void setupCombos() {
        List<ResultOption> options = List.of(
                new ResultOption(null, "Все статусы"),
                new ResultOption("requested", "Запрошен"),
                new ResultOption("interested", "Интерес есть"),
                new ResultOption("refused", "Отказ"),
                new ResultOption("contract_signed", "Договор подписан")
        );

        statusFilterBox.setItems(FXCollections.observableArrayList(options));
        statusFilterBox.getSelectionModel().selectFirst();
        statusFilterBox.setConverter(resultConverter());

        resultComboBox.setItems(FXCollections.observableArrayList(options.subList(1, options.size())));
        resultComboBox.setConverter(resultConverter());

        editTimeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime value) {
                return value == null ? "" : value.format(TIME_FORMATTER);
            }

            @Override
            public LocalTime fromString(String value) {
                return value == null || value.isBlank() ? null : LocalTime.parse(value, TIME_FORMATTER);
            }
        });
    }

    private StringConverter<ResultOption> resultConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(ResultOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public ResultOption fromString(String value) {
                return null;
            }
        };
    }

    private void refresh() {
        try {
            allShowings.clear();
            allShowings.addAll(showingDao.findRequestViews());
            renderStats();
            renderQueue();
            if (selectedShowing == null) {
                filteredShowings().stream().findFirst().ifPresent(this::selectShowing);
            }
        } catch (Exception e) {
            showingListBox.getChildren().setAll(emptyLabel("Не удалось загрузить показы. Проверьте подключение к БД."));
            queueCountLabel.setText("0");
        }
    }

    private void renderStats() {
        long requested = allShowings.stream().filter(showing -> "requested".equals(showing.getResult())).count();
        long today = allShowings.stream().filter(showing -> showing.getShownAt().toLocalDate().equals(LocalDate.now())).count();
        long interested = allShowings.stream().filter(showing -> "interested".equals(showing.getResult())).count();
        long signed = allShowings.stream().filter(showing -> "contract_signed".equals(showing.getResult())).count();

        requestedCountLabel.setText(Long.toString(requested));
        todayCountLabel.setText(Long.toString(today));
        interestedCountLabel.setText(Long.toString(interested));
        signedCountLabel.setText(Long.toString(signed));
    }

    private void renderQueue() {
        showingListBox.getChildren().clear();
        List<ShowingRequestView> rows = filteredShowings();
        queueCountLabel.setText(Integer.toString(rows.size()));

        if (rows.isEmpty()) {
            showingListBox.getChildren().add(emptyLabel("По текущим фильтрам показов нет."));
            return;
        }

        for (ShowingRequestView showing : rows) {
            showingListBox.getChildren().add(createShowingCard(showing));
        }
    }

    private List<ShowingRequestView> filteredShowings() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        ResultOption status = statusFilterBox.getValue();
        LocalDate date = dateFilterPicker.getValue();

        return allShowings.stream()
                .filter(showing -> status == null || status.value() == null || status.value().equals(showing.getResult()))
                .filter(showing -> date == null || showing.getShownAt().toLocalDate().equals(date))
                .filter(showing -> query.isEmpty() || searchableText(showing).contains(query))
                .sorted(Comparator.comparing(ShowingRequestView::getShownAt))
                .toList();
    }

    private String searchableText(ShowingRequestView showing) {
        return String.join(" ",
                safe(showing.getCompanyName()),
                safe(showing.getManagerName()),
                safe(showing.getPointCodes()),
                safe(showing.getPointLocations()),
                safe(showing.getComment()))
                .toLowerCase(Locale.ROOT);
    }

    private Button createShowingCard(ShowingRequestView showing) {
        VBox content = new VBox(5);
        Label title = new Label(showing.getCompanyName());
        title.getStyleClass().add("showing-card-title");
        Label meta = new Label(formatDateTime(showing.getShownAt()) + " · " + safe(showing.getPointCodes()));
        meta.getStyleClass().add("showing-card-meta");
        Label status = new Label(statusText(showing.getResult()));
        status.getStyleClass().addAll("showing-card-status", statusClass(showing.getResult()));
        content.getChildren().addAll(title, meta, status);

        Button card = new Button();
        card.setMaxWidth(Double.MAX_VALUE);
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.getStyleClass().add("showing-card");
        if (selectedShowing != null && selectedShowing.getShowingId() == showing.getShowingId()) {
            card.getStyleClass().add("showing-card-active");
        }
        card.setOnAction(event -> selectShowing(showing));
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);
        return card;
    }

    private void selectShowing(ShowingRequestView showing) {
        selectedShowing = showing;
        detailSubtitleLabel.setText("#" + showing.getShowingId() + " · " + formatDateTime(showing.getShownAt()));
        clientNameLabel.setText(showing.getCompanyName());
        contactLabel.setText("Менеджер: " + safe(showing.getManagerName()));
        pointsLabel.setText(safe(showing.getPointCodes()).isBlank() ? "Точки не указаны" : showing.getPointCodes());
        locationLabel.setText(safe(showing.getPointLocations()));
        managerCommentArea.setText(safe(showing.getComment()));
        detailStatusBadge.setText(statusText(showing.getResult()));
        detailStatusBadge.getStyleClass().removeAll(
                "showing-status-requested",
                "showing-status-interested",
                "showing-status-refused",
                "showing-status-contract-signed");
        detailStatusBadge.getStyleClass().add(statusClass(showing.getResult()));
        resultComboBox.getSelectionModel().select(findResultOption(showing.getResult()));
        editDatePicker.setValue(showing.getShownAt().toLocalDate());
        refreshEditableSlots();
        editTimeBox.getSelectionModel().select(showing.getShownAt().toLocalTime());
        setupContractDefaults(showing);

        detailEmptyLabel.setVisible(false);
        detailEmptyLabel.setManaged(false);
        detailContentBox.setVisible(true);
        detailContentBox.setManaged(true);
        renderQueue();
    }

    private void setupContractDefaults(ShowingRequestView showing) {
        LocalDate defaultFrom = showing.getShownAt().toLocalDate().isAfter(LocalDate.now())
                ? showing.getShownAt().toLocalDate()
                : LocalDate.now();
        contractFromPicker.setValue(defaultFrom);
        contractToPicker.setValue(defaultFrom.plusMonths(6).minusDays(1));
        clearActionStatus();
        updateContractControls();
    }

    private void selectShowingById(int showingId) {
        Optional<ShowingRequestView> showingOpt = allShowings.stream()
                .filter(showing -> showing.getShowingId() == showingId)
                .findFirst();
        showingOpt.ifPresentOrElse(this::selectShowing, this::hideDetails);
    }

    private void refreshEditableSlots() {
        if (selectedShowing == null || editDatePicker.getValue() == null) {
            return;
        }

        LocalDate date = editDatePicker.getValue();
        List<LocalTime> busy = showingDao.findBusySlotTimes(
                selectedShowing.getManagerUserId(),
                date,
                selectedShowing.getShowingId());
        List<LocalTime> slots = buildSlots(date, busy);
        LocalTime currentTime = selectedShowing.getShownAt().toLocalTime();
        if (date.equals(selectedShowing.getShownAt().toLocalDate()) && !slots.contains(currentTime)) {
            slots = new ArrayList<>(slots);
            slots.add(currentTime);
            slots.sort(Comparator.naturalOrder());
        }
        editTimeBox.setItems(FXCollections.observableArrayList(slots));
        slotStateLabel.setText(slots.isEmpty() ? "Нет свободных окон" : "Свободных окон: " + slots.size());
    }

    private List<LocalTime> buildSlots(LocalDate date, List<LocalTime> busySlots) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime now = LocalTime.now().plusMinutes(30);
        for (LocalTime time = DAY_START; time.isBefore(DAY_END); time = time.plusMinutes(30)) {
            if (date.equals(LocalDate.now()) && time.isBefore(now)) {
                continue;
            }
            if (!busySlots.contains(time)) {
                slots.add(time);
            }
        }
        return slots;
    }

    private void hideDetails() {
        selectedShowing = null;
        detailSubtitleLabel.setText("Выберите показ в очереди");
        detailStatusBadge.setText("—");
        detailEmptyLabel.setVisible(true);
        detailEmptyLabel.setManaged(true);
        detailContentBox.setVisible(false);
        detailContentBox.setManaged(false);
        if (createContractButton != null) {
            createContractButton.setDisable(true);
        }
        setTerminateVisible(false);
        clearActionStatus();
    }

    private void setTerminateVisible(boolean visible) {
        if (terminateContractButton == null) {
            return;
        }
        terminateContractButton.setVisible(visible);
        terminateContractButton.setManaged(visible);
    }

    private void updateContractControls() {
        if (contractHintLabel == null || createContractButton == null) {
            return;
        }
        if (selectedShowing == null) {
            contractHintLabel.setText("Выберите показ");
            createContractButton.setDisable(true);
            setTerminateVisible(false);
            return;
        }
        setTerminateVisible("contract_signed".equals(selectedShowing.getResult()));
        if ("contract_signed".equals(selectedShowing.getResult())) {
            contractHintLabel.setText("Этот показ закрыт договором — его можно расторгнуть");
            createContractButton.setDisable(true);
            return;
        }
        if ("refused".equals(selectedShowing.getResult())) {
            contractHintLabel.setText("По отказанному показу договор не создается");
            createContractButton.setDisable(true);
            return;
        }

        LocalDate dateFrom = contractFromPicker.getValue();
        LocalDate dateTo = contractToPicker.getValue();
        if (dateFrom == null || dateTo == null) {
            contractHintLabel.setText("Укажите даты аренды");
            createContractButton.setDisable(true);
            return;
        }
        if (dateFrom.isAfter(dateTo)) {
            contractHintLabel.setText("Дата начала позже даты окончания");
            createContractButton.setDisable(true);
            return;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;
        contractHintLabel.setText("Будет создан договор на " + days + " дн. по выбранным точкам");
        createContractButton.setDisable(false);
    }

    private ResultOption findResultOption(String value) {
        return resultComboBox.getItems().stream()
                .filter(option -> option.value().equals(value))
                .findFirst()
                .orElse(null);
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("point-list-empty");
        label.setWrapText(true);
        return label;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String statusText(String status) {
        return switch (status == null ? "" : status) {
            case "interested" -> "Интерес есть";
            case "refused" -> "Отказ";
            case "contract_signed" -> "Договор подписан";
            default -> "Запрошен";
        };
    }

    private String statusClass(String status) {
        return switch (status == null ? "" : status) {
            case "interested" -> "showing-status-interested";
            case "refused" -> "showing-status-refused";
            case "contract_signed" -> "showing-status-contract-signed";
            default -> "showing-status-requested";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Mall Lease");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.showAndWait();
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

    private record ResultOption(String value, String label) {
    }
}
