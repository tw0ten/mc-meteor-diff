package twoten.meteor.diff.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import twoten.meteor.diff.Addon;

public class VisualDiff extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of the marker.")
            .defaultValue(Color.MAGENTA)
            .build());

    public VisualDiff() {
        super(Addon.CATEGORY, "visual-diff", "Render changes.");
    }

    @EventHandler
    private void onRender3d(final Render3DEvent event) {
        Box marker = new Box(BlockPos.ORIGIN);
        marker = marker.stretch(
                marker.getLengthX(),
                marker.getLengthY(),
                marker.getLengthZ());

        event.renderer.box(marker, color.get(), color.get(), ShapeMode.Both, 0);
    }
}
