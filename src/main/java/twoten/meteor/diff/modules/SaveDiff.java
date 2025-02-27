package twoten.meteor.diff.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import twoten.meteor.diff.Addon;

public class SaveDiff extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> onLoad = new BoolSetting.Builder().name("on-load")
            .description("Save chunks when they are loaded.")
            .defaultValue(false)
            .build();

    private final Setting<Boolean> onUnload = new BoolSetting.Builder().name("on-unload")
            .description("Save chunks when they are unloaded.")
            .defaultValue(true)
            .build();

    public SaveDiff() {
        super(Addon.CATEGORY, "save-diff", "Download chunks.");
    }

}
