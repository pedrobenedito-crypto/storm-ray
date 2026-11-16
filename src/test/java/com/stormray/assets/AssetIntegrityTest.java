package com.stormray.assets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Catches broken asset references without starting the game: every sound referenced in
 * sounds.json, every particle sprite and every animation bone must exist.
 */
class AssetIntegrityTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "stormray");

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void everySoundFileExists() throws IOException {
        String json = read(ASSETS.resolve("sounds.json"));
        Matcher m = Pattern.compile("\"name\":\\s*\"stormray:([^\"]+)\"").matcher(json);
        List<String> missing = new ArrayList<>();
        int count = 0;
        while (m.find()) {
            count++;
            if (!Files.exists(ASSETS.resolve("sounds").resolve(m.group(1) + ".ogg"))) {
                missing.add(m.group(1));
            }
        }
        assertTrue(count > 0, "sounds.json references no files");
        assertTrue(missing.isEmpty(), "missing sound files: " + missing);
    }

    @Test
    void everyParticleSpriteExists() throws IOException {
        List<String> missing = new ArrayList<>();
        try (var files = Files.list(ASSETS.resolve("particles"))) {
            for (Path p : files.toList()) {
                Matcher m = Pattern.compile("\"stormray:([^\"]+)\"").matcher(read(p));
                while (m.find()) {
                    if (!Files.exists(ASSETS.resolve("textures/particle/" + m.group(1) + ".png"))) {
                        missing.add(m.group(1));
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "missing particle textures: " + missing);
    }

    private static void checkAnimations(String entity, String[] required) throws IOException {
        String geo = read(ASSETS.resolve("geo/entity/" + entity + ".geo.json"));
        String anim = read(ASSETS.resolve("animations/entity/" + entity + ".animation.json"));
        List<String> bones = new ArrayList<>();
        Matcher bm = Pattern.compile("\"name\":\\s*\"([^\"]+)\"").matcher(geo);
        while (bm.find()) {
            bones.add(bm.group(1));
        }
        // bone keys inside "bones": { "<name>": { "rotation" | "position" | "scale" ...
        Matcher am = Pattern.compile("\"([a-z0-9_]+)\":\\s*\\{\\s*\"(?:rotation|position|scale)\"").matcher(anim);
        List<String> unknown = new ArrayList<>();
        int count = 0;
        while (am.find()) {
            count++;
            if (!bones.contains(am.group(1))) {
                unknown.add(am.group(1));
            }
        }
        assertTrue(count > 0, entity + ": no animated bones found");
        assertTrue(unknown.isEmpty(), entity + ": animations reference unknown bones: " + unknown);
        for (String name : required) {
            assertTrue(anim.contains("\"animation." + entity + "." + name + "\""), entity + ": missing animation " + name);
        }
    }

    @Test
    void animationsOnlyReferenceExistingBones() throws IOException {
        checkAnimations("storm_ray", new String[]{"idle", "summon", "swoop", "lash", "charge", "release", "death"});
        checkAnimations("phantom_x", new String[]{"idle", "move", "combo", "reveal", "death"});
    }

    @Test
    void dimensionDataExists() {
        Path data = Path.of("src", "main", "resources", "data", "stormray");
        for (String path : new String[]{"dimension/ashen_reflection.json", "dimension_type/ashen_reflection.json",
                "worldgen/configured_feature/ashen_rot.json", "worldgen/placed_feature/ashen_rot.json",
                "forge/biome_modifier/ashen_rot.json"}) {
            assertTrue(Files.exists(data.resolve(path)), "missing data " + path);
        }
    }

    @Test
    void textureFilesExist() {
        for (String path : new String[]{"textures/entity/storm_ray.png", "textures/entity/storm_ray_glowmask.png",
                "textures/environment/storm_clouds.png", "textures/misc/petrified_stone.png",
                "textures/misc/petrify_overlay.png", "lang/en_us.json", "lang/pt_br.json",
                "textures/block/ashen_portal.png", "textures/block/ashen_portal.png.mcmeta",
                "blockstates/ashen_portal.json", "models/block/ashen_portal.json",
                "textures/entity/phantom_x.png", "textures/entity/phantom_x_glowmask.png",
                "models/item/phantom_x_spawn_egg.json"}) {
            assertTrue(Files.exists(ASSETS.resolve(path)), "missing " + path);
        }
    }
}
