package twoten.meteor.diff;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.Heightmap;

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

    public static Map<ChunkPos, Color[][]> chunks = new HashMap<>();

    public static Color[][] map(final Chunk c) {
        final var out = new Color[s][s];

        final var height = c.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        for (var x = 0; x < s; x++)
            for (var z = 0; z < s; z++) {
                final var p = c.getPos().getBlockPos(x, height.get(x, z) - 1, z);
                final var block = c.getBlockState(p);
                out[x][z] = new Color(block.getMapColor(mc.world, p).color);
            }

        return out;
    }

    public static void cache(Chunk c) {
        chunks.put(c.getPos(), map(c));
    }
}
