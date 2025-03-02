package twoten.meteor.diff.hud;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import twoten.meteor.diff.Addon;

public class DiffHud extends HudElement {
    public static final HudElementInfo<DiffHud> INFO = new HudElementInfo<>(
            Addon.HUD_GROUP,
            "diff-hud",
            "Diff hud element.",
            DiffHud::new);

    public DiffHud() {
        super(INFO);
    }

    @Override
    public void render(final HudRenderer renderer) {
        final var h = renderer.textHeight(true);
        final var w = Math.max(renderer.textWidth("diff", true), renderer.textWidth("++++", true));
        setSize(w, h * 4);

        renderer.text("diff", x, y + h * 0, Color.WHITE, true);
        renderer.text("date", x, y + h * 1, Color.WHITE, true);
        renderer.text("++++", x, y + h * 2, Color.GREEN, true);
        renderer.text("---", x, y + h * 3, Color.RED, true);
    }
}
