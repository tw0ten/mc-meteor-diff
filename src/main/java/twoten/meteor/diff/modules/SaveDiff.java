package twoten.meteor.diff.modules;

import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff;

public class SaveDiff extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> onLoad = sgGeneral.add(new BoolSetting.Builder().name("on-load")
            .description("Save chunks after they are loaded.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> onUnload = sgGeneral.add(new BoolSetting.Builder().name("on-unload")
            .description("Save chunks when they are unloaded.")
            .defaultValue(true)
            .build());

    public SaveDiff() {
        super(Addon.CATEGORY, "save-diff", "Download chunks.");
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        final var chunk = event.chunk();
        info("onChunkData " + chunk.getPos());
        info(Diff.getPath().toString());
    }
}
