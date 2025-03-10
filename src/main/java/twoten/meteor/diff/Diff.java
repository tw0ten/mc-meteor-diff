package twoten.meteor.diff;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.nio.file.Path;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.chunk.Chunk;

public class Diff {
    public interface paths {
        interface chunk {
            Path map = Path.of("map");
            Path blocks = Path.of("blocks");
            Path entities = Path.of("entities");
        }

        Path hash = Path.of(".hash");
        Path latest = Path.of(String.valueOf(0L));
    }

    public static final int s = Chunk.field_54147;

    public static final Path root = MeteorClient.FOLDER.toPath().resolve("diff");

    public static Path worldPath() {
        return root.resolve(Utils.getFileWorldName());
    }

    public static Path dimPath() {
        return worldPath().resolve(mc.world.getRegistryKey().getValue().toString());
    }

    public static void renderChunk(final HudRenderer r,
            final double x, final double y,
            final double scale,
            final int opacity, final Color[][] colors) {
        for (var i = 0; i < s; i++)
            for (var j = 0; j < s; j++) {
                final var color = colors[i][j];
                if (color == null)
                    return;
                r.quad(x + i * scale, y + j * scale, scale, scale, color.a(opacity));
            }
    }

    public static void renderChunk(final DrawContext r,
            final int x, final int y,
            final int scale,
            final int[][] colors) {
        for (var i = 0; i < s; i++)
            for (var j = 0; j < s; j++) {
                final var rx = x + i * scale;
                final var ry = y + j * scale;
                r.fill(rx, ry, rx + scale, ry + scale, colors[i][j]);
            }
    }
}
