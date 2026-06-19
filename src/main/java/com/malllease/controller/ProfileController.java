package com.malllease.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.malllease.dao.ClientDao;
import com.malllease.dao.UserDao;
import com.malllease.model.Client;
import com.malllease.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ProfileController {

    @FXML private Label avatarLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileStatusLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField loginField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private VBox clientCard;
    @FXML private TextField companyField;
    @FXML private TextField legalAddressField;
    @FXML private TextField bankDetailsField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField repeatPasswordField;
    @FXML private Label passwordHintLabel;

    private final UserDao userDao = new UserDao();
    private final ClientDao clientDao = new ClientDao();

    private User currentUser;
    private Client client;
    private Consumer<User> profileUpdatedCallback = user -> {};

    @FXML
    private void handleLogout() {
        com.malllease.util.SessionStore.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/auth.fxml"));
            Stage stage = (Stage) profileNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load auth screen", e);
        }
    }

    @FXML
    private void initialize() {
        List<TextField> fields = List.of(
                fullNameField, loginField, emailField, phoneField,
                companyField, legalAddressField, bankDetailsField);
        fields.forEach(field -> field.textProperty().addListener((obs, oldValue, newValue) -> markInvalid(field, false)));
        newPasswordField.textProperty().addListener((obs, oldValue, password) -> {
            markInvalid(newPasswordField, false);
            updatePasswordHint(password);
        });
        repeatPasswordField.textProperty().addListener((obs, oldValue, newValue) -> markInvalid(repeatPasswordField, false));
    }

    public void initUser(User user, Consumer<User> profileUpdatedCallback) {
        this.currentUser = user;
        this.profileUpdatedCallback = profileUpdatedCallback == null ? updatedUser -> {} : profileUpdatedCallback;
        loadProfile();
    }

    @FXML
    private void handleSaveProfile() {
        clearFieldErrors();
        if (!validateProfile()) {
            return;
        }

        try {
            currentUser.setFullName(normalizeName(fullNameField.getText()));
            currentUser.setLogin(normalize(loginField.getText()));
            currentUser.setEmail(normalize(emailField.getText()));
            currentUser.setPhone(normalize(phoneField.getText()));
            userDao.update(currentUser);

            if (client != null) {
                client.setCompanyName(normalize(companyField.getText()));
                client.setLegalAddress(normalize(legalAddressField.getText()));
                client.setBankDetails(normalize(bankDetailsField.getText()));
                clientDao.update(client);
            }

            currentUser = userDao.findById(currentUser.getUserId()).orElse(currentUser);
            profileUpdatedCallback.accept(currentUser);
            loadProfile();
            setStatus("Профиль сохранен", true);
        } catch (Exception e) {
            setStatus("Не удалось сохранить профиль. Проверьте подключение к БД.", false);
        }
    }

    @FXML
    private void handleSavePassword() {
        markInvalid(newPasswordField, false);
        markInvalid(repeatPasswordField, false);

        String password = newPasswordField.getText();
        String repeat = repeatPasswordField.getText();
        if (password.isBlank() && repeat.isBlank()) {
            setStatus("Введите новый пароль", false);
            markInvalid(newPasswordField, true);
            return;
        }
        if (!password.equals(repeat)) {
            setStatus("Пароли не совпадают", false);
            markInvalid(newPasswordField, true);
            markInvalid(repeatPasswordField, true);
            return;
        }
        if (!AuthController.isPasswordValid(password)) {
            setStatus("Пароль не соответствует требованиям", false);
            markInvalid(newPasswordField, true);
            updatePasswordHint(password);
            return;
        }

        try {
            String hash = BCrypt.withDefaults().hashToString(10, password.toCharArray());
            userDao.updatePasswordHash(currentUser.getUserId(), hash);
            newPasswordField.clear();
            repeatPasswordField.clear();
            updatePasswordHint("");
            setStatus("Пароль обновлен", true);
        } catch (Exception e) {
            setStatus("Не удалось обновить пароль. Проверьте подключение к БД.", false);
        }
    }

    private void loadProfile() {
        profileNameLabel.setText(currentUser.getFullName());
        profileRoleLabel.setText(currentUser.getRole() == null ? "Пользователь" : currentUser.getRole().getName());
        avatarLabel.setText(initials(currentUser.getFullName()));

        fullNameField.setText(currentUser.getFullName());
        loginField.setText(currentUser.getLogin());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());

        Optional<Client> clientOpt = clientDao.findByUserId(currentUser.getUserId());
        client = clientOpt.orElse(null);
        boolean hasClientProfile = client != null;
        clientCard.setVisible(hasClientProfile);
        clientCard.setManaged(hasClientProfile);
        if (hasClientProfile) {
            companyField.setText(client.getCompanyName());
            legalAddressField.setText(client.getLegalAddress());
            bankDetailsField.setText(client.getBankDetails());
        }
    }

    private boolean validateProfile() {
        boolean valid = true;
        String fullName = normalizeName(fullNameField.getText());
        String login = normalize(loginField.getText());
        String email = normalize(emailField.getText());

        if (fullName.isBlank()) {
            markInvalid(fullNameField, true);
            setStatus("Введите ФИО", false);
            valid = false;
        }
        if (login.isBlank()) {
            markInvalid(loginField, true);
            setStatus("Введите логин", false);
            valid = false;
        }
        if (email.isBlank() || !email.contains("@")) {
            markInvalid(emailField, true);
            setStatus("Введите корректный email", false);
            valid = false;
        }
        if (client != null && normalize(companyField.getText()).isBlank()) {
            markInvalid(companyField, true);
            setStatus("Введите название компании", false);
            valid = false;
        }
        if (!valid) {
            return false;
        }
        if (userDao.existsByLoginExceptUser(login, currentUser.getUserId())) {
            markInvalid(loginField, true);
            setStatus("Такой логин уже занят", false);
            return false;
        }
        if (userDao.existsByEmailExceptUser(email, currentUser.getUserId())) {
            markInvalid(emailField, true);
            setStatus("Такой email уже зарегистрирован", false);
            return false;
        }
        return true;
    }

    private void updatePasswordHint(String password) {
        boolean valid = password == null || password.isBlank() || AuthController.isPasswordValid(password);
        passwordHintLabel.getStyleClass().removeAll("auth-hint-ok", "auth-hint-error");
        passwordHintLabel.getStyleClass().add(valid ? "auth-hint-ok" : "auth-hint-error");
    }

    private void clearFieldErrors() {
        List<Node> fields = List.of(
                fullNameField, loginField, emailField, phoneField,
                companyField, legalAddressField, bankDetailsField);
        fields.forEach(field -> markInvalid(field, false));
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

    private void setStatus(String message, boolean success) {
        profileStatusLabel.setText(message);
        profileStatusLabel.getStyleClass().removeAll("profile-status-success", "profile-status-error");
        profileStatusLabel.getStyleClass().add(success ? "profile-status-success" : "profile-status-error");
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    private String initials(String name) {
        String value = normalize(name);
        if (value.isBlank()) {
            return "U";
        }
        String[] parts = value.split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }
}
