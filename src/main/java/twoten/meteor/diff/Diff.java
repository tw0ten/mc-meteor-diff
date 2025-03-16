package twoten.meteor.diff;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.nio.file.Files;
import java.nio.file.Path;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

public class Diff {
    public interface paths {
        interface pos {
            interface chunk {
                Path map = Path.of("map");
                Path blocks = Path.of("blocks");
                Path entities = Path.of("entities");
            }

            Path isNew = Path.of("new");
            Path latest = Path.of(String.valueOf(0L));
        }

        Path hash = Path.of(".hash");
    }

    public static class ChunkInfo {
        private final Path p;
        private final Path c;

        public ChunkInfo(final Path p) {
            this.p = p;
            this.c = p.resolve(paths.pos.latest);
        }

        public Boolean isNew() {
            try {
                return Files.readAllBytes(p.resolve(paths.pos.isNew))[0] == 0;
            } catch (final Exception e) {
                return null;
            }
        }

        public Path[] versions() {
            try {
                return Files.list(p)
                        .filter(p -> !p.getFileName().equals(paths.pos.latest.getFileName()))
                        .toArray(Path[]::new);
            } catch (final Exception e) {
                return new Path[0];
            }
        }

        public long newest() {
            try {
                return Long.parseLong(c.toRealPath().toFile().getName());
            } catch (final Exception e) {
                return 0;
            }
        }

        public long oldest() {
            try {
                return Long.parseLong(p.toFile().list()[1]);
            } catch (final Exception e) {
                return 0;
            }
        }
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
        return dimPath().resolve(posToString(p));
    }

    public static String posToString(final ChunkPos p) {
        return p.x + " " + p.z;
    }

    public static Color[][] map(final Chunk c) {
        final var out = new Color[s][s];

        final var height = c.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        // TODO: nether
        for (var x = 0; x < s; x++)
            for (var z = 0; z < s; z++) {
                final var p = c.getPos().getBlockPos(x, height.get(x, z) - 1, z);
                final var block = c.getBlockState(p);
                out[x][z] = new Color(block.getMapColor(mc.world, p).color);
            }

        return out;
    }
}
