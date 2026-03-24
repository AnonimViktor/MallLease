package com.malllease.controller;

import com.malllease.model.User;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private Label pageTitle;
    @FXML private HBox headerBar;
    @FXML private Label userChip;
    @FXML private Label userNameLabel;
    @FXML private Label userAvatarLabel;
    @FXML private HBox sidebarUserProfile;
    @FXML private StackPane contentPane;
    @FXML private VBox navContainer;
    @FXML private VBox sidebar;
    @FXML private VBox brandLabels;
    @FXML private VBox userTextBox;
    @FXML private HBox toggleRow;
    @FXML private HBox brandRow;
    @FXML private Button collapseToggle;
    @FXML private Button navMap;
    @FXML private Button navClients;
    @FXML private Button navShowings;
    @FXML private Button navContracts;
    @FXML private Button navPayments;
    @FXML private Button navAdmin;

    private User currentUser;
    private Button activeNavButton;
    private boolean sidebarCollapsed = false;

    private static final double SIDEBAR_EXPANDED_WIDTH = 248;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 76;

    private record NavSpec(String iconClass, String label) {}
    private final java.util.Map<Button, NavSpec> navSpecs = new java.util.LinkedHashMap<>();

    public void initUser(User user) {
        this.currentUser = user;
        configureNavItems();
        updateUserLabels(user);

        configureNavByRole();
        if (collapseToggle != null) {
            collapseToggle.setGraphic(makeIcon("fth-chevron-left"));
        }
        handleNavMap();
    }

    private void configureNavItems() {
        navSpecs.put(navMap,       new NavSpec("fth-map",         "Карта площадей"));
        navSpecs.put(navClients,   new NavSpec("fth-users",       "Клиенты"));
        navSpecs.put(navShowings,  new NavSpec("fth-calendar",    "Показы"));
        navSpecs.put(navContracts, new NavSpec("fth-file-text",   "Договоры"));
        navSpecs.put(navPayments,  new NavSpec("fth-credit-card", "Платежи"));
        navSpecs.put(navAdmin,     new NavSpec("fth-settings",    "Администрирование"));
        navSpecs.forEach((btn, spec) -> applyNavSpec(btn, spec, sidebarCollapsed));
    }

    private void applyNavSpec(Button btn, NavSpec spec, boolean collapsed) {
        btn.setText(collapsed ? "" : spec.label());
        btn.setGraphic(makeIcon(spec.iconClass()));
        btn.setContentDisplay(collapsed
                ? javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
                : javafx.scene.control.ContentDisplay.LEFT);
        btn.setGraphicTextGap(12);
        btn.setAlignment(collapsed ? javafx.geometry.Pos.CENTER : javafx.geometry.Pos.CENTER_LEFT);
    }

    private org.kordamp.ikonli.javafx.FontIcon makeIcon(String iconLiteral) {
        org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(iconLiteral);
        icon.getStyleClass().add("nav-icon");
        return icon;
    }

    @FXML
    private void handleToggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        double targetWidth = sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        javafx.animation.Timeline collapse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(220),
                        new javafx.animation.KeyValue(
                                sidebar.prefWidthProperty(), targetWidth,
                                javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(
                                sidebar.minWidthProperty(), targetWidth,
                                javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(
                                sidebar.maxWidthProperty(), targetWidth,
                                javafx.animation.Interpolator.EASE_BOTH)));
        collapse.play();

        if (brandLabels != null) {
            fadeLabels(brandLabels, !sidebarCollapsed);
        }
        if (userTextBox != null) {
            fadeLabels(userTextBox, !sidebarCollapsed);
        }

        if (toggleRow != null) {
            toggleRow.setAlignment(sidebarCollapsed
                    ? javafx.geometry.Pos.CENTER
                    : javafx.geometry.Pos.CENTER_RIGHT);
        }
        if (brandRow != null) {
            brandRow.setAlignment(sidebarCollapsed
                    ? javafx.geometry.Pos.CENTER
                    : javafx.geometry.Pos.CENTER_LEFT);
        }
        if (collapseToggle != null) {
            collapseToggle.setGraphic(makeIcon(
                    sidebarCollapsed ? "fth-chevron-right" : "fth-chevron-left"));
        }
        navSpecs.forEach((btn, spec) -> applyNavSpec(btn, spec, sidebarCollapsed));
    }

    private void fadeLabels(javafx.scene.Node node, boolean visible) {
        node.setVisible(true);
        node.setManaged(visible);
        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(visible ? 1 : 0);
        fade.setOnFinished(e -> {
            if (!visible) {
                node.setVisible(false);
            }
        });
        fade.play();
    }

    private void configureNavByRole() {
        String roleCode = currentUser.getRole().getCode();
        navAdmin.setVisible("admin".equals(roleCode));
        navAdmin.setManaged("admin".equals(roleCode));

        boolean isClient = "client".equals(roleCode);
        navClients.setVisible(!isClient);
        navClients.setManaged(!isClient);
        navShowings.setVisible(!isClient);
        navShowings.setManaged(!isClient);
    }

    @FXML
    private void handleNavMap() {
        setActiveNav(navMap);
        pageTitle.setText("Карта площадей");
        loadContent("/fxml/map.fxml");
    }

    public void navigateToMapAndFocus(int tradePointId) {
        setActiveNav(navMap);
        pageTitle.setText("Карта площадей");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/map.fxml"));
            Node content = loader.load();
            contentPane.setPadding(Insets.EMPTY);
            if (headerBar != null) {
                headerBar.setVisible(false);
                headerBar.setManaged(false);
            }
            MapController controller = loader.getController();
            controller.initUser(currentUser);
            javafx.application.Platform.runLater(() -> controller.focusOnPoint(tradePointId));
            swapContent(content);
        } catch (IOException e) {
            showPlaceholder("Не удалось открыть карту");
        }
    }

    @FXML
    private void handleNavShowings() {
        setActiveNav(navShowings);
        pageTitle.setText("Показы");
        loadContent("/fxml/showings.fxml");
    }

    @FXML
    private void handleNavClients() {
        setActiveNav(navClients);
        pageTitle.setText("Клиенты");
        loadManagementContent("clients");
    }

    @FXML
    private void handleNavContracts() {
        setActiveNav(navContracts);
        pageTitle.setText("Договоры");
        boolean isClient = currentUser != null && currentUser.getRole() != null
                && "client".equals(currentUser.getRole().getCode());
        if (isClient) {
            loadContent("/fxml/client-contracts.fxml");
        } else {
            loadManagementContent("contracts");
        }
    }

    @FXML
    private void handleNavPayments() {
        setActiveNav(navPayments);
        pageTitle.setText("Платежи");
        loadContent("/fxml/payments.fxml");
    }

    @FXML
    private void handleNavAdmin() {
        setActiveNav(navAdmin);
        pageTitle.setText("Администрирование");
        loadManagementContent("users");
    }

    @FXML
    private void handleNavProfile(MouseEvent event) {
        if (event != null && event.getTarget() instanceof Button) {
            return;
        }
        setProfileActive();
        pageTitle.setText("Профиль");
        loadContent("/fxml/profile.fxml");
    }

    @FXML
    private void handleLogout() {
        com.malllease.util.SessionStore.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/auth.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load auth screen", e);
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();

            boolean isMap = "/fxml/map.fxml".equals(fxmlPath);
            contentPane.setPadding(isMap ? Insets.EMPTY : new Insets(18, 22, 22, 22));
            if (headerBar != null) {
                headerBar.setVisible(!isMap);
                headerBar.setManaged(!isMap);
            }

            java.util.function.IntConsumer toMap = this::navigateToMapAndFocus;

            Object controller = loader.getController();
            if (controller instanceof MapController mapCtrl) {
                mapCtrl.initUser(currentUser);
            } else if (controller instanceof ShowingsController showingsCtrl) {
                showingsCtrl.initUser(currentUser);
            } else if (controller instanceof PaymentsController paymentsCtrl) {
                paymentsCtrl.initUser(currentUser);
                paymentsCtrl.setMapNavigator(toMap);
            } else if (controller instanceof ProfileController profileCtrl) {
                profileCtrl.initUser(currentUser, this::handleProfileUpdated);
            } else if (controller instanceof ClientContractsController contractsCtrl) {
                contractsCtrl.setMapNavigator(toMap);
                contractsCtrl.initUser(currentUser);
            } else if (controller instanceof ManagementController managementCtrl) {
                managementCtrl.setMapNavigator(toMap);
            }

            swapContent(content);
        } catch (IOException e) {
            showPlaceholder("Ошибка загрузки: " + fxmlPath);
        }
    }

    private void swapContent(Node content) {
        contentPane.getChildren().setAll(content);
        content.setOpacity(0);
        content.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(220), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), content);
        slide.setFromY(12);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void loadManagementContent(String defaultSectionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/management.fxml"));
            Node content = loader.load();

            contentPane.setPadding(new Insets(18, 22, 22, 22));
            if (headerBar != null) {
                headerBar.setVisible(true);
                headerBar.setManaged(true);
            }

            ManagementController controller = loader.getController();
            controller.setMapNavigator(this::navigateToMapAndFocus);
            controller.initUser(currentUser, defaultSectionId);

            swapContent(content);
        } catch (IOException e) {
            showPlaceholder("Ошибка загрузки модуля управления");
        }
    }

    private void showPlaceholder(String text) {
        VBox placeholder = new VBox(16);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        Label label = new Label(text);
        label.getStyleClass().add("heading-2");
        Label sub = new Label("Будет реализовано в следующей версии");
        sub.getStyleClass().add("text-muted");
        placeholder.getChildren().addAll(label, sub);
        contentPane.getChildren().setAll(placeholder);
    }

    private void setActiveNav(Button button) {
        sidebarUserProfile.getStyleClass().remove("sidebar-user-profile-active");
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("sidebar-nav-item-active");
        }
        button.getStyleClass().add("sidebar-nav-item-active");
        activeNavButton = button;
    }

    private void setProfileActive() {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("sidebar-nav-item-active");
            activeNavButton = null;
        }
        if (!sidebarUserProfile.getStyleClass().contains("sidebar-user-profile-active")) {
            sidebarUserProfile.getStyleClass().add("sidebar-user-profile-active");
        }
    }

    private void handleProfileUpdated(User updatedUser) {
        currentUser = updatedUser;
        updateUserLabels(updatedUser);
    }

    private void updateUserLabels(User user) {
        userNameLabel.setText(user.getFullName());
        userChip.setText(user.getRole() == null ? "Пользователь" : user.getRole().getName());
        userAvatarLabel.setText(initials(user.getFullName()));
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }
}
