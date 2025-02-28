package twoten.meteor.diff;

import java.nio.file.Path;
import java.nio.file.Paths;

import meteordevelopment.meteorclient.utils.Utils;

public class Diff {
    public static Path getPath() {
        return Paths.get("diff", Utils.getFileWorldName());
    }
}
