package twoten.meteor.diff.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import twoten.meteor.diff.Addon;

public class VisualDiff extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
            .name("blocks")
            .description("Render block changes.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> entities = sgGeneral.add(new BoolSetting.Builder()
            .name("entities")
            .description("Render entity changes.")
            .defaultValue(false)
            .build());

    public VisualDiff() {
        super(Addon.CATEGORY, "visual-diff", "Render changes.");
    }

    @EventHandler
    private void onRender3d(final Render3DEvent event) {
    }
}
