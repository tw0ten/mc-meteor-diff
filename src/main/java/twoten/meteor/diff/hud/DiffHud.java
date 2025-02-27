package twoten.meteor.diff.hud;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import twoten.meteor.diff.Addon;

public class DiffHud extends HudElement {
    public static final HudElementInfo<DiffHud> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "diff-hud",
            "Diff hud element.", DiffHud::new);

    public DiffHud() {
        super(INFO);
    }

    @Override
    public void render(final HudRenderer renderer) {
        setSize(renderer.textWidth("Example element", true), renderer.textHeight(true));

        renderer.quad(x, y, getWidth(), getHeight(), Color.LIGHT_GRAY);

        renderer.text("Example element", x, y, Color.WHITE, true);
        renderer.text("++++", x, y + renderer.textHeight(), Color.GREEN, true);
        renderer.text("---", x, y + renderer.textHeight() * 2, Color.RED, true);
    }
}
