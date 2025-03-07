package twoten.meteor.diff.modules;

import static twoten.meteor.diff.Diff.s;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.chunk.Chunk;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.hud.RadarHud;
import twoten.meteor.diff.Diff;

public class SaveDiff extends Module {

    private static boolean mkdirs(final Path path) {
        return path.toFile().mkdirs();
    }

    private static void symlink(final Path link, final Path target) throws IOException {
        Files.createSymbolicLink(link, target, new FileAttribute[0]);
    }

    private static Path follow(final Path link) throws IOException {
        return link.toRealPath();
    }

    private static byte[] hash(final Chunk c) {
        return new byte[] {
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
        };
    }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> onLoad = sgGeneral.add(new BoolSetting.Builder()
            .name("on-load")
            .description("Save chunks after they are loaded.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> onUnload = sgGeneral.add(new BoolSetting.Builder()
            .name("on-unload")
            .description("Save chunks when they are unloaded.")
            .defaultValue(false)
            .build());

    private final SettingGroup sgSave = this.settings.createGroup("Save");

    private final Setting<Boolean> saveMap = sgSave.add(new BoolSetting.Builder()
            .name("save-map")
            .description("Save the map view.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> saveBlocks = sgSave.add(new BoolSetting.Builder()
            .name("save-blocks")
            .description("Save block data.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> saveEntities = sgSave.add(new BoolSetting.Builder()
            .name("save-entities")
            .description("Save entity data.")
            .defaultValue(true)
            .build());

    private final SettingGroup sgStorage = this.settings.createGroup("Storage");

    private final Setting<Boolean> zip = sgSave.add(new BoolSetting.Builder()
            .name("zip")
            .description("Compress saved data.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> maxDepth = sgStorage.add(new IntSetting.Builder().name("max-depth")
            .description("Maximum number of unique chunk versions, 0 to disable.")
            .min(0)
            .defaultValue(0)
            .sliderMin(1)
            .build());

    private final Setting<Integer> minTime = sgStorage.add(new IntSetting.Builder().name("min-time")
            .description("The time (seconds) to wait before saving to a new iteration instead of overwriting the last.")
            .defaultValue(60 * 60)
            .min(1)
            .sliderMax(5 * 60 * 60)
            .build());

    public SaveDiff() {
        super(Addon.CATEGORY, "save-diff", "Download chunks.");
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        if (!onLoad.get())
            return;

        final var chunk = event.chunk();
        final var pos = chunk.getPos();
        final var hash = hash(chunk);
        final var time = Instant.now().getEpochSecond();

        final var p = Diff.dimPath().resolve(pos.x + " " + pos.z);
        final var file = p.resolve(String.valueOf(time));
        final var latest = p.resolve(Diff.paths.latest);

        // TODO: run this sync? schedule it?
        try {
            if (exists(latest)) {
                final var realLatest = follow(latest);
                final var latestName = realLatest.getFileName();
                final var lastTime = Long.parseLong(latestName.toString());
                if (lastTime < time)
                    if (Arrays.equals(Files.readAllBytes(latest.resolve(Diff.paths.chunk.hash)), hash)) {
                        symlink(file, latestName);
                    } else {
                        if (time - lastTime < minTime.get()) {
                            final var f = follow(latest);
                            FileUtils.deleteDirectory(f.toFile());
                            symlink(f, file.getFileName());
                        }
                    }
                delete(latest);
            }

            if (!exists(file)) {
                mkdirs(file);
                write(file.resolve(Diff.paths.chunk.hash), hash);

                if (saveMap.get())
                    write(file.resolve(Diff.paths.chunk.map), map(chunk));
                if (saveBlocks.get())
                    write(file.resolve(Diff.paths.chunk.blocks), new byte[0]);
                if (saveEntities.get())
                    write(file.resolve(Diff.paths.chunk.entities), new byte[0]);
            }

            // TODO: try RegionFile
            symlink(latest, file.getFileName());
        } catch (final Exception e) {
            e.printStackTrace();
            info(e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    public static final int colorBytes = Integer.BYTES - 1;

    private byte[] map(final Chunk c) {
        final var colors = RadarHud.map(c);
        final var out = new byte[s * s * colorBytes];

        for (var x = 0; x < s; x++) {
            for (var z = 0; z < s; z++) {
                final var color = colors[x][z];
                final var i = (x * s + z) * colorBytes;
                out[i + 0] = (byte) color.r;
                out[i + 1] = (byte) color.g;
                out[i + 2] = (byte) color.b;
            }
        }

        return out;
    }
}
