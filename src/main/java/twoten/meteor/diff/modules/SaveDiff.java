package twoten.meteor.diff.modules;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static twoten.meteor.diff.Diff.s;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.world.chunk.Chunk;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff;
import twoten.meteor.diff.Diff.paths;

public class SaveDiff extends Module {
    public static final int colorBytes = Integer.BYTES - 1;

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
                Math.random() < 0.5 ? (byte) 0 : 1,
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
            .name("map")
            .description("Save the map view.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> saveNew = sgSave.add(new BoolSetting.Builder()
            .name("new")
            .description("Detect whether the chunk is freshly generated.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> saveBlocks = sgSave.add(new BoolSetting.Builder()
            .name("blocks")
            .description("Save block data.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> saveEntities = sgSave.add(new BoolSetting.Builder()
            .name("entities")
            .description("Save entity data.")
            .defaultValue(true)
            .build());

    private final SettingGroup sgStorage = this.settings.createGroup("Storage");

    private final Setting<Boolean> async = sgStorage.add(new BoolSetting.Builder()
            .name("async")
            .description("Save each chunk in its own thread.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> zip = sgStorage.add(new BoolSetting.Builder()
            .name("zip")
            .description("Compress saved data.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> maxDepth = sgStorage.add(new IntSetting.Builder()
            .name("max-depth")
            .description("Maximum number of unique chunk versions, 0 to disable.")
            .min(0)
            .defaultValue(0)
            .sliderMin(1)
            .build());

    private final Setting<Integer> minTime = sgStorage.add(new IntSetting.Builder()
            .name("min-time")
            .description("The time (seconds) to wait before saving to a new iteration instead of overwriting the last.")
            .defaultValue(60 * 60)
            .min(1)
            .sliderMax(5 * 60 * 60)
            .build());

    public SaveDiff() {
        super(Addon.CATEGORY, "save-diff", "Download chunks.");
    }

    private void save(final Chunk chunk) {
        final var time = System.currentTimeMillis();
        if (!async.get())
            save(chunk, time);
        else
            new Thread(() -> save(chunk, time), getClass().getSimpleName() + " " + chunk.getPos() + " " + time).start();
    }

    private void save(final Chunk chunk, final long time) {
        final var hash = hash(chunk);

        final var p = Diff.chunkPath(chunk.getPos());
        final var file = p.resolve(String.valueOf(time));
        final var latest = p.resolve(paths.latest);

        try {
            if (exists(latest)) {
                final var realLatest = follow(latest);
                final var lastTime = Long.parseLong(realLatest.getFileName().toString());
                if (lastTime == time)
                    return;
                if (Arrays.equals(Files.readAllBytes(latest.resolve(paths.hash)), hash)) {
                    if (!exists(file))
                        symlink(file, realLatest.getFileName());
                } else if (time - lastTime < minTime.get() * 1000) {
                    FileUtils.deleteDirectory(realLatest.toFile());
                    symlink(realLatest, file.getFileName());
                }
                delete(latest);
            }

            if (!exists(file)) {
                mkdirs(file);
                write(file.resolve(paths.hash), hash);

                if (saveMap.get())
                    write(file.resolve(paths.chunk.map), map(chunk));
                if (saveNew.get())
                    write(file.resolve(paths.chunk.isNew), new byte[] { isNew(chunk) ? (byte) 0 : 1 });
                if (saveBlocks.get())
                    write(file.resolve(paths.chunk.blocks), blocks(chunk));
                if (saveEntities.get())
                    write(file.resolve(paths.chunk.entities), entities(chunk));
            }

            symlink(latest, follow(file).getFileName());
        } catch (final Exception e) {
            e.printStackTrace();
            info(e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        if (!onLoad.get())
            return;
        save(event.chunk());
    }

    @EventHandler
    private void onPacketReceive(final PacketEvent.Receive event) {
        if (!onUnload.get())
            return;
        if (!(event.packet instanceof final UnloadChunkS2CPacket p))
            return;
        save(mc.world.getChunk(p.pos().x, p.pos().z));
    }

    private byte[] map(final Chunk c) {
        final var colors = Diff.map(c);
        final var out = new byte[s * s * colorBytes];

        for (var x = 0; x < s; x++)
            for (var z = 0; z < s; z++) {
                final var color = colors[x][z];
                final var i = (x * s + z) * colorBytes;
                out[i + 0] = (byte) color.r;
                out[i + 1] = (byte) color.g;
                out[i + 2] = (byte) color.b;
            }

        return out;
    }

    private boolean isNew(final Chunk c) {
        return true;
    }

    private byte[] blocks(final Chunk c) {
        return new byte[0];
    }

    private byte[] entities(final Chunk c) {
        return new byte[0];
    }
}
