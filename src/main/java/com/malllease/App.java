package com.malllease;

import com.malllease.config.Database;
import com.malllease.controller.MainController;
import com.malllease.dao.UserDao;
import com.malllease.model.User;
import com.malllease.util.SessionStore;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final double MIN_WIDTH = 1180;
    private static final double MIN_HEIGHT = 760;
    private static final double START_WIDTH = 1360;
    private static final double START_HEIGHT = 880;

    @Override
    public void init() {
        Database.init();
    }

    @Override
    public void stop() {
        Database.shutdown();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Mall Lease starting up");
        primaryStage.setTitle("Mall Lease");
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setMaximized(true);

        Optional<User> restored = restoreSession();
        if (restored.isPresent()) {
            try {
                showMainFor(primaryStage, restored.get());
                return;
            } catch (Exception e) {
                log.warn("Failed to restore session UI, falling back to login", e);
                SessionStore.clear();
            }
        }
        showAuth(primaryStage);
    }

    private void showAuth(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/auth.fxml"));
        Scene scene = new Scene(root, START_WIDTH, START_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private void showMainFor(Stage stage, User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.initUser(user);

        Scene scene = new Scene(root, START_WIDTH, START_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private Optional<User> restoreSession() {
        Optional<SessionStore.Saved> saved = SessionStore.load();
        if (saved.isEmpty()) {
            return Optional.empty();
        }
        try {
            return new UserDao().findById(saved.get().userId()).filter(User::isActive);
        } catch (Exception e) {
            log.warn("Failed to look up saved user #{}", saved.get().userId(), e);
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
