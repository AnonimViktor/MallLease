package com.malllease.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.malllease.dao.UserDao;
import com.malllease.model.User;
import com.malllease.service.AuthService;
import com.malllease.util.SessionStore;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthController {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$");

    @FXML private Node loginPane;
    @FXML private Node registerPane;
    @FXML private Node authLoadingOverlay;

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label loginError;
    @FXML private Label passwordError;
    @FXML private Label generalError;

    @FXML private TextField regFullNameField;
    @FXML private TextField regLoginField;
    @FXML private TextField regEmailField;
    @FXML private TextField regPhoneField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regPasswordRepeatField;
    @FXML private TextField regCompanyField;
    @FXML private TextField regAddressField;
    @FXML private TextField regBankDetailsField;
    @FXML private Label passwordHintLabel;
    @FXML private Label regGeneralError;

    private final UserDao userDao = new UserDao();
    private final AuthService authService = new AuthService();
    private PauseTransition introDelay;
    private FadeTransition overlayFade;
    private FadeTransition paneFadeOut;
    private ParallelTransition paneTransition;

    @FXML
    private void initialize() {
        setPaneVisible(registerPane, false);
        setupLiveValidation();
        Platform.runLater(this::playIntroAnimation);
    }

    @FXML
    private void handleLogin() {
        clearLoginErrors();

        String login = normalize(loginField.getText());
        String password = passwordField.getText();

        if (login.isEmpty()) {
            showError(loginError, "Введите логин");
            markInvalid(loginField, true);
            return;
        }

        if (password.isEmpty()) {
            showError(passwordError, "Введите пароль");
            markInvalid(passwordField, true);
            return;
        }

        try {
            Optional<User> userOpt = userDao.findByLogin(login);

            if (userOpt.isEmpty()) {
                showError(generalError, "Неверный логин или пароль");
                return;
            }

            User user = userOpt.get();
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());

            if (!result.verified) {
                showError(generalError, "Неверный логин или пароль");
                return;
            }

            navigateToMain(user);
        } catch (Exception e) {
            showError(generalError, "Ошибка подключения к базе данных. Проверьте, что сервер запущен.");
        }
    }

    @FXML
    private void handleShowRegister() {
        clearLoginErrors();
        switchPane(loginPane, registerPane);
    }

    @FXML
    private void handleShowLogin() {
        clearRegisterErrors();
        switchPane(registerPane, loginPane);
    }

    @FXML
    private void handleRegister() {
        clearRegisterErrors();

        if (!validateRegisterForm()) {
            return;
        }

        try {
            User registeredUser = registerClient(
                    regLoginField.getText(),
                    regEmailField.getText(),
                    regPhoneField.getText(),
                    regFullNameField.getText(),
                    regPasswordField.getText(),
                    regPasswordRepeatField.getText(),
                    regCompanyField.getText(),
                    regAddressField.getText(),
                    regBankDetailsField.getText());
            navigateToMain(registeredUser);
        } catch (IllegalArgumentException ex) {
            showError(regGeneralError, ex.getMessage());
        } catch (Exception ex) {
            showError(regGeneralError, "Не удалось зарегистрироваться. Проверьте подключение к БД и уникальность данных.");
        }
    }

    public static boolean isPasswordValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private void setupLiveValidation() {
        List<TextField> fields = List.of(
                loginField,
                regFullNameField,
                regLoginField,
                regEmailField,
                regPhoneField,
                regCompanyField,
                regAddressField,
                regBankDetailsField);

        fields.forEach(field -> field.textProperty().addListener((obs, oldValue, newValue) -> markInvalid(field, false)));
        passwordField.textProperty().addListener((obs, oldValue, newValue) -> markInvalid(passwordField, false));
        regPasswordRepeatField.textProperty().addListener((obs, oldValue, newValue) -> markInvalid(regPasswordRepeatField, false));
        regPasswordField.textProperty().addListener((obs, oldValue, password) -> {
            markInvalid(regPasswordField, false);
            updatePasswordHint(password);
        });
    }

    private void updatePasswordHint(String password) {
        boolean valid = password != null && (password.isEmpty() || isPasswordValid(password));
        passwordHintLabel.getStyleClass().removeAll("auth-hint-ok", "auth-hint-error");
        passwordHintLabel.getStyleClass().add(valid ? "auth-hint-ok" : "auth-hint-error");
    }

    private boolean validateRegisterForm() {
        boolean valid = true;

        valid &= require(regFullNameField, "Введите ФИО пользователя");
        valid &= require(regLoginField, "Введите логин");
        valid &= require(regCompanyField, "Введите название компании");
        valid &= require(regAddressField, "Введите юридический адрес");
        valid &= require(regBankDetailsField, "Введите реквизиты");

        String email = normalize(regEmailField.getText());
        if (email.isEmpty() || !email.contains("@")) {
            markInvalid(regEmailField, true);
            showError(regGeneralError, "Введите корректный email");
            valid = false;
        }

        String phone = normalize(regPhoneField.getText());
        if (!phone.isEmpty() && !isPhoneValid(phone)) {
            markInvalid(regPhoneField, true);
            showError(regGeneralError, "Телефон: введите номер в формате +7XXXXXXXXXX или 8XXXXXXXXXX");
            valid = false;
        }

        String password = regPasswordField.getText();
        String passwordRepeat = regPasswordRepeatField.getText();
        if (!password.equals(passwordRepeat)) {
            markInvalid(regPasswordField, true);
            markInvalid(regPasswordRepeatField, true);
            showError(regGeneralError, "Пароли не совпадают");
            valid = false;
        }

        if (!isPasswordValid(password)) {
            markInvalid(regPasswordField, true);
            updatePasswordHint(password);
            showError(regGeneralError, "Пароль: минимум 8 символов, цифра, заглавная буква и спецсимвол");
            valid = false;
        }

        return valid;
    }

    static boolean isPhoneValid(String phone) {
        // Принимаем: +7XXXXXXXXXX, 7XXXXXXXXXX, 8XXXXXXXXXX (с пробелами, дефисами, скобками)
        String digits = phone.replaceAll("[\\s\\-().+]", "");
        return digits.matches("^[78]\\d{10}$");
    }

    private boolean require(TextField field, String error) {
        if (!normalize(field.getText()).isEmpty()) {
            return true;
        }
        markInvalid(field, true);
        showError(regGeneralError, error);
        return false;
    }

    private User registerClient(
            String login,
            String email,
            String phone,
            String fullName,
            String password,
            String passwordRepeat,
            String company,
            String address,
            String bankDetails) {
        login    = normalize(login);
        email    = normalize(email);
        phone    = normalize(phone);
        fullName = normalizeName(fullName);
        company  = normalize(company);
        address = normalize(address);
        bankDetails = normalize(bankDetails);

        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Введите ФИО пользователя");
        }
        if (login.isEmpty()) {
            throw new IllegalArgumentException("Введите логин");
        }
        if (email.isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Введите корректный email");
        }
        if (company.isEmpty()) {
            throw new IllegalArgumentException("Введите название компании");
        }
        if (!password.equals(passwordRepeat)) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }
        if (!isPasswordValid(password)) {
            throw new IllegalArgumentException("Пароль: минимум 8 символов, цифра, заглавная буква и спецсимвол");
        }
        if (userDao.existsByLogin(login)) {
            throw new IllegalArgumentException("Такой логин уже занят");
        }
        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Такой email уже зарегистрирован");
        }

        return authService.registerClient(new AuthService.RegistrationRequest(
                login,
                email,
                phone,
                fullName,
                password,
                company,
                address,
                bankDetails));
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    /** Схлопывает все внутренние пробельные символы в один пробел. */
    private String normalizeName(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    private void playIntroAnimation() {
        loginPane.setOpacity(0);
        loginPane.setTranslateY(14);

        FadeTransition formFade = new FadeTransition(Duration.millis(320), loginPane);
        formFade.setFromValue(0);
        formFade.setToValue(1);
        TranslateTransition formSlide = new TranslateTransition(Duration.millis(320), loginPane);
        formSlide.setFromY(14);
        formSlide.setToY(0);

        introDelay = new PauseTransition(Duration.millis(250));
        introDelay.setOnFinished(event -> {
            if (authLoadingOverlay != null) {
                overlayFade = new FadeTransition(Duration.millis(260), authLoadingOverlay);
                overlayFade.setFromValue(1);
                overlayFade.setToValue(0);
                overlayFade.setOnFinished(done -> {
                    authLoadingOverlay.setVisible(false);
                    authLoadingOverlay.setManaged(false);
                });
                overlayFade.play();
            }
            paneTransition = new ParallelTransition(formFade, formSlide);
            paneTransition.play();
        });
        introDelay.play();
    }

    private void switchPane(Node from, Node to) {
        if (from == null || to == null || to.isVisible()) {
            return;
        }

        paneFadeOut = new FadeTransition(Duration.millis(120), from);
        paneFadeOut.setFromValue(1);
        paneFadeOut.setToValue(0);
        paneFadeOut.setOnFinished(event -> {
            setPaneVisible(from, false);
            to.setOpacity(0);
            to.setTranslateY(12);
            setPaneVisible(to, true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(190), to);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(190), to);
            slideIn.setFromY(12);
            slideIn.setToY(0);
            paneTransition = new ParallelTransition(fadeIn, slideIn);
            paneTransition.play();
        });
        paneFadeOut.play();
    }

    private void setPaneVisible(Node pane, boolean visible) {
        if (pane == null) {
            return;
        }
        pane.setVisible(visible);
        pane.setManaged(visible);
        if (visible) {
            pane.setOpacity(1);
        }
    }

    private void navigateToMain(User user) {
        try {
            SessionStore.save(user.getUserId(), user.getLogin());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.initUser(user);

            Stage stage = (Stage) loginField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            showError(generalError, "Не удалось загрузить главный экран");
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
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

    private void clearLoginErrors() {
        hideError(loginError);
        hideError(passwordError);
        hideError(generalError);
        markInvalid(loginField, false);
        markInvalid(passwordField, false);
    }

    private void clearRegisterErrors() {
        hideError(regGeneralError);
        List<Node> fields = List.of(
                regFullNameField,
                regLoginField,
                regEmailField,
                regPhoneField,
                regPasswordField,
                regPasswordRepeatField,
                regCompanyField,
                regAddressField,
                regBankDetailsField);
        fields.forEach(field -> markInvalid(field, false));
    }

    private void hideError(Label label) {
        if (label == null) {
            return;
        }
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }
}
