package com.oildrills;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OilFieldDefinitionTest {

    private final OilFieldDefinition field = new OilFieldDefinition(
            "TEST", "Test Field", 100.0, -50.0, 20.0, 10.0, 75.0
    );

    @Test
    void containsCenterAndEllipseBoundary() {
        assertTrue(field.contains(100.0, -50.0));
        assertTrue(field.contains(120.0, -50.0));
        assertTrue(field.contains(100.0, -40.0));
    }

    @Test
    void excludesPointsOutsideEllipseAndBoundingBoxCorners() {
        assertFalse(field.contains(120.01, -50.0));
        assertFalse(field.contains(100.0, -39.99));
        assertFalse(field.contains(120.0, -40.0));
    }

    @Test
    void convertsAmountScoreToSharedCapacity() {
        assertEquals(4_500, field.capacity(60));
    }

    @Test
    void choosesNearestNormalizedCenterWhenFieldsOverlap() {
        OilFieldDefinition left = new OilFieldDefinition("LEFT", "Left", 0, 0, 10, 10, 50);
        OilFieldDefinition right = new OilFieldDefinition("RIGHT", "Right", 5, 0, 10, 10, 50);

        assertSame(right, OilFieldDefinition.findClosestContaining(List.of(left, right), 4.5, 0));
        assertSame(left, OilFieldDefinition.findClosestContaining(List.of(left, right), 0.5, 0));
        assertEquals(null, OilFieldDefinition.findClosestContaining(List.of(left, right), 100, 100));
    }

    @Test
    void detectsFieldFootprintsThatIntersectTheFiftyBlockSearchSquare() {
        OilFieldDefinition field = new OilFieldDefinition(
                "NEAR", "Near", 100, 0, 40, 20, 50
        );

        assertTrue(field.intersectsSquare(0, 0, 60));
        assertFalse(field.intersectsSquare(0, 0, 59.9));
        assertFalse(field.intersectsSquare(0, 0, -1));
    }

    @Test
    void choosesClosestFieldWhoseFootprintIntersectsSearchSquare() {
        OilFieldDefinition near = new OilFieldDefinition("NEAR", "Near", 60, 60, 20, 20, 50);
        OilFieldDefinition far = new OilFieldDefinition("FAR", "Far", 100, 0, 60, 20, 50);

        assertSame(
                near,
                OilFieldDefinition.findClosestIntersectingSquare(
                        List.of(far, near),
                        0,
                        0,
                        50
                )
        );
        assertEquals(
                null,
                OilFieldDefinition.findClosestIntersectingSquare(List.of(far, near), 0, 0, 5)
        );
    }

    @Test
    void resolvesMinecraftLocationsAtBlockCenters() {
        OilFieldDefinition oneBlockRadius = new OilFieldDefinition(
                "BLOCK", "Block Centre", 0.5, 0.5, 0.5, 0.5, 50
        );

        assertSame(
                oneBlockRadius,
                OilFieldDefinition.findClosestContainingBlock(List.of(oneBlockRadius), 0, 0)
        );
        assertEquals(
                null,
                OilFieldDefinition.findClosestContainingBlock(List.of(oneBlockRadius), 1, 0)
        );
    }

    @Test
    void bundledDatabaseContains79ValidUniqueFields() throws IOException {
        List<OilFieldDefinition> definitions = loadBundledDefinitions();
        Set<String> ids = new HashSet<>();

        assertEquals(79, definitions.size());
        for (OilFieldDefinition definition : definitions) {
            assertTrue(ids.add(definition.id()), "duplicate id " + definition.id());
            assertTrue(definition.capacity(60) > 0);
        }
    }

    @Test
    void rejectsInvalidDefinitions() {
        assertThrows(IllegalArgumentException.class,
                () -> new OilFieldDefinition("", "Bad", 0, 0, 1, 1, 50));
        assertThrows(IllegalArgumentException.class,
                () -> new OilFieldDefinition("BAD", "Bad", 0, 0, 0, 1, 50));
        assertThrows(IllegalArgumentException.class,
                () -> new OilFieldDefinition("BAD", "Bad", 0, 0, 1, 1, 101));
    }

    private static List<OilFieldDefinition> loadBundledDefinitions() throws IOException {
        try (InputStream stream = OilFieldDefinitionTest.class.getResourceAsStream("/oil_fields.json")) {
            assertNotNull(stream, "oil_fields.json was not bundled");
            JsonArray fields = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject().getAsJsonArray("fields");
            List<OilFieldDefinition> definitions = new ArrayList<>();
            for (JsonElement field : fields) {
                definitions.add(OilFieldDefinition.fromJson(field.getAsJsonObject()));
            }
            return definitions;
        }
    }
}
