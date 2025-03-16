package twoten.meteor.diff.hud;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.ChunkPos;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff;

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
    public void render(final HudRenderer r) {
        if (mc.world == null || true)
            return;

        final var pos = mc.player.getChunkPos();
        final var info = new Diff.ChunkInfo(Diff.chunkPath(pos));

        final var dh = r.textHeight(true);
        var h = getY();
        var w = 0d;

        w = Math.max(w, text(r, pos.x + " " + pos.z, x, h, Color.WHITE, true));
        w = Math.max(w, text(r, "new: " + info.isNew(), x, h, Color.WHITE, true));

        setSize(w, h);
    }

    private double text(HudRenderer r, String s, int x, int y, Color c, boolean shadow) {
        r.text(s, x, y, c, shadow);
        return r.textWidth(s, shadow);
    }
}
