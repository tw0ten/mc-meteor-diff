package twoten.meteor.diff;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.nio.file.Path;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.world.chunk.Chunk;

public class Diff {
    public static final int s = Chunk.field_54147;

    public static final Path root = MeteorClient.FOLDER.toPath().resolve("diff");

    public static Path worldPath() {
        return root.resolve(Utils.getFileWorldName());
    }

    public static Path dimPath() {
        return worldPath().resolve(mc.world.getRegistryKey().getValue().toString());
    }

    public interface paths {
        interface chunk {
            Path map = Path.of("map");
            Path blocks = Path.of("blocks");
            Path entities = Path.of("entities");
        }

        Path hash = Path.of(".hash");
        Path latest = Path.of(String.valueOf(0L));
    }
}
