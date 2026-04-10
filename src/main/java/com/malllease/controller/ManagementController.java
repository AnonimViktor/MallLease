package com.malllease.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.malllease.dao.ManagementDao;
import com.malllease.model.User;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class ManagementController {

    @FXML private ComboBox<Section> sectionComboBox;
    @FXML private TextField tableSearchField;
    @FXML private ComboBox<RowFilter> tableFilterComboBox;
    @FXML private TableView<LinkedHashMap<String, Object>> dataTable;
    @FXML private GridPane formGrid;
    @FXML private ScrollPane formScrollPane;
    @FXML private Button newButton;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Label statusLabel;
    @FXML private Label filteredCountLabel;
    @FXML private Label formTitleLabel;

    private final ManagementDao managementDao = new ManagementDao();
    private final Map<String, Control> formControls = new LinkedHashMap<>();
    private final List<LinkedHashMap<String, Object>> currentRows = new ArrayList<>();

    private User currentUser;
    private Section currentSection;
    private LinkedHashMap<String, Object> selectedRow;
    private boolean updatingTableFilters = false;
    private java.util.function.IntConsumer mapNavigator = id -> {};
    private Button openOnMapButton;
    @FXML private javafx.scene.layout.HBox openOnMapRow;

    public void setMapNavigator(java.util.function.IntConsumer nav) {
        this.mapNavigator = nav == null ? id -> {} : nav;
    }

    @FXML
    private void initialize() {
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        sectionComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Section section) {
                return section == null ? "" : section.title();
            }

            @Override
            public Section fromString(String value) {
                return null;
            }
        });

        sectionComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentSection = newValue;
            selectedRow = null;
            resetTableFilters(false);
            buildForm();
            refreshTable();
        });

        tableSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyTableFilters());
        tableFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(RowFilter filter) {
                return filter == null ? "" : filter.label();
            }

            @Override
            public RowFilter fromString(String value) {
                return null;
            }
        });
        tableFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyTableFilters());

        dataTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> {
            selectedRow = row;
            fillForm(row);
            updateButtonState();
        });
    }

    public void initUser(User user, String defaultSectionId) {
        this.currentUser = user;
        List<Section> sections = buildSections(user);
        sectionComboBox.setItems(FXCollections.observableArrayList(sections));

        Section selected = sections.stream()
                .filter(section -> section.id().equals(defaultSectionId))
                .findFirst()
                .orElse(sections.isEmpty() ? null : sections.get(0));

        sectionComboBox.getSelectionModel().select(selected);

        boolean isAdmin = user != null && user.getRole() != null
                && "admin".equals(user.getRole().getCode());
        sectionComboBox.setVisible(isAdmin);
        sectionComboBox.setManaged(isAdmin);
    }

    @FXML
    private void handleNew() {
        selectedRow = null;
        dataTable.getSelectionModel().clearSelection();
        clearForm();
        setStatus("Новая запись");
        updateButtonState();
    }

    @FXML
    private void handleSave() {
        if (currentSection == null || currentSection.readOnly()) {
            return;
        }

        try {
            if (selectedRow == null) {
                insertCurrent();
                setStatus("Запись создана");
            } else {
                updateCurrent();
                setStatus("Запись обновлена");
            }
            refreshTable();
        } catch (Exception e) {
            setStatus("Ошибка сохранения: " + rootMessage(e));
        }
    }

    @FXML
    private void handleDelete() {
        if (currentSection == null || currentSection.readOnly() || selectedRow == null) {
            return;
        }

        try {
            List<Object> params = primaryKeyParams(currentSection, selectedRow);
            String sql = currentSection.deleteSql();
            if (sql == null || sql.isBlank()) {
                sql = buildDeleteSql(currentSection);
            }
            managementDao.execute(sql, params);
            selectedRow = null;
            clearForm();
            refreshTable();
            setStatus("Запись удалена или деактивирована");
        } catch (Exception e) {
            setStatus("Ошибка удаления: " + rootMessage(e));
        }
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
        setStatus("Данные обновлены");
    }

    @FXML
    private void handleResetTableFilters() {
        resetTableFilters(true);
    }

    private void refreshTable() {
        dataTable.getColumns().clear();
        dataTable.getItems().clear();
        currentRows.clear();
        clearForm();

        if (currentSection == null) {
            return;
        }

        try {
            List<LinkedHashMap<String, Object>> rows =
                    managementDao.query(currentSection.selectSql(), currentSection.selectParams());
            currentRows.addAll(rows);

            for (ColumnDef columnDef : currentSection.columns()) {
                TableColumn<LinkedHashMap<String, Object>, String> column = new TableColumn<>(columnDef.label());
                column.setCellValueFactory(data ->
                        new SimpleStringProperty(formatValue(columnDef.column(), data.getValue().get(columnDef.column()))));
                column.setPrefWidth(columnDef.width());
                if (isBadgeColumn(columnDef.column())) {
                    column.setMinWidth(columnDef.width());
                    applyBadgeCellFactory(column);
                } else {
                    column.setMinWidth(Math.max(64, columnDef.width() * 0.7));
                }
                dataTable.getColumns().add(column);
            }

            rebuildQuickFilters();
            applyTableFilters();
            if (!dataTable.getItems().isEmpty()) {
                dataTable.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            setStatus("Ошибка загрузки: " + rootMessage(e));
        }

        updateButtonState();
    }

    private void applyTableFilters() {
        if (updatingTableFilters || currentSection == null) {
            return;
        }

        String query = tableSearchField.getText() == null
                ? ""
                : tableSearchField.getText().trim().toLowerCase();
        RowFilter filter = tableFilterComboBox.getValue();

        List<LinkedHashMap<String, Object>> filtered = currentRows.stream()
                .filter(row -> matchesSearch(row, query))
                .filter(row -> matchesQuickFilter(row, filter))
                .toList();

        dataTable.getItems().setAll(filtered);
        if (filtered.isEmpty()) {
            selectedRow = null;
            clearForm();
        } else if (selectedRow == null || !filtered.contains(selectedRow)) {
            dataTable.getSelectionModel().selectFirst();
        }

        filteredCountLabel.setText("Показано: " + filtered.size() + " / " + currentRows.size());
        setStatus("Записей: " + currentRows.size());
    }

    private boolean matchesSearch(Map<String, Object> row, String query) {
        if (query.isBlank()) {
            return true;
        }
        return row.values().stream()
                .map(value -> Objects.toString(value, "").toLowerCase())
                .anyMatch(value -> value.contains(query));
    }

    private boolean matchesQuickFilter(Map<String, Object> row, RowFilter filter) {
        if (filter == null || filter.column() == null) {
            return true;
        }
        return Objects.equals(Objects.toString(row.get(filter.column()), ""), filter.value());
    }

    private void rebuildQuickFilters() {
        updatingTableFilters = true;
        try {
            List<RowFilter> options = new ArrayList<>();
            options.add(new RowFilter(null, null, "Все записи"));

            String filterColumn = currentSection.columns().stream()
                    .map(ColumnDef::column)
                    .filter(column -> column.equals("result") || column.equals("status"))
                    .findFirst()
                    .orElse(null);

            if (filterColumn != null) {
                currentRows.stream()
                        .map(row -> Objects.toString(row.get(filterColumn), ""))
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .sorted()
                        .map(value -> new RowFilter(filterColumn, value, statusLabel(value)))
                        .forEach(options::add);
            }

            tableFilterComboBox.setItems(FXCollections.observableArrayList(options));
            tableFilterComboBox.getSelectionModel().selectFirst();
            tableFilterComboBox.setDisable(options.size() == 1);
        } finally {
            updatingTableFilters = false;
        }
    }

    private void resetTableFilters(boolean apply) {
        updatingTableFilters = true;
        try {
            if (tableSearchField != null) {
                tableSearchField.clear();
            }
            if (tableFilterComboBox != null) {
                tableFilterComboBox.getSelectionModel().selectFirst();
            }
        } finally {
            updatingTableFilters = false;
        }
        if (apply) {
            applyTableFilters();
        }
    }

    private void buildForm() {
        formControls.clear();
        formGrid.getChildren().clear();

        if (currentSection == null) {
            formTitleLabel.setText("Форма");
            return;
        }

        formTitleLabel.setText(currentSection.readOnly()
                ? currentSection.title() + " / просмотр"
                : currentSection.title() + " / форма");

        int rowIndex = 0;
        for (FieldDef field : currentSection.fields()) {
            Label label = new Label(field.label());
            label.getStyleClass().add("text-secondary");
            label.setMinWidth(112);
            label.setPrefWidth(112);
            label.setWrapText(true);

            Control control = createControl(field);
            formControls.put(field.column(), control);

            formGrid.add(label, 0, rowIndex);
            formGrid.add(control, 1, rowIndex);
            GridPane.setHgrow(control, Priority.ALWAYS);
            rowIndex++;
        }

        formScrollPane.setFitToWidth(true);
        updateButtonState();
    }

    private Control createControl(FieldDef field) {
        if (field.type() == FieldType.REFERENCE || field.type() == FieldType.CHOICE) {
            ComboBox<OptionItem> comboBox = new ComboBox<>();
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setPromptText("Выберите...");
            comboBox.setDisable(currentSection.readOnly());
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(OptionItem item) {
                    return item == null ? "" : item.label();
                }

                @Override
                public OptionItem fromString(String value) {
                    return null;
                }
            });
            comboBox.setItems(FXCollections.observableArrayList(loadOptions(field)));
            return comboBox;
        }

        if (field.type() == FieldType.BOOLEAN) {
            CheckBox checkBox = new CheckBox();
            checkBox.setDisable(currentSection.readOnly());
            return checkBox;
        }

        if (field.type() == FieldType.TEXT_LONG) {
            TextArea area = new TextArea();
            area.setPrefRowCount(3);
            area.setWrapText(true);
            area.setDisable(currentSection.readOnly());
            return area;
        }

        TextField textField = field.type() == FieldType.PASSWORD
                ? new javafx.scene.control.PasswordField()
                : new TextField();
        textField.setPromptText(field.prompt());
        textField.setDisable(currentSection.readOnly());
        return textField;
    }

    private void fillForm(Map<String, Object> row) {
        if (row == null) {
            clearForm();
            return;
        }

        for (FieldDef field : currentSection.fields()) {
            Control control = formControls.get(field.column());
            Object value = row.get(field.column());

            if (control instanceof CheckBox checkBox) {
                checkBox.setSelected(Boolean.TRUE.equals(value));
            } else if (control instanceof ComboBox<?> comboBox) {
                selectOption(comboBox, value);
            } else if (control instanceof TextArea textArea) {
                textArea.setText(formatInputValue(field, value));
            } else if (control instanceof TextField textField) {
                textField.setText(field.type() == FieldType.PASSWORD ? "" : formatInputValue(field, value));
            }
        }
    }

    private void clearForm() {
        for (Control control : formControls.values()) {
            if (control instanceof CheckBox checkBox) {
                checkBox.setSelected(false);
            } else if (control instanceof ComboBox<?> comboBox) {
                comboBox.getSelectionModel().clearSelection();
            } else if (control instanceof TextArea textArea) {
                textArea.clear();
            } else if (control instanceof TextField textField) {
                textField.clear();
            }
        }

        Control activeControl = formControls.get("is_active");
        if (activeControl instanceof CheckBox checkBox) {
            checkBox.setSelected(true);
        }
    }

    private void insertCurrent() {
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();

        for (FieldDef field : currentSection.fields()) {
            if (!shouldIncludeOnInsert(field)) {
                continue;
            }
            Object value = readFieldValue(field, true);
            columns.add(field.column());
            placeholders.add("?");
            params.add(value);
            values.put(field.column(), value);
        }

        validateRow(values);

        String sql = "INSERT INTO " + currentSection.table() +
                " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")";
        managementDao.execute(sql, params);
    }

    private void updateCurrent() {
        List<String> assignments = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        Set<String> pkColumns = new LinkedHashSet<>(currentSection.pkColumns());
        Map<String, Object> values = new LinkedHashMap<>(selectedRow);

        for (FieldDef field : currentSection.fields()) {
            if (pkColumns.contains(field.column())) {
                continue;
            }
            if (field.type() == FieldType.PASSWORD && readRawField(field).isBlank()) {
                continue;
            }
            Object value = readFieldValue(field, false);
            assignments.add(field.column() + " = ?");
            params.add(value);
            values.put(field.column(), value);
        }

        if (assignments.isEmpty()) {
            return;
        }

        validateRow(values);

        String sql = "UPDATE " + currentSection.table() +
                " SET " + String.join(", ", assignments) +
                " WHERE " + primaryKeyWhere(currentSection);
        params.addAll(primaryKeyParams(currentSection, selectedRow));
        managementDao.execute(sql, params);
    }

    private boolean shouldIncludeOnInsert(FieldDef field) {
        if (field.type() == FieldType.BOOLEAN) {
            return true;
        }
        if (field.type() == FieldType.REFERENCE || field.type() == FieldType.CHOICE) {
            return field.requiredOnCreate() || readFieldValue(field, true) != null;
        }
        String raw = readRawField(field);
        return field.requiredOnCreate() || !raw.isBlank();
    }

    private Object readFieldValue(FieldDef field, boolean createMode) {
        if (field.type() == FieldType.REFERENCE || field.type() == FieldType.CHOICE) {
            ComboBox<?> comboBox = (ComboBox<?>) formControls.get(field.column());
            Object selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected instanceof OptionItem optionItem) {
                return optionItem.value();
            }
            if ((createMode && field.requiredOnCreate()) || (!createMode && field.requiredOnUpdate())) {
                throw new IllegalArgumentException("Выберите значение: " + field.label());
            }
            return null;
        }

        String raw = readRawField(field);

        if (raw.isBlank() && !(field.type() == FieldType.BOOLEAN)) {
            if ((createMode && field.requiredOnCreate()) || (!createMode && field.requiredOnUpdate())) {
                throw new IllegalArgumentException("Заполните поле: " + field.label());
            }
            return null;
        }

        try {
            Object value = switch (field.type()) {
                case TEXT, TEXT_LONG -> raw;
                case INT -> Integer.parseInt(raw);
                case DECIMAL -> new BigDecimal(raw.replace(',', '.'));
                case BOOLEAN -> ((CheckBox) formControls.get(field.column())).isSelected();
                case DATE -> Date.valueOf(LocalDate.parse(raw));
                case TIMESTAMP -> Timestamp.valueOf(parseDateTime(raw));
                case PASSWORD -> BCrypt.withDefaults().hashToString(10, raw.toCharArray());
                case REFERENCE, CHOICE -> throw new IllegalStateException("Combo fields are handled before raw parsing");
            };
            validateFieldValue(field, value);
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Введите корректное число: " + field.label());
        } catch (IllegalArgumentException e) {
            if (field.type() == FieldType.DATE || field.type() == FieldType.TIMESTAMP) {
                throw new IllegalArgumentException("Проверьте формат даты: " + field.label());
            }
            throw e;
        }
    }

    private String readRawField(FieldDef field) {
        Control control = formControls.get(field.column());
        if (control instanceof CheckBox checkBox) {
            return Boolean.toString(checkBox.isSelected());
        }
        if (control instanceof ComboBox<?> comboBox) {
            Object selected = comboBox.getSelectionModel().getSelectedItem();
            return selected == null ? "" : selected.toString();
        }
        if (control instanceof TextArea textArea) {
            return textArea.getText().trim();
        }
        if (control instanceof TextField textField) {
            return textField.getText().trim();
        }
        return "";
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw.contains("T")) {
            return LocalDateTime.parse(raw);
        }
        return LocalDateTime.parse(raw.replace(' ', 'T'));
    }

    private List<OptionItem> loadOptions(FieldDef field) {
        if (field.type() == FieldType.CHOICE) {
            return field.options();
        }
        List<LinkedHashMap<String, Object>> rows = managementDao.query(field.referenceSql(), List.of());
        List<OptionItem> options = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get("value");
            Object label = row.get("label");
            options.add(new OptionItem(value, Objects.toString(label, "")));
        }
        return options;
    }

    @SuppressWarnings("unchecked")
    private void selectOption(ComboBox<?> comboBox, Object value) {
        ComboBox<OptionItem> typedComboBox = (ComboBox<OptionItem>) comboBox;
        typedComboBox.getSelectionModel().clearSelection();
        if (value == null) {
            return;
        }
        for (OptionItem option : typedComboBox.getItems()) {
            if (Objects.equals(option.value(), value)) {
                typedComboBox.getSelectionModel().select(option);
                return;
            }
        }
    }

    private void validateFieldValue(FieldDef field, Object value) {
        if (value instanceof BigDecimal decimal && decimal.compareTo(BigDecimal.ZERO) <= 0
                && Set.of("area_m2", "current_daily_rate", "daily_rate_fixed", "amount").contains(field.column())) {
            throw new IllegalArgumentException("Введите положительное значение: " + field.label());
        }
        if ("floor".equals(field.column()) && value instanceof Integer floor && floor < 1) {
            throw new IllegalArgumentException("Этаж должен быть больше нуля");
        }
        if ("status".equals(field.column()) && value instanceof String status && !status.isBlank()) {
            validateStatus(status);
        }
        if (field.type() == FieldType.PASSWORD && value != null && readRawField(field).length() < 8) {
            throw new IllegalArgumentException("Пароль должен быть не короче 8 символов");
        }
    }

    private void validateRow(Map<String, Object> values) {
        validateDateOrder(values, "date_from", "date_to", "Дата начала не может быть позже даты окончания");
        validateDateOrder(values, "period_from", "period_to", "Начало периода платежа не может быть позже окончания");
    }

    private void validateDateOrder(Map<String, Object> values, String fromColumn, String toColumn, String message) {
        Object from = values.get(fromColumn);
        Object to = values.get(toColumn);
        if (from instanceof Date fromDate && to instanceof Date toDate && fromDate.after(toDate)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateStatus(String status) {
        Set<String> allowed = Set.of(
                "active", "inactive", "draft", "completed", "terminated",
                "free", "occupied", "unavailable", "paid", "unpaid",
                "requested", "interested", "refused", "contract_signed"
        );
        if (!allowed.contains(status)) {
            throw new IllegalArgumentException("Недопустимый статус: " + status);
        }
    }

    private String buildDeleteSql(Section section) {
        return "DELETE FROM " + section.table() + " WHERE " + primaryKeyWhere(section);
    }

    private String primaryKeyWhere(Section section) {
        StringJoiner joiner = new StringJoiner(" AND ");
        for (String column : section.pkColumns()) {
            joiner.add(column + " = ?");
        }
        return joiner.toString();
    }

    private List<Object> primaryKeyParams(Section section, Map<String, Object> row) {
        List<Object> params = new ArrayList<>();
        for (String column : section.pkColumns()) {
            params.add(row.get(column));
        }
        return params;
    }

    private void updateButtonState() {
        boolean noSection = currentSection == null;
        boolean readOnly = noSection || currentSection.readOnly();

        newButton.setDisable(readOnly);
        saveButton.setDisable(readOnly);
        deleteButton.setDisable(readOnly || selectedRow == null);
        updateOpenOnMapButton();
    }

    private void updateOpenOnMapButton() {
        boolean isTradePoint = currentSection != null && "points".equals(currentSection.id())
                && selectedRow != null && selectedRow.get("trade_point_id") instanceof Number;
        if (!isTradePoint) {
            if (openOnMapRow != null) {
                openOnMapRow.setVisible(false);
                openOnMapRow.setManaged(false);
            }
            return;
        }
        if (openOnMapButton == null) {
            openOnMapButton = new Button("Открыть на карте");
            openOnMapButton.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fth-map-pin"));
            openOnMapButton.getStyleClass().add("open-on-map-button");
            openOnMapButton.setMaxWidth(Double.MAX_VALUE);
            if (openOnMapRow != null) {
                openOnMapRow.getChildren().add(openOnMapButton);
                javafx.scene.layout.HBox.setHgrow(openOnMapButton, javafx.scene.layout.Priority.ALWAYS);
            }
        }
        Number id = (Number) selectedRow.get("trade_point_id");
        openOnMapButton.setOnAction(e -> mapNavigator.accept(id.intValue()));
        if (openOnMapRow != null) {
            openOnMapRow.setVisible(true);
            openOnMapRow.setManaged(true);
        }
    }

    private List<Section> buildSections(User user) {
        String roleCode = user.getRole().getCode();
        List<Section> sections = new ArrayList<>();

        if ("client".equals(roleCode)) {
            sections.add(clientContracts(user));
            sections.add(clientPayments(user));
            return sections;
        }

        if ("admin".equals(roleCode)) {
            sections.add(users());
            sections.add(centers());
            sections.add(points(false));
        }

        sections.add(clients(false));
        sections.add(showings(false));
        sections.add(showingPoints(false));
        sections.add(contracts(false));
        sections.add(rentals(false));
        sections.add(monthlyCharges(false));
        sections.add(payments(false));

        if ("manager".equals(roleCode)) {
            sections.add(points(true));
        }

        return sections;
    }

    private Section users() {
        return new Section(
                "users",
                "Пользователи",
                "users",
                """
                SELECT u.user_id, u.role_id, r.code AS role_code, u.login, u.email, u.phone,
                       u.full_name, u.is_active
                FROM users u
                JOIN role r ON r.role_id = u.role_id
                ORDER BY u.user_id
                """,
                List.of(),
                List.of(
                        col("user_id", "ID", 70),
                        col("role_code", "Роль", 100),
                        col("login", "Логин", 140),
                        col("email", "Email", 200),
                        col("full_name", "ФИО", 220),
                        col("is_active", "Активен", 90)
                ),
                List.of(
                        refField("role_id", "Роль", true, true,
                                "SELECT role_id AS value, name || ' (' || code || ')' AS label FROM role ORDER BY role_id"),
                        field("login", "Логин", FieldType.TEXT, true, true, "login"),
                        field("email", "Email", FieldType.TEXT, true, true, "email@example.com"),
                        field("phone", "Телефон", FieldType.TEXT, false, false, "+7..."),
                        field("full_name", "ФИО", FieldType.TEXT, true, true, "ФИО"),
                        field("password_hash", "Пароль", FieldType.PASSWORD, true, false, "только для создания/смены"),
                        field("is_active", "Активен", FieldType.BOOLEAN, false, false, "")
                ),
                List.of("user_id"),
                false,
                "UPDATE users SET is_active = FALSE WHERE user_id = ?"
        );
    }

    private Section clients(boolean readOnly) {
        return new Section(
                "clients",
                "Клиенты",
                "client",
                """
                SELECT c.client_id, c.user_id, u.login, c.company_name, c.legal_address,
                       c.bank_details, c.status
                FROM client c
                JOIN users u ON u.user_id = c.user_id
                ORDER BY c.company_name
                """,
                List.of(),
                List.of(
                        col("client_id", "ID", 70),
                        col("user_id", "User ID", 80),
                        col("login", "Логин", 130),
                        col("company_name", "Компания", 240),
                        col("status", "Статус", 126)
                ),
                List.of(
                        refField("user_id", "Пользователь", true, true,
                                """
                                SELECT u.user_id AS value, u.full_name || ' / ' || u.login AS label
                                FROM users u
                                JOIN role r ON r.role_id = u.role_id
                                WHERE r.code = 'client'
                                ORDER BY u.full_name
                                """),
                        field("company_name", "Компания", FieldType.TEXT, true, true, "ООО ..."),
                        field("legal_address", "Юр. адрес", FieldType.TEXT_LONG, false, false, ""),
                        field("bank_details", "Реквизиты", FieldType.TEXT_LONG, false, false, ""),
                        choiceField("status", "Статус", true, true, clientStatuses())
                ),
                List.of("client_id"),
                readOnly,
                null
        );
    }

    private Section centers() {
        return new Section(
                "centers",
                "Торговые центры",
                "shopping_center",
                "SELECT shopping_center_id, name, address, image_url, map_path FROM shopping_center ORDER BY shopping_center_id",
                List.of(),
                List.of(
                        col("shopping_center_id", "ID", 70),
                        col("name", "Название", 200),
                        col("address", "Адрес", 260),
                        col("map_path", "Карта", 130)
                ),
                List.of(
                        field("name", "Название", FieldType.TEXT, true, true, "Название ТЦ"),
                        field("address", "Адрес", FieldType.TEXT_LONG, true, true, "Адрес"),
                        field("image_url", "Картинка (URL)", FieldType.TEXT, false, false, "https://..."),
                        field("map_path", "Путь к карте", FieldType.TEXT, false, false, "maps/center-1 (пусто = нет карты)")
                ),
                List.of("shopping_center_id"),
                false,
                null
        );
    }

    private Section points(boolean readOnly) {
        return new Section(
                "points",
                "Торговые точки",
                "trade_point",
                """
                SELECT tp.trade_point_id, tp.shopping_center_id, sc.name AS center_name, tp.point_code,
                       tp.floor, tp.area_m2, tp.has_air_conditioner, tp.current_daily_rate, tp.is_active, tp.image_url,
                       CASE
                           WHEN NOT tp.is_active THEN 'unavailable'
                           WHEN EXISTS (SELECT 1 FROM contract_rental cr
                                        WHERE cr.point_id = tp.trade_point_id AND cr.status = 'active'
                                          AND CURRENT_DATE BETWEEN cr.date_from AND cr.date_to) THEN 'occupied'
                           ELSE 'free'
                       END AS status
                FROM trade_point tp
                JOIN shopping_center sc ON sc.shopping_center_id = tp.shopping_center_id
                ORDER BY sc.name, tp.floor, tp.point_code
                """,
                List.of(),
                List.of(
                        col("trade_point_id", "ID", 70),
                        col("center_name", "ТЦ", 150),
                        col("point_code", "Точка", 100),
                        col("floor", "Этаж", 80),
                        col("area_m2", "Площадь", 100),
                        col("current_daily_rate", "Ставка", 120),
                        col("status", "Статус", 126),
                        col("is_active", "Открыта", 90)
                ),
                List.of(
                        refField("shopping_center_id", "Торговый центр", true, true,
                                "SELECT shopping_center_id AS value, name AS label FROM shopping_center ORDER BY name"),
                        field("point_code", "Код точки", FieldType.TEXT, true, true, "A-101"),
                        field("floor", "Этаж", FieldType.INT, true, true, "1"),
                        field("area_m2", "Площадь", FieldType.DECIMAL, true, true, "45.00"),
                        field("has_air_conditioner", "Кондиционер", FieldType.BOOLEAN, false, false, ""),
                        field("current_daily_rate", "Ставка/день", FieldType.DECIMAL, true, true, "5500.00"),
                        field("is_active", "Открыта (доступна)", FieldType.BOOLEAN, false, false, ""),
                        field("image_url", "Картинка (URL)", FieldType.TEXT, false, false, "https://...")
                ),
                List.of("trade_point_id"),
                readOnly,
                null
        );
    }

    private Section showings(boolean readOnly) {
        return new Section(
                "showings",
                "Показы",
                "showing",
                """
                SELECT s.showing_id, s.client_id, c.company_name, s.manager_user_id,
                       u.full_name AS manager_name, s.shown_at, s.result, s.comment
                FROM showing s
                JOIN client c ON c.client_id = s.client_id
                JOIN users u ON u.user_id = s.manager_user_id
                ORDER BY s.shown_at DESC
                """,
                List.of(),
                List.of(
                        col("showing_id", "ID", 70),
                        col("company_name", "Клиент", 200),
                        col("manager_name", "Менеджер", 180),
                        col("shown_at", "Дата", 170),
                        col("result", "Результат", 150)
                ),
                List.of(
                        refField("client_id", "Клиент", true, true,
                                "SELECT client_id AS value, company_name AS label FROM client ORDER BY company_name"),
                        refField("manager_user_id", "Менеджер", true, true,
                                """
                                SELECT u.user_id AS value, u.full_name || ' / ' || u.login AS label
                                FROM users u
                                JOIN role r ON r.role_id = u.role_id
                                WHERE r.code IN ('manager', 'admin') AND u.is_active = TRUE
                                ORDER BY u.full_name
                                """),
                        field("shown_at", "Дата/время", FieldType.TIMESTAMP, false, true, "2026-06-07T12:00"),
                        choiceField("result", "Результат", false, false, showingResults()),
                        field("comment", "Комментарий", FieldType.TEXT_LONG, false, false, "")
                ),
                List.of("showing_id"),
                readOnly,
                null
        );
    }

    private Section showingPoints(boolean readOnly) {
        return new Section(
                "showing_points",
                "Точки показов",
                "showing_point",
                """
                SELECT sp.showing_id, sp.point_id, tp.point_code, tp.floor
                FROM showing_point sp
                JOIN trade_point tp ON tp.trade_point_id = sp.point_id
                ORDER BY sp.showing_id DESC, tp.point_code
                """,
                List.of(),
                List.of(
                        col("showing_id", "Показ", 90),
                        col("point_id", "Точка ID", 90),
                        col("point_code", "Код", 100),
                        col("floor", "Этаж", 80)
                ),
                List.of(
                        refField("showing_id", "Показ", true, true,
                                """
                                SELECT s.showing_id AS value, '#' || s.showing_id || ' / ' || c.company_name || ' / ' || s.shown_at AS label
                                FROM showing s
                                JOIN client c ON c.client_id = s.client_id
                                ORDER BY s.shown_at DESC
                                """),
                        refField("point_id", "Торговая точка", true, true,
                                """
                                SELECT tp.trade_point_id AS value, sc.name || ' / этаж ' || tp.floor || ' / ' || tp.point_code AS label
                                FROM trade_point tp
                                JOIN shopping_center sc ON sc.shopping_center_id = tp.shopping_center_id
                                ORDER BY sc.name, tp.floor, tp.point_code
                                """)
                ),
                List.of("showing_id", "point_id"),
                readOnly,
                null
        );
    }

    private Section contracts(boolean readOnly) {
        return new Section(
                "contracts",
                "Договоры",
                "contract",
                """
                SELECT ct.contract_id, ct.client_id, c.company_name,
                       ct.contract_no, ct.signed_at, ct.status, ct.comment
                FROM contract ct
                JOIN client c ON c.client_id = ct.client_id
                ORDER BY ct.signed_at DESC, ct.contract_id DESC
                """,
                List.of(),
                List.of(
                        col("contract_id", "ID", 70),
                        col("contract_no", "Номер", 140),
                        col("company_name", "Клиент", 240),
                        col("signed_at", "Подписан", 120),
                        col("status", "Статус", 126)
                ),
                List.of(
                        refField("client_id", "Клиент", true, true,
                                "SELECT client_id AS value, company_name AS label FROM client ORDER BY company_name"),
                        field("contract_no", "Номер договора", FieldType.TEXT, true, true, "ML-2026-001"),
                        field("signed_at", "Дата подписания", FieldType.DATE, false, true, "2026-06-07"),
                        choiceField("status", "Статус", true, true, contractStatuses()),
                        field("comment", "Комментарий", FieldType.TEXT_LONG, false, false, "")
                ),
                List.of("contract_id"),
                readOnly,
                null
        );
    }

    private Section rentals(boolean readOnly) {
        return new Section(
                "rentals",
                "Точки в договорах",
                "contract_rental",
                """
                SELECT cr.contract_id, ct.contract_no, cr.point_id, tp.point_code, cr.date_from,
                       cr.date_to, cr.daily_rate_fixed, cr.status
                FROM contract_rental cr
                JOIN contract ct ON ct.contract_id = cr.contract_id
                JOIN trade_point tp ON tp.trade_point_id = cr.point_id
                ORDER BY cr.contract_id DESC, tp.point_code
                """,
                List.of(),
                List.of(
                        col("contract_id", "Договор", 90),
                        col("contract_no", "Номер", 140),
                        col("point_id", "Точка ID", 90),
                        col("point_code", "Код точки", 110),
                        col("date_from", "С", 120),
                        col("date_to", "По", 120),
                        col("daily_rate_fixed", "Ставка", 120),
                        col("status", "Статус", 126)
                ),
                List.of(
                        refField("contract_id", "Договор", true, true,
                                """
                                SELECT ct.contract_id AS value, ct.contract_no || ' / ' || c.company_name AS label
                                FROM contract ct
                                JOIN client c ON c.client_id = ct.client_id
                                ORDER BY ct.signed_at DESC, ct.contract_no
                                """),
                        refField("point_id", "Торговая точка", true, true,
                                """
                                SELECT tp.trade_point_id AS value, sc.name || ' / этаж ' || tp.floor || ' / ' || tp.point_code AS label
                                FROM trade_point tp
                                JOIN shopping_center sc ON sc.shopping_center_id = tp.shopping_center_id
                                ORDER BY sc.name, tp.floor, tp.point_code
                                """),
                        field("date_from", "Дата начала", FieldType.DATE, true, true, "2026-06-07"),
                        field("date_to", "Дата окончания", FieldType.DATE, true, true, "2026-12-31"),
                        field("daily_rate_fixed", "Фикс. ставка", FieldType.DECIMAL, true, true, "5500.00"),
                        choiceField("status", "Статус", true, true, rentalStatuses())
                ),
                List.of("contract_id", "point_id"),
                readOnly,
                null
        );
    }

    private Section payments(boolean readOnly) {
        return new Section(
                "payments",
                "Платежи",
                "payment",
                """
                SELECT p.payment_id, p.charge_id, ct.contract_no, tp.point_code,
                       mc.month, p.paid_at, p.amount, p.document_no, p.comment
                FROM payment p
                JOIN monthly_charges mc ON mc.charge_id = p.charge_id
                JOIN contract ct ON ct.contract_id = mc.contract_id
                JOIN trade_point tp ON tp.trade_point_id = mc.point_id
                ORDER BY p.paid_at DESC, p.payment_id DESC
                """,
                List.of(),
                List.of(
                        col("payment_id", "ID", 70),
                        col("contract_no", "Договор", 140),
                        col("point_code", "Точка", 100),
                        col("month", "Месяц", 120),
                        col("paid_at", "Оплачен", 120),
                        col("amount", "Сумма", 120),
                        col("document_no", "Документ", 130)
                ),
                List.of(
                        refField("charge_id", "Начисление", true, true,
                                """
                                SELECT mc.charge_id AS value,
                                       ct.contract_no || ' / ' || tp.point_code || ' / ' || to_char(mc.month, 'YYYY-MM') AS label
                                FROM monthly_charges mc
                                JOIN contract ct ON ct.contract_id = mc.contract_id
                                JOIN trade_point tp ON tp.trade_point_id = mc.point_id
                                ORDER BY ct.contract_no, tp.point_code, mc.month
                                """),
                        field("paid_at", "Дата оплаты", FieldType.DATE, false, true, "2026-06-07"),
                        field("amount", "Сумма", FieldType.DECIMAL, true, true, "150000.00"),
                        field("document_no", "Номер документа", FieldType.TEXT, false, false, ""),
                        field("comment", "Комментарий", FieldType.TEXT_LONG, false, false, "")
                ),
                List.of("payment_id"),
                readOnly,
                null
        );
    }

    private Section monthlyCharges(boolean readOnly) {
        return new Section(
                "charges",
                "Начисления",
                "monthly_charges",
                """
                SELECT mc.charge_id, ct.contract_no, tp.point_code, mc.month,
                       mc.amount_due, mc.status,
                       COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.charge_id = mc.charge_id), 0) AS paid
                FROM monthly_charges mc
                JOIN contract ct ON ct.contract_id = mc.contract_id
                JOIN trade_point tp ON tp.trade_point_id = mc.point_id
                ORDER BY ct.contract_no, tp.point_code, mc.month
                """,
                List.of(),
                List.of(
                        col("charge_id", "ID", 70),
                        col("contract_no", "Договор", 140),
                        col("point_code", "Точка", 100),
                        col("month", "Месяц", 120),
                        col("amount_due", "К оплате", 120),
                        col("paid", "Оплачено", 120),
                        col("status", "Статус", 126)
                ),
                List.of(
                        refField("contract_id", "Договор", true, true,
                                """
                                SELECT ct.contract_id AS value, ct.contract_no || ' / ' || c.company_name AS label
                                FROM contract ct
                                JOIN client c ON c.client_id = ct.client_id
                                ORDER BY ct.signed_at DESC, ct.contract_no
                                """),
                        refField("point_id", "Точка в договоре", true, true,
                                """
                                SELECT cr.point_id AS value, ct.contract_no || ' / ' || tp.point_code AS label
                                FROM contract_rental cr
                                JOIN contract ct ON ct.contract_id = cr.contract_id
                                JOIN trade_point tp ON tp.trade_point_id = cr.point_id
                                ORDER BY ct.contract_no, tp.point_code
                                """),
                        field("month", "Месяц (дата начала)", FieldType.DATE, true, true, "2026-06-01"),
                        field("amount_due", "Сумма к оплате", FieldType.DECIMAL, true, true, "150000.00"),
                        choiceField("status", "Статус", true, true, chargeStatuses())
                ),
                List.of("charge_id"),
                readOnly,
                null
        );
    }

    private Section clientContracts(User user) {
        Section source = contracts(true);
        return source.withSelectSql("""
                SELECT ct.contract_id, ct.client_id, c.company_name,
                       ct.contract_no, ct.signed_at, ct.status, ct.comment
                FROM contract ct
                JOIN client c ON c.client_id = ct.client_id
                WHERE c.user_id = ?
                ORDER BY ct.signed_at DESC, ct.contract_id DESC
                """, List.of(user.getUserId()));
    }

    private Section clientPayments(User user) {
        Section source = payments(true);
        return source.withSelectSql("""
                SELECT p.payment_id, p.charge_id, ct.contract_no, tp.point_code,
                       mc.month, p.paid_at, p.amount, p.document_no, p.comment
                FROM payment p
                JOIN monthly_charges mc ON mc.charge_id = p.charge_id
                JOIN contract ct ON ct.contract_id = mc.contract_id
                JOIN client c ON c.client_id = ct.client_id
                JOIN trade_point tp ON tp.trade_point_id = mc.point_id
                WHERE c.user_id = ?
                ORDER BY p.paid_at DESC, p.payment_id DESC
                """, List.of(user.getUserId()));
    }

    private ColumnDef col(String column, String label, double width) {
        return new ColumnDef(column, label, width);
    }

    private FieldDef field(
            String column,
            String label,
            FieldType type,
            boolean requiredOnCreate,
            boolean requiredOnUpdate,
            String prompt) {
        return new FieldDef(column, label, type, requiredOnCreate, requiredOnUpdate, prompt, null, List.of());
    }

    private FieldDef refField(
            String column,
            String label,
            boolean requiredOnCreate,
            boolean requiredOnUpdate,
            String referenceSql) {
        return new FieldDef(column, label, FieldType.REFERENCE, requiredOnCreate, requiredOnUpdate, "", referenceSql, List.of());
    }

    private FieldDef choiceField(
            String column,
            String label,
            boolean requiredOnCreate,
            boolean requiredOnUpdate,
            List<OptionItem> options) {
        return new FieldDef(column, label, FieldType.CHOICE, requiredOnCreate, requiredOnUpdate, "", null, options);
    }

    private List<OptionItem> clientStatuses() {
        return List.of(
                option("active", "Активен"),
                option("inactive", "Неактивен")
        );
    }

    private List<OptionItem> chargeStatuses() {
        return List.of(
                option("unpaid", "Не оплачено"),
                option("paid", "Оплачено")
        );
    }

    private List<OptionItem> showingResults() {
        return List.of(
                option("requested", "Запрошен"),
                option("interested", "Интерес есть"),
                option("refused", "Отказ"),
                option("contract_signed", "Договор подписан")
        );
    }

    private List<OptionItem> contractStatuses() {
        return List.of(
                option("draft", "Черновик"),
                option("active", "Активен"),
                option("completed", "Завершен"),
                option("terminated", "Расторгнут")
        );
    }

    private List<OptionItem> rentalStatuses() {
        return List.of(
                option("active", "Активна"),
                option("completed", "Завершена"),
                option("terminated", "Расторгнута")
        );
    }

    private OptionItem option(String value, String label) {
        return new OptionItem(value, label);
    }

    private String formatValue(String column, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "Да" : "Нет";
        }
        if (Set.of("status", "result").contains(column) && value instanceof String text) {
            return statusLabel(text);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        return Objects.toString(value, "");
    }

    private String statusLabel(String value) {
        return switch (value) {
            case "active" -> "Активен";
            case "inactive" -> "Неактивен";
            case "draft" -> "Черновик";
            case "completed" -> "Завершен";
            case "terminated" -> "Расторгнут";
            case "free" -> "Свободно";
            case "occupied" -> "Занято";
            case "unavailable" -> "Недоступно";
            case "paid" -> "Оплачено";
            case "unpaid" -> "Не оплачено";
            case "requested" -> "Запрошен";
            case "interested" -> "Интерес есть";
            case "refused" -> "Отказ";
            case "contract_signed" -> "Договор подписан";
            default -> value;
        };
    }

    private boolean isBadgeColumn(String column) {
        return Set.of("status", "result", "is_active", "has_air_conditioner").contains(column);
    }

    private void applyBadgeCellFactory(TableColumn<LinkedHashMap<String, Object>, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(value);
                pill.getStyleClass().setAll("cell-badge", badgeClass(value));
                setGraphic(pill);
                setText(null);
            }
        });
    }

    private String badgeClass(String displayValue) {
        return switch (displayValue) {
            case "Да", "Свободно", "Активен", "Активна", "Завершен", "Завершена",
                 "Интерес есть", "Договор подписан", "Оплачено" -> "cell-badge-green";
            case "Занято", "Отказ", "Расторгнут", "Расторгнута" -> "cell-badge-red";
            case "Недоступно", "Черновик", "Запрошен", "Не оплачено" -> "cell-badge-amber";
            case "Нет", "Неактивен" -> "cell-badge-muted";
            default -> "cell-badge-neutral";
        };
    }

    private String formatInputValue(FieldDef field, Object value) {
        if (value == null) {
            return "";
        }
        if (field.type() == FieldType.TIMESTAMP && value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (field.type() == FieldType.DATE && value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        return Objects.toString(value, "");
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private enum FieldType {
        TEXT,
        TEXT_LONG,
        INT,
        DECIMAL,
        BOOLEAN,
        DATE,
        TIMESTAMP,
        PASSWORD,
        REFERENCE,
        CHOICE
    }

    private record ColumnDef(String column, String label, double width) {}

    private record FieldDef(
            String column,
            String label,
            FieldType type,
            boolean requiredOnCreate,
            boolean requiredOnUpdate,
            String prompt,
            String referenceSql,
            List<OptionItem> options
    ) {}

    private record OptionItem(Object value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record RowFilter(String column, String value, String label) {}

    private record Section(
            String id,
            String title,
            String table,
            String selectSql,
            List<Object> selectParams,
            List<ColumnDef> columns,
            List<FieldDef> fields,
            List<String> pkColumns,
            boolean readOnly,
            String deleteSql
    ) {
        Section withSelectSql(String sql, List<Object> params) {
            return new Section(id, title, table, sql, params, columns, fields, pkColumns, readOnly, deleteSql);
        }
    }
}
