package com.malllease.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class RangeSliderControl extends HBox {

    private static final double THUMB_R = 9;
    private static final double TRACK_H = 5;
    private static final double PANE_H = THUMB_R * 2 + 8;

    private final DoubleProperty minProp  = new SimpleDoubleProperty(0);
    private final DoubleProperty maxProp  = new SimpleDoubleProperty(100);
    private final DoubleProperty lowProp  = new SimpleDoubleProperty(0);
    private final DoubleProperty highProp = new SimpleDoubleProperty(100);

    private final TextField lowField;
    private final TextField highField;
    private final Pane sliderPane;
    private final Region track;
    private final Region rangeBar;
    private final Region lowThumb;
    private final Region highThumb;

    private boolean draggingLow = true;
    private boolean suppressSync = false;

    public RangeSliderControl(double min, double max) {
        minProp.set(min);
        maxProp.set(max);
        lowProp.set(min);
        highProp.set(max);

        setAlignment(Pos.CENTER);
        setSpacing(8);

        lowField  = makeField();
        highField = makeField();
        lowField.setText(fmt(min));
        highField.setText(fmt(max));

        sliderPane = new Pane();
        sliderPane.setPrefHeight(PANE_H);
        sliderPane.setMinHeight(PANE_H);
        sliderPane.setMaxHeight(PANE_H);
        HBox.setHgrow(sliderPane, Priority.ALWAYS);
        sliderPane.setMinWidth(80);

        track = new Region();
        track.getStyleClass().add("rs-track");

        rangeBar = new Region();
        rangeBar.getStyleClass().add("rs-range");

        lowThumb = makeThumb();
        highThumb = makeThumb();

        sliderPane.getChildren().addAll(track, rangeBar, lowThumb, highThumb);
        getChildren().addAll(lowField, sliderPane, highField);

        sliderPane.widthProperty().addListener((obs, o, w) -> layoutSlider());
        sliderPane.setOnMousePressed(this::onPress);
        sliderPane.setOnMouseDragged(this::onDrag);
        lowField.setOnAction(e -> commitLow());
        lowField.focusedProperty().addListener((obs, o, f) -> { if (!f) commitLow(); });
        highField.setOnAction(e -> commitHigh());
        highField.focusedProperty().addListener((obs, o, f) -> { if (!f) commitHigh(); });
    }

    public DoubleProperty lowValueProperty()  { return lowProp; }
    public DoubleProperty highValueProperty() { return highProp; }
    public double getLowValue()  { return lowProp.get(); }
    public double getHighValue() { return highProp.get(); }
    public double getMin()       { return minProp.get(); }
    public double getMax()       { return maxProp.get(); }

    public boolean isAtMin() { return Math.abs(getLowValue()  - getMin()) < 0.5; }
    public boolean isAtMax() { return Math.abs(getHighValue() - getMax()) < 0.5; }

    public void reset(double min, double max) {
        suppressSync = true;
        try {
            minProp.set(min);
            maxProp.set(max);
            lowProp.set(min);
            highProp.set(max);
            lowField.setText(fmt(min));
            highField.setText(fmt(max));
            layoutSlider();
        } finally {
            suppressSync = false;
        }
    }

    private void onPress(MouseEvent e) {
        double lowCx  = thumbCenterX(lowProp.get());
        double highCx = thumbCenterX(highProp.get());
        double mx = e.getX();
        draggingLow = Math.abs(mx - lowCx) <= Math.abs(mx - highCx);
    }

    private void onDrag(MouseEvent e) {
        double trackW = sliderPane.getWidth() - THUMB_R * 2;
        if (trackW <= 0) return;
        double frac = clamp01((e.getX() - THUMB_R) / trackW);
        double raw = minProp.get() + frac * (maxProp.get() - minProp.get());

        if (draggingLow) {
            double v = clamp(raw, minProp.get(), highProp.get());
            lowProp.set(v);
            suppressSync = true;
            lowField.setText(fmt(v));
            suppressSync = false;
        } else {
            double v = clamp(raw, lowProp.get(), maxProp.get());
            highProp.set(v);
            suppressSync = true;
            highField.setText(fmt(v));
            suppressSync = false;
        }
        layoutSlider();
    }

    private void commitLow() {
        if (suppressSync) return;
        double v = clamp(parse(lowField.getText(), lowProp.get()), minProp.get(), highProp.get());
        lowProp.set(v);
        lowField.setText(fmt(v));
        layoutSlider();
    }

    private void commitHigh() {
        if (suppressSync) return;
        double v = clamp(parse(highField.getText(), highProp.get()), lowProp.get(), maxProp.get());
        highProp.set(v);
        highField.setText(fmt(v));
        layoutSlider();
    }

    private void layoutSlider() {
        double w = sliderPane.getWidth();
        if (w <= 0) return;

        double trackY = (PANE_H - TRACK_H) / 2.0;
        double trackX = THUMB_R;
        double trackW = w - THUMB_R * 2;

        track.setLayoutX(trackX);
        track.setLayoutY(trackY);
        track.setPrefWidth(trackW);
        track.setPrefHeight(TRACK_H);

        double lowCx  = thumbCenterX(lowProp.get());
        double highCx = thumbCenterX(highProp.get());

        rangeBar.setLayoutX(lowCx);
        rangeBar.setLayoutY(trackY);
        rangeBar.setPrefWidth(Math.max(0, highCx - lowCx));
        rangeBar.setPrefHeight(TRACK_H);

        double thumbY = (PANE_H - THUMB_R * 2) / 2.0;
        lowThumb.setLayoutX(lowCx  - THUMB_R);
        lowThumb.setLayoutY(thumbY);
        highThumb.setLayoutX(highCx - THUMB_R);
        highThumb.setLayoutY(thumbY);
    }

    private double thumbCenterX(double value) {
        double range = maxProp.get() - minProp.get();
        if (range <= 0) return THUMB_R;
        double frac = (value - minProp.get()) / range;
        return THUMB_R + frac * (sliderPane.getWidth() - THUMB_R * 2);
    }

    private static Region makeThumb() {
        Region t = new Region();
        t.setPrefSize(THUMB_R * 2, THUMB_R * 2);
        t.getStyleClass().add("rs-thumb");
        t.setCursor(Cursor.H_RESIZE);
        return t;
    }

    private static TextField makeField() {
        TextField tf = new TextField();
        tf.setPrefWidth(76);
        tf.setMinWidth(56);
        tf.setMaxWidth(90);
        tf.setAlignment(Pos.CENTER);
        tf.getStyleClass().addAll("map-filter-field", "rs-field");
        tf.setTextFormatter(new TextFormatter<>(change -> {
            String t = change.getControlNewText();
            return t.matches("[0-9]*[.,]?[0-9]*") ? change : null;
        }));
        return tf;
    }

    private static String fmt(double v) {
        return Long.toString(Math.round(v));
    }

    private static double parse(String text, double fallback) {
        if (text == null || text.isBlank()) return fallback;
        try { return Double.parseDouble(text.trim().replace(',', '.')); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
