package twoten.meteor.diff.modules;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;

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

    private static boolean mkdir(final Path path) {
        return path.toFile().mkdir();
    }

    private static boolean mkdirs(final Path path) {
        return path.toFile().mkdirs();
    }

    private static void symlink(final Path link, final Path target) throws IOException {
        Files.createSymbolicLink(link, target, new FileAttribute[0]);
    }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> onLoad = sgGeneral.add(new BoolSetting.Builder().name("on-load")
            .description("Save chunks after they are loaded.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> onUnload = sgGeneral.add(new BoolSetting.Builder().name("on-unload")
            .description("Save chunks when they are unloaded.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> maxDepth = sgGeneral.add(new IntSetting.Builder().name("max-depth")
            .description("Maximum number of unique chunk versions, 0 to disable.")
            .defaultValue(0)
            .min(0)
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
        final var hash = BigInteger.valueOf(chunk.hashCode()).toByteArray(); // TODO: actual hash?
        final var time = String.valueOf(System.currentTimeMillis());

        final var p = Diff.dimPath().resolve(pos.x + " " + pos.z);
        final var file = p.resolve(time);

        try {
            {
                final var latest = p.resolve(paths.latest);
                if (exists(latest.resolve(paths.hash))) {
                    if (Arrays.equals(Files.readAllBytes(latest.resolve(paths.hash)), hash))
                        symlink(file, latest.toRealPath());
                    delete(latest);
                }
                mkdirs(p);
                symlink(latest, file);
            }

            if (!exists(file)) {
                mkdir(file);
                write(file.resolve(paths.hash), hash);
                Files.writeString(file.resolve("map"), "");
                Files.writeString(file.resolve("chunk"), "");
                Files.writeString(file.resolve("entities"), "");
            }
        } catch (final Exception e) {
            info(e.getClass().getSimpleName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }
}
