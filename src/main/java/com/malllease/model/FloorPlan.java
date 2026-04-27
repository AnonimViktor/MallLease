package com.malllease.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FloorPlan {

    private int centerId;
    private int floor;
    private double canvasWidth;
    private double canvasHeight;
    private List<double[]> buildingOutline = new ArrayList<>();
    private List<double[][]> walls = new ArrayList<>();
    private List<Corridor> corridors = new ArrayList<>();
    private List<Room> rooms = new ArrayList<>();

    public static class Corridor {
        public double x, y, width, height;
        public double radius = 18;
        public String label;

        public Corridor(double x, double y, double width, double height, String label) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.label = label;
        }
    }

    public static class Room {
        public String pointCode;
        public List<double[]> polygon;
        public double labelX;
        public double labelY;

        public Room(String pointCode, List<double[]> polygon, double labelX, double labelY) {
            this.pointCode = pointCode;
            this.polygon = polygon;
            this.labelX = labelX;
            this.labelY = labelY;
        }

        public boolean containsPoint(double px, double py) {
            int n = polygon.size();
            boolean inside = false;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
                double xj = polygon.get(j)[0], yj = polygon.get(j)[1];
                if ((yi > py) != (yj > py) &&
                    px < (xj - xi) * (py - yi) / (yj - yi) + xi) {
                    inside = !inside;
                }
            }
            return inside;
        }
    }

    public int getCenterId() { return centerId; }
    public int getFloor() { return floor; }
    public double getCanvasWidth() { return canvasWidth; }
    public double getCanvasHeight() { return canvasHeight; }
    public List<double[]> getBuildingOutline() { return buildingOutline; }
    public List<double[][]> getWalls() { return walls; }
    public List<Corridor> getCorridors() { return corridors; }
    public List<Room> getRooms() { return rooms; }

    public Optional<Room> findRoomAt(double x, double y) {
        for (Room room : rooms) {
            if (room.containsPoint(x, y)) {
                return Optional.of(room);
            }
        }
        return Optional.empty();
    }

    public Optional<Room> findRoomByPointCode(String pointCode) {
        return rooms.stream()
                .filter(room -> room.pointCode.equals(pointCode))
                .findFirst();
    }

    public static FloorPlan fallback(int centerId, int floor, List<String> pointCodes) {
        FloorPlan plan = new FloorPlan();
        plan.centerId = centerId;
        plan.floor = floor;
        plan.canvasWidth = 1120;
        plan.canvasHeight = 760;
        plan.buildingOutline = points(
                p(95, 70), p(970, 70), p(1060, 155), p(1060, 575),
                p(965, 690), p(145, 690), p(55, 585), p(55, 165)
        );
        plan.corridors.add(new Corridor(235, 245, 655, 92, ""));
        plan.corridors.add(new Corridor(235, 470, 655, 86, ""));
        plan.corridors.add(new Corridor(445, 330, 290, 140, ""));
        plan.walls.add(new double[][]{p(95, 225), p(1015, 225)});
        plan.walls.add(new double[][]{p(95, 560), p(1015, 560)});
        plan.walls.add(new double[][]{p(520, 285), p(520, 520)});
        plan.walls.add(new double[][]{p(640, 285), p(640, 520)});

        double[][][] slots = new double[][][]{
                {p(100, 100), p(315, 100), p(315, 220), p(165, 220), p(100, 170)},
                {p(325, 100), p(510, 100), p(510, 220), p(325, 220)},
                {p(520, 100), p(710, 100), p(710, 220), p(520, 220)},
                {p(720, 100), p(900, 100), p(900, 220), p(720, 220)},
                {p(910, 100), p(985, 100), p(1030, 155), p(1030, 220), p(910, 220)},
                {p(915, 250), p(1030, 250), p(1030, 365), p(915, 365)},
                {p(915, 380), p(1030, 380), p(1030, 520), p(915, 520)},
                {p(805, 570), p(965, 570), p(1015, 620), p(960, 670), p(805, 670)},
                {p(610, 570), p(795, 570), p(795, 670), p(610, 670)},
                {p(415, 570), p(600, 570), p(600, 670), p(415, 670)},
                {p(215, 570), p(405, 570), p(405, 670), p(215, 670)},
                {p(90, 570), p(205, 570), p(205, 670), p(145, 670), p(90, 615)},
                {p(90, 395), p(220, 395), p(220, 555), p(90, 555)},
                {p(90, 245), p(220, 245), p(220, 385), p(90, 385)},
                {p(350, 320), p(500, 320), p(500, 500), p(350, 500)}
        };

        int count = Math.min(pointCodes.size(), slots.length);
        for (int i = 0; i < count; i++) {
            double[][] slot = slots[i];
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (double[] point : slot) {
                minX = Math.min(minX, point[0]);
                maxX = Math.max(maxX, point[0]);
                minY = Math.min(minY, point[1]);
                maxY = Math.max(maxY, point[1]);
            }
            plan.rooms.add(new Room(pointCodes.get(i), points(slot), (minX + maxX) / 2, (minY + maxY) / 2 + 6));
        }
        return plan;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FloorPlan.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Optional<FloorPlan> load(int centerId, int floor) {
        String path = "/maps/center-" + centerId + "_floor-" + floor + ".json";
        try (InputStream is = FloorPlan.class.getResourceAsStream(path)) {
            if (is == null) {
                return Optional.empty();
            }
            return Optional.of(parse(MAPPER.readTree(is)));
        } catch (Exception e) {
            LOG.error("FloorPlan parse error for {}: {}", path, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static FloorPlan parse(JsonNode root) {
        FloorPlan plan = new FloorPlan();
        plan.centerId = root.path("centerId").asInt();
        plan.floor = root.path("floor").asInt();
        plan.canvasWidth = root.path("canvasWidth").asDouble();
        plan.canvasHeight = root.path("canvasHeight").asDouble();

        JsonNode building = root.path("building");
        plan.buildingOutline = readPolygon(building.path("outline"));
        plan.walls = readWalls(building.path("walls"));
        plan.corridors = readCorridors(building.path("corridors"));
        plan.rooms = readRooms(root.path("rooms"));
        return plan;
    }

    private static List<double[]> readPolygon(JsonNode array) {
        List<double[]> points = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return points;
        }
        for (JsonNode point : array) {
            if (point.isArray() && point.size() >= 2) {
                points.add(new double[]{point.get(0).asDouble(), point.get(1).asDouble()});
            }
        }
        return points;
    }

    private static List<double[][]> readWalls(JsonNode array) {
        List<double[][]> walls = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return walls;
        }
        for (JsonNode wall : array) {
            if (wall.isArray() && wall.size() >= 2) {
                JsonNode a = wall.get(0);
                JsonNode b = wall.get(1);
                if (a.isArray() && b.isArray() && a.size() >= 2 && b.size() >= 2) {
                    walls.add(new double[][]{
                            {a.get(0).asDouble(), a.get(1).asDouble()},
                            {b.get(0).asDouble(), b.get(1).asDouble()}
                    });
                }
            }
        }
        return walls;
    }

    private static List<Corridor> readCorridors(JsonNode array) {
        List<Corridor> corridors = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return corridors;
        }
        for (JsonNode item : array) {
            JsonNode rect = item.path("rect");
            if (rect.isArray() && rect.size() >= 4) {
                corridors.add(new Corridor(
                        rect.get(0).asDouble(),
                        rect.get(1).asDouble(),
                        rect.get(2).asDouble(),
                        rect.get(3).asDouble(),
                        item.path("label").asText("")));
            }
        }
        return corridors;
    }

    private static List<Room> readRooms(JsonNode array) {
        List<Room> rooms = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return rooms;
        }
        for (JsonNode item : array) {
            String code = item.path("pointCode").asText(null);
            List<double[]> polygon = readPolygon(item.path("polygon"));
            JsonNode labelPos = item.path("labelPos");
            double labelX = 0, labelY = 0;
            if (labelPos.isArray() && labelPos.size() >= 2) {
                labelX = labelPos.get(0).asDouble();
                labelY = labelPos.get(1).asDouble();
            }
            if (code != null && !polygon.isEmpty()) {
                rooms.add(new Room(code, polygon, labelX, labelY));
            }
        }
        return rooms;
    }

    private static double[] p(double x, double y) {
        return new double[]{x, y};
    }

    private static List<double[]> points(double[]... points) {
        return new ArrayList<>(List.of(points));
    }
}
