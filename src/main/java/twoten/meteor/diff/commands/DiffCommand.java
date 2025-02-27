package twoten.meteor.diff.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

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

        builder.then(literal("name").then(argument("nameArgument", StringArgumentType.word()).executes(context -> {
            final String argument = StringArgumentType.getString(context, "nameArgument");
            info("hi, " + argument);
            return SINGLE_SUCCESS;
        })));
    }
}
