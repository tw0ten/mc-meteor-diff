package twoten.meteor.diff.hud;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import twoten.meteor.diff.Addon;

public class RadarHud extends HudElement {
    public static final HudElementInfo<DiffHud> INFO = new HudElementInfo<>(
            Addon.HUD_GROUP,
            "radar",
            "Diff minimap.",
            DiffHud::new);

    public RadarHud() {
        super(INFO);
    }

    @Override
    public void render(final HudRenderer renderer) {
    }
}
