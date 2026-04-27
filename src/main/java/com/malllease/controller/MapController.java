package com.malllease.controller;

import com.malllease.dao.ClientDao;
import com.malllease.dao.ShoppingCenterDao;
import com.malllease.dao.ShowingDao;
import com.malllease.dao.TradePointDao;
import com.malllease.dao.UserDao;
import com.malllease.model.Client;
import com.malllease.model.FloorPlan;
import com.malllease.model.ShoppingCenter;
import com.malllease.model.Showing;
import com.malllease.model.TradePoint;
import com.malllease.model.User;
import com.malllease.ui.RangeSliderControl;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.Cursor;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MapController {

    private static final Color COLOR_FREE = Color.web("#35B96F");
    private static final Color COLOR_OCCUPIED = Color.web("#F05E73");
    private static final Color COLOR_UNAVAILABLE = Color.web("#F5C451");
    private static final Color COLOR_SELECTED = Color.web("#3B82F6");
    private static final Color COLOR_SELF = Color.web("#8B5CF6");
    private static final Color COLOR_FREE_FILL = Color.web("#CFF4DA");
    private static final Color COLOR_OCCUPIED_FILL = Color.web("#FFD0D7");
    private static final Color COLOR_UNAVAILABLE_FILL = Color.web("#FEF3C7");
    private static final Color COLOR_SELECTED_FILL = Color.web("#DCE8FF");
    private static final Color COLOR_SELF_FILL = Color.web("#EDE3FF");
    private static final Color COLOR_HOVER_OVERLAY = Color.rgb(37, 99, 235, 0.12);
    private static final Color COLOR_FILTERED_FILL = Color.web("#EEF2F7");
    private static final Color COLOR_FILTERED_BORDER = Color.web("#CBD5E1");
    private static final Color COLOR_FILTERED_TEXT = Color.web("#94A3B8");
    private static final Color COLOR_WALL = Color.web("#B7C1D1");
    private static final Color COLOR_CORRIDOR = Color.web("#F1F5FA");
    private static final Color COLOR_BUILDING_BG = Color.web("#FFFFFF");
    private static final Color COLOR_CANVAS_BG = Color.web("#EEF3FA");
    private static final Color COLOR_CORRIDOR_TEXT = Color.web("#94A3B8");

    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 3.0;
    private static final LocalTime SHOWING_DAY_START = LocalTime.of(10, 0);
    private static final LocalTime SHOWING_DAY_END = LocalTime.of(18, 0);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private ComboBox<ShoppingCenter> centerComboBox;
    @FXML private ImageView centerImageView;
    @FXML private ImageView pointImageView;
    @FXML private VBox noMapPanel;
    @FXML private VBox leftPanel;
    @FXML private VBox rightColumn;
    @FXML private ImageView noMapHeaderImage;
    @FXML private Label noMapCenterName;
    @FXML private Label noMapCountLabel;
    @FXML private VBox noMapListBox;
    @FXML private javafx.scene.control.TextField pointSearchField;
    @FXML private javafx.scene.control.ScrollPane searchResultsScroll;
    @FXML private VBox searchResultsBox;
    @FXML private HBox priceRangeContainer;
    @FXML private HBox areaRangeContainer;
    @FXML private HBox statusDropdownContainer;
    @FXML private CheckBox acOnlyCheckBox;
    @FXML private Label filterResultLabel;
    @FXML private DatePicker availFromPicker;
    @FXML private DatePicker availToPicker;
    @FXML private Label availabilityHintLabel;

    private RangeSliderControl priceRangeSlider;
    private RangeSliderControl areaRangeSlider;
    private MenuButton statusMenuButton;
    private final Map<String, CheckMenuItem> statusMenuItems = new LinkedHashMap<>();
    @FXML private ComboBox<Integer> floorComboBox;
    @FXML private HBox searchBar;
    @FXML private HBox bottomBar;
    @FXML private VBox searchResultsCard;
    @FXML private StackPane mapStage;
    @FXML private Canvas mapCanvas;
    @FXML private Pane mapPane;
    @FXML private VBox pointInfoCard;
    @FXML private Label inspectorEmptyLabel;
    @FXML private VBox pointDetailsBox;
    @FXML private Label pointCodeLabel;
    @FXML private Label pointFloorLabel;
    @FXML private Label pointAreaLabel;
    @FXML private Label pointRateLabel;
    @FXML private Label pointStatusLabel;
    @FXML private Label pointAcLabel;
    @FXML private Label periodPriceLabel;
    @FXML private VBox showingSchedulerBox;
    @FXML private DatePicker showingDatePicker;
    @FXML private ComboBox<LocalTime> showingSlotBox;
    @FXML private Label showingSlotHintLabel;
    @FXML private Label zoomLabel;
    @FXML private Button requestShowingButton;

    private final ShoppingCenterDao centerDao = new ShoppingCenterDao();
    private final TradePointDao tradePointDao = new TradePointDao();
    private final ClientDao clientDao = new ClientDao();
    private final ShowingDao showingDao = new ShowingDao();
    private final UserDao userDao = new UserDao();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.of("ru", "RU"));

    private User currentUser;
    private Integer currentClientId;
    private List<TradePoint> currentPoints;
    private Map<String, TradePoint> pointsByCode = new HashMap<>();
    private Map<Integer, Boolean> filterMatchesByPointId = new HashMap<>();
    private Map<Integer, com.malllease.model.TradePointAvailability> availabilityByPointId = new HashMap<>();
    private TradePoint selectedPoint;
    private FloorPlan.Room hoveredRoom;
    private int currentFloor = 1;
    private FloorPlan currentPlan;
    private boolean fallbackPlanActive = false;

    private double zoom = 1.0;
    private double panX = 0;
    private double panY = 0;
    private double dragStartX;
    private double dragStartY;
    private double dragStartPanX;
    private double dragStartPanY;
    private boolean isDragging = false;
    private boolean autoFitViewport = true;
    private boolean updatingSearchBox = false;
    private boolean updatingRangeControls = false;
    private Timeline viewportAnimation;
    private FadeTransition inspectorFade;

    private final DropShadow plateShadow =
            new DropShadow(BlurType.GAUSSIAN, Color.rgb(15, 23, 42, 0.16), 30, 0.0, 0, 16);
    private final DropShadow selectedGlow =
            new DropShadow(BlurType.GAUSSIAN, Color.rgb(59, 130, 246, 0.45), 24, 0.0, 0, 5);
    private boolean interacting = false;
    private PauseTransition settleTimer;

    private void markInteracting() {
        interacting = true;
        if (settleTimer == null) {
            settleTimer = new PauseTransition(javafx.util.Duration.millis(140));
            settleTimer.setOnFinished(e -> {
                interacting = false;
                drawMap();
            });
        }
        settleTimer.playFromStart();
    }

    @FXML
    private void initialize() {
        mapStage.setPickOnBounds(true);
        mapPane.setPickOnBounds(true);
        mapStage.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        mapStage.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        mapStage.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        mapStage.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        mapStage.addEventFilter(ScrollEvent.SCROLL, this::handleScroll);
        mapStage.addEventFilter(ZoomEvent.ZOOM_STARTED, event -> {
            if (!isMapSurfaceEvent(event)) {
                return;
            }
            stopViewportAnimation();
            autoFitViewport = false;
            event.consume();
        });
        mapStage.addEventFilter(ZoomEvent.ZOOM, this::handleZoom);
        mapStage.addEventFilter(ZoomEvent.ZOOM_FINISHED, event -> {
            if (isMapSurfaceEvent(event)) {
                event.consume();
            }
        });
        mapCanvas.setPickOnBounds(true);
        mapCanvas.setFocusTraversable(true);

        centerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ShoppingCenter center) {
                return center == null ? "" : center.getName();
            }
            @Override
            public ShoppingCenter fromString(String s) { return null; }
        });

        setupPointSearch();
        setupRangeSliders();
        setupStatusDropdown();
        setupAcFilter();
        setupAvailabilityFilter();
        setupShowingScheduler();

        mapPane.widthProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());
        mapPane.heightProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());

        hidePointInfo();
        setupFloorComboBox();
        Platform.runLater(() -> {
            resizeCanvas();
            centerOverlay(searchBar);
            centerOverlay(searchResultsCard);
            centerOverlay(bottomBar);
        });
    }

    private void setupFloorComboBox() {
        if (floorComboBox == null) return;
        floorComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Integer floor) {
                return floor == null ? "" : "Этаж " + floor;
            }
            @Override public Integer fromString(String s) { return null; }
        });
        floorComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV != currentFloor) {
                currentFloor = newV;
                loadFloor();
            }
        });
    }

    private void centerOverlay(Node overlay) {
        if (overlay == null || !(overlay.getParent() instanceof javafx.scene.layout.AnchorPane parent)) {
            return;
        }
        Runnable apply = () -> {
            double parentW = parent.getWidth();
            double w = overlay.getBoundsInLocal().getWidth();
            if (w <= 0 && overlay instanceof javafx.scene.layout.Region r) {
                w = r.getPrefWidth();
            }
            javafx.scene.layout.AnchorPane.setLeftAnchor(overlay, Math.max(0, (parentW - w) / 2));
        };
        apply.run();
        parent.widthProperty().addListener((o, a, b) -> apply.run());
        if (overlay instanceof javafx.scene.layout.Region r) {
            r.widthProperty().addListener((o, a, b) -> apply.run());
        }
    }

    private void setupPointSearch() {
        if (pointSearchField == null) {
            return;
        }
        pointSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingSearchBox) {
                return;
            }
            updatePointSearchOptions(newValue);
        });
        pointSearchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                pointSearchField.clear();
                hideSearchResults();
            }
        });
    }

    private void setupRangeSliders() {
        priceRangeSlider = new RangeSliderControl(0, 50000);
        priceRangeSlider.getStyleClass().add("rs-price");
        priceRangeSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(priceRangeSlider, javafx.scene.layout.Priority.ALWAYS);
        priceRangeSlider.lowValueProperty().addListener((obs, o, n) -> { if (!updatingRangeControls) applyFilters(); });
        priceRangeSlider.highValueProperty().addListener((obs, o, n) -> { if (!updatingRangeControls) applyFilters(); });
        if (priceRangeContainer != null) priceRangeContainer.getChildren().add(priceRangeSlider);

        areaRangeSlider = new RangeSliderControl(0, 500);
        areaRangeSlider.getStyleClass().add("rs-area");
        areaRangeSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(areaRangeSlider, javafx.scene.layout.Priority.ALWAYS);
        areaRangeSlider.lowValueProperty().addListener((obs, o, n) -> { if (!updatingRangeControls) applyFilters(); });
        areaRangeSlider.highValueProperty().addListener((obs, o, n) -> { if (!updatingRangeControls) applyFilters(); });
        if (areaRangeContainer != null) areaRangeContainer.getChildren().add(areaRangeSlider);
    }

    private void setupStatusDropdown() {
        statusMenuButton = new MenuButton("Все статусы");
        statusMenuButton.getStyleClass().add("status-dropdown");

        String[][] statuses = {
            {"free",        "Свободно"},
            {"occupied",    "Занято"},
            {"unavailable", "Недоступно"}
        };
        for (String[] s : statuses) {
            CheckMenuItem item = new CheckMenuItem(s[1]);
            item.getStyleClass().add("status-check-item");
            item.selectedProperty().addListener((obs, o, n) -> onStatusSelectionChanged());
            statusMenuItems.put(s[0], item);
            statusMenuButton.getItems().add(item);
        }

        statusMenuButton.setMaxWidth(Double.MAX_VALUE);
        if (statusDropdownContainer != null) {
            statusDropdownContainer.getChildren().add(statusMenuButton);
            HBox.setHgrow(statusMenuButton, javafx.scene.layout.Priority.ALWAYS);
        }
    }

    private void onStatusSelectionChanged() {
        Set<String> selected = getSelectedStatuses();
        updateStatusMenuLabel(selected);
        applyFilters();
    }

    private Set<String> getSelectedStatuses() {
        return statusMenuItems.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void updateStatusMenuLabel(Set<String> selected) {
        if (statusMenuButton == null) return;
        if (selected.isEmpty()) {
            statusMenuButton.setText("Все статусы");
        } else if (selected.size() == 1) {
            String key = selected.iterator().next();
            statusMenuButton.setText(statusMenuItems.get(key).getText());
        } else {
            statusMenuButton.setText(selected.size() + " статуса");
        }
    }

    private void setupAcFilter() {
        if (acOnlyCheckBox != null) {
            acOnlyCheckBox.selectedProperty().addListener((obs, o, n) -> applyFilters());
        }
    }

    private void setupAvailabilityFilter() {
        if (availFromPicker == null || availToPicker == null) return;
        availFromPicker.valueProperty().addListener((obs, o, n) -> refreshAvailability());
        availToPicker.valueProperty().addListener((obs, o, n) -> refreshAvailability());
    }

    private void refreshAvailability() {
        availabilityByPointId.clear();
        ShoppingCenter center = centerComboBox == null ? null : centerComboBox.getValue();
        java.time.LocalDate from = availFromPicker == null ? null : availFromPicker.getValue();
        java.time.LocalDate to   = availToPicker   == null ? null : availToPicker.getValue();

        if (center == null || from == null || to == null) {
            if (availabilityHintLabel != null) {
                availabilityHintLabel.setText("Укажите обе даты, чтобы увидеть доступность");
            }
            drawMap();
            return;
        }
        if (from.isAfter(to)) {
            if (availabilityHintLabel != null) {
                availabilityHintLabel.setText("Дата «с» должна быть не позже даты «по»");
            }
            drawMap();
            return;
        }

        int clientId = currentClientId == null ? -1 : currentClientId;
        try {
            for (var a : tradePointDao.findAvailability(
                    center.getShoppingCenterId(), currentFloor, from, to, clientId)) {
                availabilityByPointId.put(a.getTradePointId(), a);
            }
            long free = availabilityByPointId.values().stream()
                    .filter(a -> a.getStatus() == com.malllease.model.TradePointAvailability.Status.FREE)
                    .count();
            long self = availabilityByPointId.values().stream()
                    .filter(a -> a.getStatus() == com.malllease.model.TradePointAvailability.Status.OCCUPIED_BY_SELF)
                    .count();
            if (availabilityHintLabel != null) {
                StringBuilder sb = new StringBuilder("Свободно на этаже: " + free);
                if (self > 0) {
                    sb.append(" · ваших: ").append(self);
                }
                availabilityHintLabel.setText(sb.toString());
            }
        } catch (Exception e) {
            if (availabilityHintLabel != null) {
                availabilityHintLabel.setText("Не удалось получить доступность: " + e.getMessage());
            }
        }
        drawMap();
    }

    private void updateRangeControls() {
        if (currentPoints == null || currentPoints.isEmpty()
                || priceRangeSlider == null || areaRangeSlider == null) {
            return;
        }

        double maxPrice = currentPoints.stream()
                .map(p -> p.getCurrentDailyRate().doubleValue())
                .max(Double::compareTo).orElse(50000.0);
        double maxArea = currentPoints.stream()
                .map(p -> p.getAreaM2().doubleValue())
                .max(Double::compareTo).orElse(500.0);

        updatingRangeControls = true;
        try {
            priceRangeSlider.reset(0, Math.ceil(maxPrice));
            areaRangeSlider.reset(0, Math.ceil(maxArea));
        } finally {
            updatingRangeControls = false;
        }
    }

    private void setupShowingScheduler() {
        if (showingDatePicker != null) {
            showingDatePicker.setValue(LocalDate.now().plusDays(1));
            showingDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> refreshShowingSlots());
        }

        if (showingSlotBox != null) {
            showingSlotBox.setConverter(new StringConverter<>() {
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
    }

    public void initUser(User user) {
        this.currentUser = user;
        this.currentClientId = null;
        if (user != null && user.getRole() != null && "client".equals(user.getRole().getCode())) {
            try {
                clientDao.findByUserId(user.getUserId())
                        .ifPresent(client -> this.currentClientId = client.getClientId());
            } catch (Exception ignored) {
            }
        }
        loadShoppingCenters();
    }

    public void focusOnPoint(int tradePointId) {
        try {
            tradePointDao.findById(tradePointId).ifPresent(tp -> {
                centerComboBox.getItems().stream()
                        .filter(c -> c.getShoppingCenterId() == tp.getShoppingCenterId())
                        .findFirst()
                        .ifPresent(center -> {
                            centerComboBox.getSelectionModel().select(center);
                            handleCenterChange();
                            currentFloor = tp.getFloor();
                            if (floorComboBox != null) floorComboBox.setValue(tp.getFloor());
                            loadFloor();
                            TradePoint loaded = pointsByCode.get(tp.getPointCode());
                            if (loaded != null) {
                                selectPoint(loaded, true);
                            }
                        });
            });
        } catch (Exception e) {
        }
    }

    private void loadShoppingCenters() {
        try {
            List<ShoppingCenter> centers = centerDao.findAll();
            centerComboBox.setItems(FXCollections.observableArrayList(centers));
            if (!centers.isEmpty()) {
                centerComboBox.getSelectionModel().selectFirst();
                handleCenterChange();
            }
        } catch (Exception e) {
        }
    }

    @FXML
    private void handleCenterChange() {
        ShoppingCenter selected = centerComboBox.getValue();
        if (selected == null) return;

        loadImage(centerImageView, selected.getImageUrl());

        try {
            List<Integer> floors = tradePointDao.findDistinctFloors(selected.getShoppingCenterId());
            if (!floors.isEmpty()) {
                currentFloor = floors.get(0);
                buildFloorTabs(floors);
                loadFloor();
            }
        } catch (Exception e) {
        }
    }

    private void applyListMode(ShoppingCenter center, boolean listMode) {
        setVisibleManaged(mapStage, !listMode);
        setVisibleManaged(bottomBar, !listMode);
        setVisibleManaged(noMapPanel, listMode);
        if (listMode) {
            setVisibleManaged(rightColumn, false);
            if (noMapCenterName != null) {
                noMapCenterName.setText(center == null ? "" : center.getName());
            }
            loadImage(noMapHeaderImage, center == null ? null : center.getImageUrl());
        } else {
            setVisibleManaged(rightColumn, true);
        }
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean on) {
        if (node == null) return;
        node.setVisible(on);
        node.setManaged(on);
    }

    private void applyRoundedClip(ImageView view) {
        double w = view.getFitWidth();
        double h = view.getFitHeight();
        if (w <= 0 || h <= 0) return;
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(w, h);
        double arc = Math.min(28, Math.min(w, h) * 0.5);
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        view.setClip(clip);
    }

    private void loadImage(ImageView view, String url) {
        if (view == null) return;
        if (url == null || url.isBlank()) {
            view.setImage(null);
            view.setVisible(false);
            view.setManaged(false);
            return;
        }
        try {
            Image image = new Image(url, true);
            image.errorProperty().addListener((obs, was, isError) -> {
                if (isError) {
                    view.setVisible(false);
                    view.setManaged(false);
                }
            });
            view.setImage(image);
            view.setVisible(true);
            view.setManaged(true);
            applyRoundedClip(view);
        } catch (Exception e) {
            view.setImage(null);
            view.setVisible(false);
            view.setManaged(false);
        }
    }

    @FXML
    private void handleZoomIn() {
        autoFitViewport = false;
        zoomAt(mapCanvas.getWidth() / 2, mapCanvas.getHeight() / 2, 1.18);
    }

    @FXML
    private void handleZoomOut() {
        autoFitViewport = false;
        zoomAt(mapCanvas.getWidth() / 2, mapCanvas.getHeight() / 2, 0.84);
    }

    @FXML
    private void handleZoomReset() {
        autoFitViewport = true;
        resetViewport();
    }

    private void handleZoom(ZoomEvent event) {
        if (!isMapSurfaceEvent(event)) {
            return;
        }
        if (event.getZoomFactor() == 0) {
            return;
        }
        autoFitViewport = false;
        Point2D point = mapPoint(event.getSceneX(), event.getSceneY());
        zoomAt(point.getX(), point.getY(), event.getZoomFactor());
        event.consume();
    }

    private void zoomAt(double viewportX, double viewportY, double factor) {
        stopViewportAnimation();
        double oldZoom = zoom;
        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        if (Math.abs(newZoom - oldZoom) < 0.0001) {
            return;
        }

        panX = viewportX - (viewportX - panX) * (newZoom / oldZoom);
        panY = viewportY - (viewportY - panY) * (newZoom / oldZoom);
        zoom = newZoom;
        updateZoomLabel();
        markInteracting();
        drawMap();
    }

    private void setViewport(double newZoom, double newPanX, double newPanY) {
        zoom = newZoom;
        panX = newPanX;
        panY = newPanY;
        updateZoomLabel();
        drawMap();
    }

    private void animateViewport(double targetZoom, double targetPanX, double targetPanY, int durationMs) {
        stopViewportAnimation();

        double startZoom = zoom;
        double startPanX = panX;
        double startPanY = panY;
        int frames = 14;
        viewportAnimation = new Timeline();
        for (int i = 1; i <= frames; i++) {
            double progress = i / (double) frames;
            double eased = 1 - Math.pow(1 - progress, 3);
            viewportAnimation.getKeyFrames().add(new KeyFrame(
                    javafx.util.Duration.millis(durationMs * progress),
                    event -> setViewport(
                            interpolate(startZoom, targetZoom, eased),
                            interpolate(startPanX, targetPanX, eased),
                            interpolate(startPanY, targetPanY, eased)
                    )
            ));
        }
        viewportAnimation.play();
    }

    private void stopViewportAnimation() {
        if (viewportAnimation != null) {
            viewportAnimation.stop();
            viewportAnimation = null;
        }
    }

    private double interpolate(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            zoomLabel.setText((int)(zoom * 100) + "%");
        }
    }

    private void buildFloorTabs(List<Integer> floors) {
        if (floorComboBox == null) return;
        floorComboBox.setItems(FXCollections.observableArrayList(floors));
        floorComboBox.setValue(currentFloor);
    }

    private void loadFloor() {
        ShoppingCenter center = centerComboBox.getValue();
        if (center == null) return;

        autoFitViewport = true;
        updateZoomLabel();

        try {
            currentPoints = tradePointDao.findByShoppingCenterAndFloor(
                    center.getShoppingCenterId(), currentFloor);
            pointsByCode.clear();
            for (TradePoint tp : currentPoints) {
                pointsByCode.put(tp.getPointCode(), tp);
            }

            Optional<FloorPlan> loadedPlan = FloorPlan.load(center.getShoppingCenterId(), currentFloor);
            currentPlan = loadedPlan.orElse(null);
            fallbackPlanActive = currentPlan == null;
            applyListMode(center, fallbackPlanActive);
            selectedPoint = null;
            hoveredRoom = null;
            clearPointSearch();
            hidePointInfo();
            updateRangeControls();
            applyFilters();
            refreshAvailability();
            resetViewport();
        } catch (Exception e) {
            currentPoints = List.of();
            pointsByCode.clear();
            filterMatchesByPointId.clear();
            currentPlan = null;
            fallbackPlanActive = false;
            clearPointSearch();
            populatePointList();
            drawMap();
        }
    }

    private void resizeCanvas() {
        if (mapPane == null || mapCanvas == null) {
            return;
        }

        double width = Math.max(480, mapPane.getWidth());
        double height = Math.max(360, mapPane.getHeight());
        mapCanvas.setWidth(width);
        mapCanvas.setHeight(height);

        if (autoFitViewport) {
            resetViewport();
        } else {
            drawMap();
        }
    }

    private void resetViewport() {
        if (currentPlan == null || mapCanvas == null) {
            drawMap();
            return;
        }

        double viewportWidth = Math.max(1, mapCanvas.getWidth());
        double viewportHeight = Math.max(1, mapCanvas.getHeight());
        PlanBounds bounds = computePlanBounds();
        double planWidth = Math.max(1, bounds.width());
        double planHeight = Math.max(1, bounds.height());

        double leftInset = 360, rightInset = 400, topInset = 90, bottomInset = 110;
        double availW = Math.max(80, viewportWidth - leftInset - rightInset);
        double availH = Math.max(80, viewportHeight - topInset - bottomInset);

        double targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM,
                Math.min(availW / planWidth, availH / planHeight) * 0.92));
        double targetPanX = leftInset + (availW - planWidth * targetZoom) / 2 - bounds.minX() * targetZoom;
        double targetPanY = topInset + (availH - planHeight * targetZoom) / 2 - bounds.minY() * targetZoom;

        if (Math.abs(zoom - 1.0) < 0.001 && Math.abs(panX) < 0.001 && Math.abs(panY) < 0.001) {
            setViewport(targetZoom, targetPanX, targetPanY);
        } else {
            animateViewport(targetZoom, targetPanX, targetPanY, 220);
        }
    }

    private void drawMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        double w = mapCanvas.getWidth();
        double h = mapCanvas.getHeight();

        drawCanvasBackdrop(gc, w, h);

        if (currentPlan == null) {
            drawEmptyMap(gc, w, h);
            return;
        }

        gc.save();
        gc.translate(panX, panY);
        gc.scale(zoom, zoom);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        drawBuilding(gc);
        drawCorridors(gc);
        drawWalls(gc);
        drawAmenities(gc);
        drawRooms(gc);

        gc.restore();
    }

    private void drawCanvasBackdrop(GraphicsContext gc, double w, double h) {
        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#F1F5FB")),
                new javafx.scene.paint.Stop(1, Color.web("#E7EDF6"))));
        gc.fillRect(0, 0, w, h);
    }

    private void drawBuilding(GraphicsContext gc) {
        List<double[]> outline = currentPlan.getBuildingOutline();
        if (outline.isEmpty()) return;

        double[] xpts = outline.stream().mapToDouble(p -> p[0]).toArray();
        double[] ypts = outline.stream().mapToDouble(p -> p[1]).toArray();

        if (!interacting) {
            gc.save();
            gc.setEffect(plateShadow);
            gc.setFill(COLOR_BUILDING_BG);
            gc.fillPolygon(xpts, ypts, outline.size());
            gc.restore();
        } else {
            gc.setFill(COLOR_BUILDING_BG);
            gc.fillPolygon(xpts, ypts, outline.size());
        }

        gc.setStroke(COLOR_WALL);
        gc.setLineWidth(2.4);
        gc.strokePolygon(xpts, ypts, outline.size());
    }

    private void drawEmptyMap(GraphicsContext gc, double width, double height) {
        double cardW = 440, cardH = 110;
        double cx = width / 2 - cardW / 2;
        double cy = height / 2 - cardH / 2;
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillRoundRect(cx, cy, cardW, cardH, 18, 18);
        gc.setStroke(Color.web("#D8E0EC"));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(cx, cy, cardW, cardH, 18, 18);
        gc.setFill(Color.web("#0F172A"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        boolean haveAnyPoint = currentPoints != null && !currentPoints.isEmpty();
        String title = haveAnyPoint
                ? "Карта этажа пока не разработана"
                : "Нет данных для этажа";
        gc.fillText(title, width / 2, height / 2 - 6);
        gc.setFill(Color.web("#64748B"));
        gc.setFont(Font.font("System", FontWeight.NORMAL, 13));
        String subtitle = haveAnyPoint
                ? "Список точек справа — выберите интересующую"
                : "Проверьте торговые точки и схему этажа";
        gc.fillText(subtitle, width / 2, height / 2 + 18);
    }

    private void drawCorridors(GraphicsContext gc) {
        for (FloorPlan.Corridor corridor : currentPlan.getCorridors()) {
            gc.setFill(COLOR_CORRIDOR);
            gc.fillRoundRect(corridor.x, corridor.y, corridor.width, corridor.height, 18, 18);
        }
    }

    private void drawWalls(GraphicsContext gc) {
        gc.setStroke(COLOR_WALL);
        gc.setLineWidth(2);
        for (double[][] wall : currentPlan.getWalls()) {
            gc.strokeLine(wall[0][0], wall[0][1], wall[1][0], wall[1][1]);
        }
    }

    private void drawAmenities(GraphicsContext gc) {
        PlanBounds bounds = computePlanBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();

        gc.setFill(Color.web("#FBFCFE"));
        gc.setStroke(Color.web("#E2E8F2"));
        gc.setLineWidth(1.6);
        gc.fillRoundRect(cx - 124, cy - 74, 248, 148, 22, 22);
        gc.strokeRoundRect(cx - 124, cy - 74, 248, 148, 22, 22);

        drawEscalator(gc, cx - 64, cy - 48, 52, 96);
        drawEscalator(gc, cx + 12, cy - 48, 52, 96);
        drawAmenityCaption(gc, "Эскалаторы", cx, cy + 64);

        drawElevators(gc, cx + 178, cy - 56, 64, 112);
        drawRestrooms(gc, cx + 178, cy + 70, 64, 44);
        drawPlant(gc, cx - 196, cy - 24);
        drawPlant(gc, cx - 182, cy + 88);
        drawBench(gc, cx - 58, cy + 172);
    }

    private void drawEscalator(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#EEF2F9"));
        gc.setStroke(Color.web("#D3DCEA"));
        gc.setLineWidth(1.4);
        gc.fillRoundRect(x, y, w, h, 10, 10);
        gc.strokeRoundRect(x, y, w, h, 10, 10);
        gc.setStroke(Color.web("#C2CCDD"));
        gc.setLineWidth(1.2);
        int steps = 7;
        for (int i = 1; i <= steps; i++) {
            double yy = y + h * i / (steps + 1.0);
            gc.strokeLine(x + 7, yy, x + w - 7, yy - 5);
        }
        gc.setStroke(Color.web("#9AA8BF"));
        gc.setLineWidth(2.0);
        double ax = x + w / 2;
        gc.strokeLine(ax - 7, y + 18, ax, y + 9);
        gc.strokeLine(ax + 7, y + 18, ax, y + 9);
    }

    private void drawElevators(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#EAF0F8"));
        gc.setStroke(Color.web("#CFD9E8"));
        gc.setLineWidth(1.4);
        gc.fillRoundRect(x, y, w, h, 12, 12);
        gc.strokeRoundRect(x, y, w, h, 12, 12);
        double midY = y + h / 2;
        gc.setStroke(Color.web("#CFD9E8"));
        gc.strokeLine(x + 8, midY, x + w - 8, midY);
        gc.setStroke(Color.web("#A8B5C9"));
        gc.setLineWidth(2.2);
        double cxMid = x + w / 2;
        gc.strokeLine(cxMid, y + 12, cxMid, midY - 12);
        gc.strokeLine(cxMid, midY + 12, cxMid, y + h - 12);
        drawAmenityCaption(gc, "Лифты", cxMid, y + h + 14);
    }

    private void drawRestrooms(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#EEF2F9"));
        gc.setStroke(Color.web("#D3DCEA"));
        gc.setLineWidth(1.4);
        gc.fillRoundRect(x, y, w, h, 10, 10);
        gc.strokeRoundRect(x, y, w, h, 10, 10);
        gc.setFill(Color.web("#8C9AB2"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("WC", x + w / 2, y + h / 2 + 6);
    }

    private void drawAmenityCaption(GraphicsContext gc, String text, double x, double y) {
        gc.setFill(COLOR_CORRIDOR_TEXT);
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x, y);
    }

    private void drawPlant(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#E7F6EA"));
        gc.fillOval(x - 16, y - 16, 32, 32);
        gc.setStroke(Color.web("#D2EBDD"));
        gc.strokeOval(x - 16, y - 16, 32, 32);
        gc.setFill(Color.web("#73B77D"));
        gc.fillOval(x - 7, y - 11, 14, 22);
        gc.fillOval(x - 11, y - 4, 22, 12);
    }

    private void drawBench(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#D7C8C1"));
        gc.fillRoundRect(x - 26, y - 7, 52, 14, 5, 5);
        gc.setFill(Color.web("#A89189"));
        gc.fillRoundRect(x - 20, y - 18, 40, 8, 4, 4);
    }

    private void drawRooms(GraphicsContext gc) {
        for (FloorPlan.Room room : currentPlan.getRooms()) {
            TradePoint tp = pointsByCode.get(room.pointCode);
            if (tp == null) continue;

            Color fillColor;
            Color borderColor;
            boolean filteredOut = !matchesCurrentFilters(tp);

            var avail = availabilityByPointId.get(tp.getTradePointId());

            if (tp.equals(selectedPoint)) {
                fillColor = COLOR_SELECTED_FILL;
                borderColor = COLOR_SELECTED;
            } else if (filteredOut) {
                fillColor = COLOR_FILTERED_FILL;
                borderColor = COLOR_FILTERED_BORDER;
            } else if (avail != null) {
                switch (avail.getStatus()) {
                    case FREE -> { fillColor = COLOR_FREE_FILL; borderColor = COLOR_FREE; }
                    case OCCUPIED_BY_SELF -> { fillColor = COLOR_SELF_FILL; borderColor = COLOR_SELF; }
                    case OCCUPIED_BY_OTHER -> { fillColor = COLOR_OCCUPIED_FILL; borderColor = COLOR_OCCUPIED; }
                    default -> { fillColor = COLOR_UNAVAILABLE_FILL; borderColor = COLOR_UNAVAILABLE; }
                }
            } else {
                switch (tp.getStatus()) {
                    case "free" -> { fillColor = COLOR_FREE_FILL; borderColor = COLOR_FREE; }
                    case "occupied" -> { fillColor = COLOR_OCCUPIED_FILL; borderColor = COLOR_OCCUPIED; }
                    default -> { fillColor = COLOR_UNAVAILABLE_FILL; borderColor = COLOR_UNAVAILABLE; }
                }
            }

            double[] xpts = room.polygon.stream().mapToDouble(p -> p[0]).toArray();
            double[] ypts = room.polygon.stream().mapToDouble(p -> p[1]).toArray();

            boolean isSelected = tp.equals(selectedPoint);
            int n = room.polygon.size();

            if (!filteredOut && !isSelected) {
                double[] sx = new double[n];
                double[] sy = new double[n];
                for (int i = 0; i < n; i++) {
                    sx[i] = xpts[i];
                    sy[i] = ypts[i] + 4;
                }
                gc.setFill(Color.rgb(15, 23, 42, 0.08));
                gc.fillPolygon(sx, sy, n);
            }

            javafx.scene.paint.Paint roomPaint = filteredOut
                    ? fillColor
                    : new javafx.scene.paint.LinearGradient(
                        0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, ((Color) fillColor).brighter()),
                        new javafx.scene.paint.Stop(1, fillColor));
            if (isSelected && !interacting) {
                gc.save();
                gc.setEffect(selectedGlow);
                gc.setFill(roomPaint);
                gc.fillPolygon(xpts, ypts, n);
                gc.restore();
            } else {
                gc.setFill(roomPaint);
                gc.fillPolygon(xpts, ypts, n);
            }

            if (room == hoveredRoom && !isSelected) {
                gc.setFill(COLOR_HOVER_OVERLAY);
                gc.fillPolygon(xpts, ypts, room.polygon.size());
            }

            gc.setStroke(borderColor);
            gc.setLineWidth(isSelected ? 4.0 : 2.4);
            gc.strokePolygon(xpts, ypts, room.polygon.size());

            gc.setTextAlign(TextAlignment.CENTER);

            gc.setFill(filteredOut && !tp.equals(selectedPoint) ? COLOR_FILTERED_TEXT : Color.web("#0F172A"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 19));
            gc.fillText(tp.getPointCode(), room.labelX, room.labelY - 16);

            gc.setFill(filteredOut && !tp.equals(selectedPoint) ? COLOR_FILTERED_TEXT : Color.web("#334155"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 13));
            gc.fillText(tp.getAreaM2() + " м²", room.labelX, room.labelY + 4);

            gc.setFill(filteredOut && !tp.equals(selectedPoint) ? COLOR_FILTERED_TEXT : Color.web("#475569"));
            gc.setFont(Font.font("System", FontWeight.NORMAL, 13));
            gc.fillText(formatCurrency(tp.getCurrentDailyRate()) + " ₽", room.labelX, room.labelY + 20);
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (!isMapSurfaceEvent(event)) {
            return;
        }
        stopViewportAnimation();
        Point2D point = mapPoint(event.getSceneX(), event.getSceneY());
        dragStartX = point.getX();
        dragStartY = point.getY();
        dragStartPanX = panX;
        dragStartPanY = panY;
        isDragging = false;
        mapCanvas.requestFocus();
        event.consume();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!isMapSurfaceEvent(event)) {
            return;
        }
        Point2D point = mapPoint(event.getSceneX(), event.getSceneY());
        double dx = point.getX() - dragStartX;
        double dy = point.getY() - dragStartY;

        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            isDragging = true;
            autoFitViewport = false;
            mapCanvas.setCursor(Cursor.MOVE);
        }

        if (isDragging) {
            interacting = true;
            panX = dragStartPanX + dx;
            panY = dragStartPanY + dy;
            drawMap();
        }
        event.consume();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (!isMapSurfaceEvent(event)) {
            return;
        }
        if (!isDragging) {
            handleClick(event);
        }
        isDragging = false;
        mapCanvas.setCursor(Cursor.DEFAULT);
        if (interacting) {
            interacting = false;
            drawMap();
        }
        event.consume();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (!isMapSurfaceEvent(event)) {
            return;
        }
        if (currentPlan == null) return;

        Point2D point = mapPoint(event.getSceneX(), event.getSceneY());
        double mx = (point.getX() - panX) / zoom;
        double my = (point.getY() - panY) / zoom;

        Optional<FloorPlan.Room> roomOpt = findInteractiveRoom(mx, my);
        FloorPlan.Room newHovered = roomOpt.orElse(null);

        if (newHovered != hoveredRoom) {
            hoveredRoom = newHovered;
            mapCanvas.setCursor(newHovered != null ? Cursor.HAND : Cursor.DEFAULT);
            drawMap();
        }
        event.consume();
    }

    private void handleScroll(ScrollEvent event) {
        if (!isMapSurfaceEvent(event)) {
            return;
        }
        autoFitViewport = false;
        double factor = Math.pow(1.0018, event.getDeltaY());
        Point2D point = mapPoint(event.getSceneX(), event.getSceneY());
        zoomAt(point.getX(), point.getY(), factor);
        event.consume();
    }

    private void handleClick(MouseEvent event) {
        if (currentPlan == null) return;

        Point2D point = mapPoint(event.getSceneX(), event.getSceneY());
        double mx = (point.getX() - panX) / zoom;
        double my = (point.getY() - panY) / zoom;

        Optional<FloorPlan.Room> roomOpt = findInteractiveRoom(mx, my);
        if (roomOpt.isPresent()) {
            FloorPlan.Room room = roomOpt.get();
            TradePoint tp = pointsByCode.get(room.pointCode);
            if (tp != null) {
                selectPoint(tp, false);
                return;
            }
        }

        selectedPoint = null;
        syncSearchSelection(null);
        hidePointInfo();
        populatePointList();
        drawMap();
    }

    private Point2D mapPoint(double sceneX, double sceneY) {
        return mapPane.sceneToLocal(sceneX, sceneY);
    }

    private boolean isMapSurfaceEvent(InputEvent event) {
        Object target = event.getTarget();
        if (!(target instanceof Node node)) {
            return false;
        }
        if (node instanceof Control && !isDescendantOf(node, mapPane)) {
            return false;
        }
        if (isDescendantOf(node, mapPane)) {
            return true;
        }

        Point2D point = null;
        if (event instanceof MouseEvent mouseEvent) {
            point = mapPoint(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        } else if (event instanceof ScrollEvent scrollEvent) {
            point = mapPoint(scrollEvent.getSceneX(), scrollEvent.getSceneY());
        } else if (event instanceof ZoomEvent zoomEvent) {
            point = mapPoint(zoomEvent.getSceneX(), zoomEvent.getSceneY());
        }
        return point != null
                && point.getX() >= 0
                && point.getY() >= 0
                && point.getX() <= mapPane.getWidth()
                && point.getY() <= mapPane.getHeight();
    }

    private boolean isDescendantOf(Node node, Node ancestor) {
        Node current = node;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Optional<FloorPlan.Room> findInteractiveRoom(double x, double y) {
        if (currentPlan == null) {
            return Optional.empty();
        }

        Optional<FloorPlan.Room> exact = currentPlan.findRoomAt(x, y)
                .filter(room -> pointsByCode.containsKey(room.pointCode));
        if (exact.isPresent()) {
            return exact;
        }

        double tolerance = Math.max(24, 46 / Math.max(zoom, 0.1));
        FloorPlan.Room closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (FloorPlan.Room room : currentPlan.getRooms()) {
            if (!pointsByCode.containsKey(room.pointCode)) {
                continue;
            }
            double distance = distanceToRoom(x, y, room);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = room;
            }
        }
        return closest != null && closestDistance <= tolerance
                ? Optional.of(closest)
                : Optional.empty();
    }

    private double distanceToRoom(double x, double y, FloorPlan.Room room) {
        if (room.containsPoint(x, y)) {
            return 0;
        }

        double distance = Double.MAX_VALUE;
        int size = room.polygon.size();
        for (int i = 0; i < size; i++) {
            double[] a = room.polygon.get(i);
            double[] b = room.polygon.get((i + 1) % size);
            distance = Math.min(distance, distanceToSegment(x, y, a[0], a[1], b[0], b[1]));
        }

        double labelDistance = Math.hypot(x - room.labelX, y - room.labelY);
        return Math.min(distance, labelDistance * 0.55);
    }

    private double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return Math.hypot(px - ax, py - ay);
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        double closestX = ax + t * dx;
        double closestY = ay + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }

    private void selectPoint(TradePoint point, boolean focusOnMap) {
        selectedPoint = point;
        showPointInfo(point);
        syncSearchSelection(point);
        if (focusOnMap) {
            focusRoom(point.getPointCode());
        }
        populatePointList();
        drawMap();
    }

    @FXML
    private void handleResetFilters() {
        updatingRangeControls = true;
        try {
            if (priceRangeSlider != null) priceRangeSlider.reset(priceRangeSlider.getMin(), priceRangeSlider.getMax());
            if (areaRangeSlider  != null) areaRangeSlider.reset(areaRangeSlider.getMin(), areaRangeSlider.getMax());
        } finally {
            updatingRangeControls = false;
        }
        if (acOnlyCheckBox != null) acOnlyCheckBox.setSelected(false);
        statusMenuItems.values().forEach(item -> item.setSelected(false));
        if (statusMenuButton != null) statusMenuButton.setText("Все статусы");
        applyFilters();
    }

    private void applyFilters() {
        filterMatchesByPointId.clear();

        if (currentPoints == null) {
            updatePointSearchOptions("");
            updateFilterResult(0, 0);
            populatePointList();
            drawMap();
            return;
        }

        FilterCriteria criteria = readFilterCriteria();
        int matched = 0;
        for (TradePoint point : currentPoints) {
            boolean matches = matchesFilterCriteria(point, criteria);
            filterMatchesByPointId.put(point.getTradePointId(), matches);
            if (matches) {
                matched++;
            }
        }

        updatePointSearchOptions(pointSearchField == null ? "" : pointSearchField.getText());
        updateFilterResult(matched, currentPoints.size());
        populatePointList();
        drawMap();
    }

    private boolean matchesCurrentFilters(TradePoint point) {
        if (point == null) {
            return false;
        }
        return filterMatchesByPointId.getOrDefault(point.getTradePointId(), true);
    }

    private void populatePointList() {
        if (noMapListBox == null) {
            return;
        }

        if (!fallbackPlanActive) {
            noMapListBox.getChildren().clear();
            if (noMapCountLabel != null) noMapCountLabel.setText("0");
            return;
        }

        noMapListBox.getChildren().clear();
        List<TradePoint> points = currentPoints == null
                ? List.of()
                : currentPoints.stream()
                .filter(this::matchesCurrentFilters)
                .sorted(Comparator.comparing(TradePoint::getPointCode))
                .toList();
        if (noMapCountLabel != null) noMapCountLabel.setText(Integer.toString(points.size()));

        if (points.isEmpty()) {
            Label empty = new Label("На этом этаже нет торговых точек");
            empty.getStyleClass().add("point-list-empty");
            empty.setWrapText(true);
            noMapListBox.getChildren().add(empty);
            return;
        }

        for (TradePoint point : points) {
            noMapListBox.getChildren().add(buildPointInfoBlock(point));
        }
    }

    private VBox buildPointInfoBlock(TradePoint point) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("point-info-block", "point-info-" + point.getStatus());
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);
        if (isSamePoint(point, selectedPoint)) {
            card.getStyleClass().add("point-info-block-active");
        }

        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label code = new Label(point.getPointCode());
        code.getStyleClass().add("point-info-code");
        Label status = new Label(statusText(point.getStatus()));
        status.getStyleClass().addAll("point-info-status", "point-info-status-" + point.getStatus());
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label price = new Label(formatCurrency(point.getCurrentDailyRate()) + " ₽/день");
        price.getStyleClass().add("point-info-price");
        header.getChildren().addAll(code, status, spacer, price);

        HBox meta = new HBox(14);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        meta.getChildren().addAll(
                metaItem("Площадь", point.getAreaM2() + " м²"),
                metaItem("Этаж", String.valueOf(point.getFloor())),
                metaItem("Кондиционер", point.isHasAirConditioner() ? "Да" : "Нет")
        );

        Button openBtn = new Button("Открыть карточку →");
        openBtn.getStyleClass().add("point-info-open-button");
        openBtn.setMaxWidth(Double.MAX_VALUE);
        openBtn.setOnAction(e -> selectPoint(point, false));

        card.getChildren().addAll(header, meta, openBtn);
        return card;
    }

    private VBox metaItem(String caption, String value) {
        VBox box = new VBox(2);
        Label c = new Label(caption);
        c.getStyleClass().add("point-info-meta-caption");
        Label v = new Label(value);
        v.getStyleClass().add("point-info-meta-value");
        box.getChildren().addAll(c, v);
        return box;
    }

    private void updatePointSearchOptions(String query) {
        if (pointSearchField == null || searchResultsBox == null) {
            return;
        }

        String normalizedQuery = normalize(query).toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            hideSearchResults();
            return;
        }

        List<TradePoint> points = currentPoints == null
                ? List.of()
                : currentPoints.stream()
                .filter(this::matchesCurrentFilters)
                .filter(point -> point.getPointCode().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || statusText(point.getStatus()).toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(TradePoint::getPointCode))
                .limit(40)
                .toList();

        searchResultsBox.getChildren().clear();
        if (points.isEmpty()) {
            hideSearchResults();
            return;
        }

        for (TradePoint point : points) {
            HBox row = new HBox();
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getStyleClass().addAll("search-result-item", "search-result-" + point.getStatus());

            Label codeLabel = new Label(point.getPointCode());
            codeLabel.getStyleClass().add("search-result-code");

            Label detailLabel = new Label("  ·  " + point.getAreaM2() + " м²"
                    + "  ·  " + formatCurrency(point.getCurrentDailyRate()) + " ₽");
            detailLabel.getStyleClass().add("search-result-detail");

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            javafx.scene.layout.Region dot = new javafx.scene.layout.Region();
            dot.getStyleClass().addAll("search-result-dot", "search-result-dot-" + point.getStatus());

            row.getChildren().addAll(codeLabel, detailLabel, spacer, dot);
            row.setOnMouseClicked(e -> {
                selectPoint(point, true);
                hideSearchResults();
            });
            searchResultsBox.getChildren().add(row);
        }
        showSearchResults();
    }

    private void showSearchResults() {
        if (searchResultsCard != null) {
            searchResultsCard.setVisible(true);
            searchResultsCard.setManaged(true);
        }
    }

    private void hideSearchResults() {
        if (searchResultsCard != null) {
            searchResultsCard.setVisible(false);
            searchResultsCard.setManaged(false);
        }
    }

    private void syncSearchSelection(TradePoint point) {
        if (pointSearchField == null) {
            return;
        }
        updatingSearchBox = true;
        if (point == null) {
            pointSearchField.clear();
        } else {
            pointSearchField.setText(point.getPointCode());
        }
        updatingSearchBox = false;
    }

    private void clearPointSearch() {
        if (pointSearchField == null) {
            return;
        }
        updatingSearchBox = true;
        pointSearchField.clear();
        if (searchResultsBox != null) {
            searchResultsBox.getChildren().clear();
        }
        hideSearchResults();
        updatingSearchBox = false;
    }

    private void updateFilterResult(int matched, int total) {
        if (filterResultLabel == null) {
            return;
        }
        if (total <= 0) {
            filterResultLabel.setText("Нет точек на этаже");
            return;
        }
        filterResultLabel.setText(matched + " из " + total + " подходят");
    }

    private FilterCriteria readFilterCriteria() {
        BigDecimal minPrice = (priceRangeSlider == null || priceRangeSlider.isAtMin())
                ? null : BigDecimal.valueOf(Math.round(priceRangeSlider.getLowValue()));
        BigDecimal maxPrice = (priceRangeSlider == null || priceRangeSlider.isAtMax())
                ? null : BigDecimal.valueOf(Math.round(priceRangeSlider.getHighValue()));
        BigDecimal minArea  = (areaRangeSlider == null || areaRangeSlider.isAtMin())
                ? null : BigDecimal.valueOf(Math.round(areaRangeSlider.getLowValue()));
        BigDecimal maxArea  = (areaRangeSlider == null || areaRangeSlider.isAtMax())
                ? null : BigDecimal.valueOf(Math.round(areaRangeSlider.getHighValue()));
        Set<String> statuses = getSelectedStatuses();
        return new FilterCriteria(
                minPrice, maxPrice, minArea, maxArea,
                acOnlyCheckBox != null && acOnlyCheckBox.isSelected(),
                statuses.isEmpty() ? null : statuses
        );
    }

    private boolean matchesFilterCriteria(TradePoint point, FilterCriteria criteria) {
        if (criteria.minPrice() != null && point.getCurrentDailyRate().compareTo(criteria.minPrice()) < 0) {
            return false;
        }
        if (criteria.maxPrice() != null && point.getCurrentDailyRate().compareTo(criteria.maxPrice()) > 0) {
            return false;
        }
        if (criteria.minArea() != null && point.getAreaM2().compareTo(criteria.minArea()) < 0) {
            return false;
        }
        if (criteria.maxArea() != null && point.getAreaM2().compareTo(criteria.maxArea()) > 0) {
            return false;
        }
        if (criteria.acOnly() && !point.isHasAirConditioner()) {
            return false;
        }
        return criteria.statuses() == null || criteria.statuses().contains(point.getStatus());
    }

    private void markInvalid(Node field, boolean invalid) {
        if (field == null) {
            return;
        }
        field.getStyleClass().remove("field-invalid");
        if (invalid) {
            field.getStyleClass().add("field-invalid");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isSamePoint(TradePoint first, TradePoint second) {
        return first != null && second != null && first.getTradePointId() == second.getTradePointId();
    }

    private void focusRoom(String pointCode) {
        if (currentPlan == null || mapCanvas == null) {
            return;
        }
        currentPlan.findRoomByPointCode(pointCode).ifPresent(room -> {
            PlanBounds bounds = boundsOf(room.polygon);
            double viewportWidth = Math.max(1, mapCanvas.getWidth());
            double viewportHeight = Math.max(1, mapCanvas.getHeight());
            double targetZoom = Math.min(MAX_ZOOM, Math.max(zoom, 1.04));
            double targetPanX = viewportWidth / 2 - bounds.centerX() * targetZoom;
            double targetPanY = viewportHeight / 2 - bounds.centerY() * targetZoom;
            autoFitViewport = false;
            animateViewport(targetZoom, targetPanX, targetPanY, 260);
        });
    }

    private String statusText(String status) {
        return switch (status) {
            case "free" -> "Свободно";
            case "occupied" -> "Занято";
            default -> "Недоступно";
        };
    }

    private void showPointInfo(TradePoint point) {
        if (fallbackPlanActive) {
            setVisibleManaged(rightColumn, true);
        }
        pointCodeLabel.setVisible(true);
        pointCodeLabel.setManaged(true);
        pointCodeLabel.setText(point.getPointCode());
        loadImage(pointImageView, point.getImageUrl());
        pointFloorLabel.setText("Этаж " + point.getFloor());
        pointAreaLabel.setText(point.getAreaM2() + " м²");
        pointRateLabel.setText(formatCurrency(point.getCurrentDailyRate()) + " ₽/день");
        pointAcLabel.setText(point.isHasAirConditioner() ? "Да" : "Нет");
        periodPriceLabel.setText(formatCurrency(point.getCurrentDailyRate().multiply(BigDecimal.valueOf(30))) + " ₽");

        var avail = availabilityByPointId.get(point.getTradePointId());
        String statusText;
        String statusClass;
        if (avail != null && avail.getStatus() == com.malllease.model.TradePointAvailability.Status.OCCUPIED_BY_SELF) {
            statusText = "Ваша аренда";
            statusClass = "status-self";
            if (avail.getOccupiedFrom() != null && avail.getOccupiedTo() != null) {
                statusText += " · до " + avail.getOccupiedTo();
            }
        } else if (avail != null && avail.getStatus() == com.malllease.model.TradePointAvailability.Status.OCCUPIED_BY_OTHER) {
            statusText = "Занята в выбранный период";
            statusClass = "status-occupied";
        } else {
            statusText = statusText(point.getStatus());
            statusClass = "status-" + point.getStatus();
        }
        pointStatusLabel.setText(statusText);
        pointStatusLabel.getStyleClass().removeAll("status-free", "status-occupied", "status-unavailable", "status-self");
        pointStatusLabel.getStyleClass().add(statusClass);

        inspectorEmptyLabel.setVisible(false);
        inspectorEmptyLabel.setManaged(false);
        pointDetailsBox.setVisible(true);
        pointDetailsBox.setManaged(true);
        boolean canRequestShowing = canCurrentUserRequestShowing(point);
        setSchedulerVisible(canRequestShowing);
        if (canRequestShowing) {
            refreshShowingSlots();
        }
        requestShowingButton.setDisable(!canRequestShowing);
        fadeInInspector();
    }

    private boolean canCurrentUserRequestShowing(TradePoint point) {
        return point != null
                && "free".equals(point.getStatus())
                && currentUser != null
                && currentUser.getRole() != null
                && "client".equals(currentUser.getRole().getCode());
    }

    private void setSchedulerVisible(boolean visible) {
        if (showingSchedulerBox != null) {
            showingSchedulerBox.setVisible(visible);
            showingSchedulerBox.setManaged(visible);
        }
    }

    private void refreshShowingSlots() {
        if (showingSlotBox == null || showingDatePicker == null || selectedPoint == null) {
            return;
        }

        showingSlotBox.getItems().clear();
        showingSlotBox.getSelectionModel().clearSelection();

        Optional<User> managerOpt = findShowingManager();
        if (managerOpt.isEmpty()) {
            setShowingSlotHint("В системе нет менеджера для обработки показа.");
            return;
        }

        LocalDate date = showingDatePicker.getValue();
        if (date == null) {
            setShowingSlotHint("Выберите дату показа.");
            return;
        }

        if (date.isBefore(LocalDate.now())) {
            setShowingSlotHint("Дата в прошлом недоступна для записи.");
            return;
        }

        try {
            int managerId = managerOpt.get().getUserId();
            List<LocalTime> busySlots = showingDao.findBusySlotTimes(managerId, date, -1);
            List<LocalTime> freeSlots = buildFreeSlots(date, busySlots);
            showingSlotBox.setItems(FXCollections.observableArrayList(freeSlots));
            if (!freeSlots.isEmpty()) {
                showingSlotBox.getSelectionModel().selectFirst();
                setShowingSlotHint("Доступно " + freeSlots.size() + " окон. Менеджер: " + managerOpt.get().getFullName());
            } else {
                setShowingSlotHint("На выбранную дату свободных окон нет.");
            }
        } catch (Exception e) {
            setShowingSlotHint("Не удалось загрузить свободные слоты. Проверьте подключение к БД.");
        }
    }

    private List<LocalTime> buildFreeSlots(LocalDate date, List<LocalTime> busySlots) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime now = LocalTime.now().plusMinutes(30);
        for (LocalTime time = SHOWING_DAY_START; time.isBefore(SHOWING_DAY_END); time = time.plusMinutes(30)) {
            if (date.equals(LocalDate.now()) && time.isBefore(now)) {
                continue;
            }
            if (!busySlots.contains(time)) {
                slots.add(time);
            }
        }
        return slots;
    }

    private void setShowingSlotHint(String text) {
        if (showingSlotHintLabel != null) {
            showingSlotHintLabel.setText(text);
        }
    }

    private Optional<User> findShowingManager() {
        return userDao.findFirstByRoleCode("manager")
                .or(() -> userDao.findFirstByRoleCode("admin"));
    }

    private void fadeInInspector() {
        if (pointInfoCard == null) {
            return;
        }
        if (inspectorFade != null) {
            inspectorFade.stop();
        }
        pointInfoCard.setOpacity(0.88);
        inspectorFade = new FadeTransition(javafx.util.Duration.millis(150), pointInfoCard);
        inspectorFade.setFromValue(0.88);
        inspectorFade.setToValue(1.0);
        inspectorFade.play();
    }

    @FXML
    private void handleClosePointInfo() {
        hidePointInfo();
        selectedPoint = null;
        syncSearchSelection(null);
        populatePointList();
        drawMap();
    }

    @FXML
    private void handleRequestShowing() {
        if (selectedPoint == null) {
            showAlert(Alert.AlertType.WARNING, "Выберите торговую точку на карте");
            return;
        }
        if (!"free".equals(selectedPoint.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Показ можно запросить только для свободной точки");
            return;
        }
        if (currentUser == null || currentUser.getRole() == null || !"client".equals(currentUser.getRole().getCode())) {
            showAlert(Alert.AlertType.INFORMATION, "Запрос показа создается из клиентской учетной записи");
            return;
        }
        if (showingDatePicker == null || showingSlotBox == null
                || showingDatePicker.getValue() == null
                || showingSlotBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Выберите свободную дату и время показа");
            return;
        }

        try {
            Optional<Client> clientOpt = clientDao.findByUserId(currentUser.getUserId());
            if (clientOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Для пользователя не найден профиль клиента");
                return;
            }

            Optional<User> managerOpt = findShowingManager();
            if (managerOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "В системе нет менеджера для обработки показа");
                return;
            }

            LocalDateTime shownAt = LocalDateTime.of(showingDatePicker.getValue(), showingSlotBox.getValue());
            if (showingDao.isManagerSlotBusy(managerOpt.get().getUserId(), shownAt)) {
                refreshShowingSlots();
                showAlert(Alert.AlertType.WARNING, "Этот слот уже занят. Выберите другое время.");
                return;
            }

            Showing showing = new Showing();
            showing.setClientId(clientOpt.get().getClientId());
            showing.setManagerUserId(managerOpt.get().getUserId());
            showing.setShownAt(shownAt);
            showing.setResult("requested");
            showing.setComment("Запрос показа с интерактивной карты: " + selectedPoint.getPointCode()
                    + ", слот " + shownAt);

            int showingId = showingDao.create(showing);
            showingDao.addPoint(showingId, selectedPoint.getTradePointId());

            refreshShowingSlots();
            showAlert(Alert.AlertType.INFORMATION, "Запрос показа создан на "
                    + showingDatePicker.getValue() + " "
                    + showingSlotBox.getConverter().toString(showingSlotBox.getValue())
                    + ". Менеджер увидит его в разделе «Показы».");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Не удалось создать запрос показа. Проверьте подключение к БД.");
        }
    }

    private void hidePointInfo() {
        pointCodeLabel.setText("—");
        pointCodeLabel.setVisible(false);
        pointCodeLabel.setManaged(false);
        inspectorEmptyLabel.setVisible(true);
        inspectorEmptyLabel.setManaged(true);
        pointDetailsBox.setVisible(false);
        pointDetailsBox.setManaged(false);
        setSchedulerVisible(false);
        if (requestShowingButton != null) {
            requestShowingButton.setDisable(true);
        }
        if (fallbackPlanActive) {
            setVisibleManaged(rightColumn, false);
        }
    }

    private String formatCurrency(BigDecimal value) {
        return currencyFormat.format(value);
    }

    private PlanBounds computePlanBounds() {
        if (currentPlan == null) {
            return new PlanBounds(0, 0,
                    mapCanvas == null ? 1 : Math.max(1, mapCanvas.getWidth()),
                    mapCanvas == null ? 1 : Math.max(1, mapCanvas.getHeight()));
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (double[] point : currentPlan.getBuildingOutline()) {
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }

        for (FloorPlan.Room room : currentPlan.getRooms()) {
            for (double[] point : room.polygon) {
                minX = Math.min(minX, point[0]);
                minY = Math.min(minY, point[1]);
                maxX = Math.max(maxX, point[0]);
                maxY = Math.max(maxY, point[1]);
            }
        }

        if (minX == Double.MAX_VALUE) {
            return new PlanBounds(0, 0,
                    Math.max(1, currentPlan.getCanvasWidth()),
                    Math.max(1, currentPlan.getCanvasHeight()));
        }
        return new PlanBounds(minX, minY, maxX, maxY);
    }

    private PlanBounds boundsOf(List<double[]> points) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (double[] point : points) {
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
        return new PlanBounds(minX, minY, maxX, maxY);
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Mall Lease");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.showAndWait();
    }

    private record FilterCriteria(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minArea,
            BigDecimal maxArea,
            boolean acOnly,
            Set<String> statuses) {
    }

    private record PlanBounds(double minX, double minY, double maxX, double maxY) {
        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }

        double centerX() {
            return (minX + maxX) / 2;
        }

        double centerY() {
            return (minY + maxY) / 2;
        }
    }
}
