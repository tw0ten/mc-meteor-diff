package twoten.meteor.diff.hud;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import twoten.meteor.diff.Addon;

public class DiffHud extends HudElement {
    public static final HudElementInfo<DiffHud> INFO = new HudElementInfo<>(
            Addon.HUD_GROUP,
            "diff",
            "Diff hud element.",
            DiffHud::new);

    public DiffHud() {
        super(INFO);
    }

    @Override
    public void render(final HudRenderer renderer) {
        final var h = renderer.textHeight(true);
        final var w = Math.max(renderer.textWidth("date", true), renderer.textWidth("    ", true));
        setSize(w, h * 2);

        renderer.text("date", x, y + h * 0, Color.WHITE, true);
        final var add = "+";
        renderer.text(add, x, y + h * 1, Color.GREEN, true);
        renderer.text("---", x + renderer.textWidth(add, true), y + h * 1, Color.RED, true);
    }
}
