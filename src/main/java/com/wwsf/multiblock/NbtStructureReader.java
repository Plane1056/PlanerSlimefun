package com.wwsf.multiblock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.annotation.Nullable;

public final class NbtStructureReader {

    private NbtStructureReader() {}

    public static final class PaletteEntry {
        public final String name;
        public final Map<String, String> properties;

        public PaletteEntry(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String baseMaterial() {
            String n = name;
            int colon = n.indexOf(':');
            if (colon >= 0) n = n.substring(colon + 1);
            return n.toUpperCase(Locale.ROOT).replace('-', '_');
        }
    }

    public static final class StructureBlock {
        public final int x, y, z;
        public final int paletteIndex;

        public StructureBlock(int x, int y, int z, int paletteIndex) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.paletteIndex = paletteIndex;
        }
    }

    public static final class StructureData {
        public final int sizeX, sizeY, sizeZ;
        public final List<PaletteEntry> palette;
        public final List<StructureBlock> blocks;

        public StructureData(int sizeX, int sizeY, int sizeZ,
                             List<PaletteEntry> palette,
                             List<StructureBlock> blocks) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.palette = palette;
            this.blocks = blocks;
        }
    }

    public static StructureData read(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new java.util.zip.GZIPInputStream(new FileInputStream(file))))) {
            return read(in);
        }
    }

    public static StructureData read(DataInputStream in) throws IOException {
        if (in.read() != 0x0A) throw new IOException("Expected TAG_Compound");
        readString(in); // root name
        Map<String, Object> root = readCompound(in);
        return parse(root);
    }

    @SuppressWarnings("unchecked")
    private static StructureData parse(Map<String, Object> root) {
        List<Integer> size = (List<Integer>) root.get("size");
        int sizeX = size.get(0);
        int sizeY = size.get(1);
        int sizeZ = size.get(2);

        List<Map<String, Object>> paletteList = (List<Map<String, Object>>) root.get("palette");
        List<PaletteEntry> palette = new ArrayList<>();
        for (Map<String, Object> entry : paletteList) {
            String name = (String) entry.get("Name");
            Map<String, String> props = new HashMap<>();
            Map<String, Object> propCompound = (Map<String, Object>) entry.get("Properties");
            if (propCompound != null) {
                for (Map.Entry<String, Object> pe : propCompound.entrySet()) {
                    props.put(pe.getKey(), pe.getValue().toString());
                }
            }
            palette.add(new PaletteEntry(name, props));
        }

        List<Map<String, Object>> blocksList = (List<Map<String, Object>>) root.get("blocks");
        List<StructureBlock> blocks = new ArrayList<>();
        for (Map<String, Object> block : blocksList) {
            List<Integer> pos = (List<Integer>) block.get("pos");
            int state = ((Number) block.get("state")).intValue();
            blocks.add(new StructureBlock(pos.get(0), pos.get(1), pos.get(2), state));
        }

        return new StructureData(sizeX, sizeY, sizeZ, palette, blocks);
    }

    @Nullable
    public static LayerResult readNbtLayers(File nbtFile, java.util.Map<Character, org.bukkit.Material> yamlPalette) throws Exception {
        StructureData data = read(nbtFile);

        char[][][] grid = new char[data.sizeY][data.sizeZ][data.sizeX];
        for (int y = 0; y < data.sizeY; y++) {
            for (int z = 0; z < data.sizeZ; z++) {
                for (int x = 0; x < data.sizeX; x++) {
                    grid[y][z][x] = '.';
                }
            }
        }

        java.util.Map<org.bukkit.Material, Character> matToChar = new java.util.HashMap<>();
        for (java.util.Map.Entry<Character, org.bukkit.Material> e : yamlPalette.entrySet()) {
            matToChar.put(e.getValue(), e.getKey());
        }

        for (StructureBlock block : data.blocks) {
            if (block.x < 0 || block.x >= data.sizeX) continue;
            if (block.y < 0 || block.y >= data.sizeY) continue;
            if (block.z < 0 || block.z >= data.sizeZ) continue;
            if (block.paletteIndex < 0 || block.paletteIndex >= data.palette.size()) continue;

            PaletteEntry entry = data.palette.get(block.paletteIndex);
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(entry.baseMaterial());
            if (mat == null) continue;

            Character c = matToChar.get(mat);
            if (c == null) continue;

            grid[block.y][block.z][block.x] = c;
        }

        java.util.Map<Integer, String[]> layerRows = new java.util.LinkedHashMap<>();
        for (int y = 0; y < data.sizeY; y++) {
            String[] rows = new String[data.sizeZ];
            for (int z = 0; z < data.sizeZ; z++) {
                StringBuilder sb = new StringBuilder(data.sizeX);
                for (int x = 0; x < data.sizeX; x++) {
                    sb.append(grid[y][z][x]);
                }
                rows[z] = sb.toString();
            }
            layerRows.put(y, rows);
        }

        return new LayerResult(data.sizeX, data.sizeY, data.sizeZ, layerRows);
    }

    // --- Minimal NBT reader ---

    private static final byte TAG_END = 0x00;
    private static final byte TAG_BYTE = 0x01;
    private static final byte TAG_SHORT = 0x02;
    private static final byte TAG_INT = 0x03;
    private static final byte TAG_LONG = 0x04;
    private static final byte TAG_FLOAT = 0x05;
    private static final byte TAG_DOUBLE = 0x06;
    private static final byte TAG_BYTE_ARRAY = 0x07;
    private static final byte TAG_STRING = 0x08;
    private static final byte TAG_LIST = 0x09;
    private static final byte TAG_COMPOUND = 0x0A;
    private static final byte TAG_INT_ARRAY = 0x0B;
    private static final byte TAG_LONG_ARRAY = 0x0C;

    private static String readString(DataInputStream in) throws IOException {
        short len = in.readShort();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Object readTag(DataInputStream in, byte type) throws IOException {
        return switch (type) {
            case TAG_END -> null;
            case TAG_BYTE -> in.readByte();
            case TAG_SHORT -> in.readShort();
            case TAG_INT -> in.readInt();
            case TAG_LONG -> in.readLong();
            case TAG_FLOAT -> in.readFloat();
            case TAG_DOUBLE -> in.readDouble();
            case TAG_BYTE_ARRAY -> {
                int len = in.readInt();
                byte[] arr = new byte[len];
                in.readFully(arr);
                yield arr;
            }
            case TAG_STRING -> readString(in);
            case TAG_LIST -> {
                byte elemType = in.readByte();
                int len = in.readInt();
                java.util.List<Object> list = new java.util.ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(readTag(in, elemType));
                }
                yield list;
            }
            case TAG_COMPOUND -> readCompound(in);
            case TAG_INT_ARRAY -> {
                int len = in.readInt();
                java.util.List<Integer> list = new java.util.ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(in.readInt());
                }
                yield list;
            }
            case TAG_LONG_ARRAY -> {
                int len = in.readInt();
                java.util.List<Long> list = new java.util.ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(in.readLong());
                }
                yield list;
            }
            default -> throw new IOException("Unknown NBT tag type: " + type);
        };
    }

    private static java.util.Map<String, Object> readCompound(DataInputStream in) throws IOException {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        while (true) {
            byte type = in.readByte();
            if (type == TAG_END) break;
            String name = readString(in);
            Object value = readTag(in, type);
            if (value != null) {
                map.put(name, value);
            }
        }
        return map;
    }

    public static final class LayerResult {
        public final int sizeX, sizeY, sizeZ;
        public final java.util.Map<Integer, String[]> layerRows;

        public LayerResult(int sizeX, int sizeY, int sizeZ, java.util.Map<Integer, String[]> layerRows) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.layerRows = layerRows;
        }
    }
}

