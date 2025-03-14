package twoten.meteor.diff;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.nio.file.Path;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

public class Diff {
    public interface paths {
        interface chunk {
            Path map = Path.of("map");
            Path isNew = Path.of("new");
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

    public static Path chunkPath(final ChunkPos p) {
        return dimPath().resolve(p.x + " " + p.z);
    }

    public static Color[][] map(final Chunk c) {
        final var out = new Color[s][s];

        final var height = c.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        // TODO: nether
        for (var x = 0; x < out.length; x++)
            for (var z = 0; z < out[x].length; z++) {
                final var p = c.getPos().getBlockPos(x, height.get(x, z) - 1, z);
                final var block = c.getBlockState(p);
                out[x][z] = new Color(block.getMapColor(mc.world, p).color);
            }

        return out;
    }

}
