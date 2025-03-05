package twoten.meteor.diff.modules;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.math.BigInteger;
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
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff;

public class SaveDiff extends Module {
    private interface paths {
        Path latest = Path.of(String.valueOf(0L));
        Path hash = Path.of(".hash");
    }

    private static boolean mkdirs(final Path path) {
        return path.toFile().mkdirs();
    }

    private static void symlink(final Path link, final Path target) throws IOException {
        Files.createSymbolicLink(link, target, new FileAttribute[0]);
    }

    private static Path follow(final Path link) throws IOException {
        return link.toRealPath();
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
        final var hash = BigInteger.valueOf(chunk.hashCode()).toByteArray(); // TODO: actual hash? maybe world time
        final var time = Instant.now().getEpochSecond();

        final var p = Diff.dimPath().resolve(pos.x + " " + pos.z);
        final var file = p.resolve(String.valueOf(time));
        final var latest = p.resolve(paths.latest);

        try {
            if (exists(latest.resolve(paths.hash))) {
                final var latestName = follow(latest).getFileName();
                if (Arrays.equals(Files.readAllBytes(latest.resolve(paths.hash)), hash))
                    symlink(file, latestName);
                else {
                    final var lastTime = Long.parseLong(latestName.toString());
                    if (time - lastTime < minTime.get())
                        FileUtils.deleteDirectory(follow(latest).toFile());
                }
                delete(latest);
            }

            if (!exists(file)) {
                mkdirs(file);
                write(file.resolve(paths.hash), hash);
                if (saveMap.get())
                    write(file.resolve("map"), new byte[0]);
                if (saveBlocks.get())
                    write(file.resolve("blocks"), new byte[0]);
                if (saveEntities.get())
                    write(file.resolve("entities"), new byte[0]);
            }
            symlink(latest, file.getFileName());
        } catch (final Exception e) {
            info(e.getClass().getSimpleName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }
}
