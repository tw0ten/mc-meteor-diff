package twoten.meteor.diff.commands;

import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import twoten.meteor.diff.Diff;
import twoten.meteor.diff.Diff.paths;

public class DiffCommand extends Command {
    public DiffCommand() {
        super("diff", "Control diff.");
    }

    @Override
    public void build(final LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("hi");
            return SINGLE_SUCCESS;
        });

        builder.then(literal("clean").then(argument("server", StringArgumentType.word()).executes(context -> {
            final var server = StringArgumentType.getString(context, "server");
            final var p = Diff.root.resolve(server);
            new Thread(getName() + " clean " + server) {
                @Override
                public void run() {
                    info(getName());
                    for (final var d : p.toFile().listFiles()) {
                        info(getName() + "/" + d.getName());
                        for (final var cpos : d.listFiles()) {
                            try {
                                final var latest = cpos.toPath().resolve(paths.latest);
                                final var realLatest = latest.toRealPath();
                                for (final var i : cpos.listFiles()) {
                                    if (i.toPath().equals(realLatest) || i.toPath().equals(latest))
                                        continue;
                                    if (Files.isSymbolicLink(i.toPath()))
                                        i.delete();
                                    else
                                        FileUtils.deleteDirectory(i);
                                }
                            } catch (final Exception e) {
                                continue;
                            }
                        }
                    }
                    info(getName() + ": done");
                }
            }.start();
            return SINGLE_SUCCESS;
        })));
    }
}
