package com.oildrills;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Immutable geographic oil-field definition loaded from {@code oil_fields.json}.
 */
final class OilFieldDefinition {

    private final String id;
    private final String name;
    private final double centerX;
    private final double centerZ;
    private final double radiusX;
    private final double radiusZ;
    private final double oilAmountScore;

    OilFieldDefinition(
            String id,
            String name,
            double centerX,
            double centerZ,
            double radiusX,
            double radiusZ,
            double oilAmountScore
    ) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.centerX = requireFinite(centerX, "center x");
        this.centerZ = requireFinite(centerZ, "center z");
        this.radiusX = requirePositive(radiusX, "radius x");
        this.radiusZ = requirePositive(radiusZ, "radius z");
        this.oilAmountScore = requireRange(oilAmountScore, 1.0, 100.0, "oil amount score");
    }

    static OilFieldDefinition fromJson(JsonObject json) {
        Objects.requireNonNull(json, "field definition");
        JsonObject minecraft = requiredObject(json, "minecraft");
        JsonObject size = requiredObject(json, "size");
        return new OilFieldDefinition(
                requiredString(json, "id"),
                requiredString(json, "name"),
                requiredDouble(minecraft, "x"),
                requiredDouble(minecraft, "z"),
                requiredDouble(size, "radius_x_blocks"),
                requiredDouble(size, "radius_z_blocks"),
                requiredDouble(json, "oil_amount_score")
        );
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    double oilAmountScore() {
        return oilAmountScore;
    }

    double centerX() {
        return centerX;
    }

    double centerZ() {
        return centerZ;
    }

    /**
     * Returns elliptical distance squared. Values at or below 1 are inside the field.
     */
    double normalizedDistanceSquared(double x, double z) {
        double normalizedX = (x - centerX) / radiusX;
        double normalizedZ = (z - centerZ) / radiusZ;
        return normalizedX * normalizedX + normalizedZ * normalizedZ;
    }

    boolean contains(double x, double z) {
        return normalizedDistanceSquared(x, z) <= 1.0;
    }

    /** Selects one deterministic field when inspection ellipses overlap. */
    static OilFieldDefinition findClosestContaining(
            Iterable<OilFieldDefinition> fields,
            double x,
            double z
    ) {
        OilFieldDefinition closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (OilFieldDefinition field : fields) {
            double distance = field.normalizedDistanceSquared(x, z);
            if (distance <= 1.0 && distance < closestDistance) {
                closest = field;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * Resolves a Minecraft block by its physical centre. Survey and drill callers both use this
     * path so they cannot disagree at an ellipse boundary.
     */
    static OilFieldDefinition findClosestContainingBlock(
            Iterable<OilFieldDefinition> fields,
            int blockX,
            int blockZ
    ) {
        return findClosestContaining(fields, blockX + 0.5, blockZ + 0.5);
    }

    boolean intersectsSquare(double centerX, double centerZ, double range) {
        if (range < 0.0) {
            return false;
        }
        double closestX = Math.max(centerX - range, Math.min(this.centerX, centerX + range));
        double closestZ = Math.max(centerZ - range, Math.min(this.centerZ, centerZ + range));
        return contains(closestX, closestZ);
    }

    static OilFieldDefinition findClosestIntersectingSquare(
            Iterable<OilFieldDefinition> fields,
            double x,
            double z,
            double range
    ) {
        OilFieldDefinition closest = null;
        double closestCenterDistanceSquared = Double.POSITIVE_INFINITY;
        for (OilFieldDefinition field : fields) {
            if (!field.intersectsSquare(x, z, range)) {
                continue;
            }
            double deltaX = field.centerX - x;
            double deltaZ = field.centerZ - z;
            double centerDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (centerDistanceSquared < closestCenterDistanceSquared) {
                closest = field;
                closestCenterDistanceSquared = centerDistanceSquared;
            }
        }
        return closest;
    }

    int capacity(int barrelsPerScore) {
        if (barrelsPerScore <= 0) {
            return 0;
        }
        long capacity = Math.round(oilAmountScore * barrelsPerScore);
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    private static JsonObject requiredObject(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalArgumentException("Missing object '" + key + "'");
        }
        return json.getAsJsonObject(key);
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing string '" + key + "'");
        }
        return json.get(key).getAsString();
    }

    private static double requiredDouble(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing number '" + key + "'");
        }
        try {
            return json.get(key).getAsDouble();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid number '" + key + "'", ex);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    private static double requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return value;
    }

    private static double requirePositive(double value, String label) {
        requireFinite(value, label);
        if (value <= 0.0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }

    private static double requireRange(double value, double min, double max, String label) {
        requireFinite(value, label);
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max);
        }
        return value;
    }
}
