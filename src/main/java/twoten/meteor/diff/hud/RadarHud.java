package twoten.meteor.diff.hud;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static twoten.meteor.diff.Diff.s;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import twoten.meteor.diff.Addon;

public class RadarHud extends HudElement {
    public static final HudElementInfo<RadarHud> INFO = new HudElementInfo<>(
            Addon.HUD_GROUP,
            "radar",
            "Diff minimap.",
            RadarHud::new);

    public static Color[][] map(final Chunk c) {
        final var out = new Color[s][s];

        final var height = c.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        for (var x = 0; x < s; x++)
            for (var z = 0; z < s; z++) {
                final var p = c.getPos().getBlockPos(x, height.get(x, z) - 1, z);
                final var block = c.getBlockState(p);
                out[x][z] = new Color(block.getMapColor(mc.world, p).color);
            }

        return out;
    }

    private int chunks;
    private Color[][] data;
    private ChunkPos refPos;
    private boolean update = false;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
            .name("size")
            .description("The width and height of the radar in blocks.")
            .defaultValue(11 * s)
            .min(1)
            .onChanged((i) -> {
                chunks = i / s + 3;
                data = new Color[chunks * s][chunks * s];
                calculateSize();
                update = true;
            })
            .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .defaultValue(1)
            .onChanged((i) -> calculateSize())
            .build());

    private final SettingGroup sgColor = settings.createGroup("Color");

    private final Setting<Integer> opacity = sgColor.add(new IntSetting.Builder()
            .name("opacity")
            .defaultValue(200)
            .range(0, 0xFF)
            .sliderRange(0, 0xFF)
            .build());

    private final Setting<SettingColor> selfColor = sgColor.add(new ColorSetting.Builder()
            .name("self")
            .defaultValue(Color.WHITE)
            .build());

    public RadarHud() {
        super(INFO);

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void render(final HudRenderer r) {
        final var size = this.size.get();
        final var opacity = this.opacity.get();
        final var scale = this.scale.get();
        for (var x = 0; x < size; x++)
            for (var z = 0; z < size; z++) {
                Color color;
                try {
                    color = data[s + x + mc.player.getBlockX() - refPos.x * s - 1][s + z + mc.player.getBlockZ()
                            - refPos.z * s - 1];
                    if (color == null)
                        continue;
                } catch (Exception e) {
                    continue;
                }
                r.quad(getX() + x * scale, getY() + z * scale, scale, scale, color.a(opacity));
            }
    }

    private void calculateSize() {
        final var scale = this.scale.get();
        final var size = this.size.get();
        setSize(scale * size, scale * size);
    }

    @EventHandler
    private void onBlockUpdate(final BlockUpdateEvent event) {
        update = true;
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        update();
    }

    @EventHandler
    private void onTick(final TickEvent.Post event) {
        if (update && mc.world != null) {
            update = false;
            update();
        }
    }

    private void update() {
        if (!isActive())
            return;

        refPos = new ChunkPos(
                mc.player.getChunkPos().x - chunks / 2,
                mc.player.getChunkPos().z - chunks / 2);
        for (var x = 0; x < chunks; x++)
            for (var z = 0; z < chunks; z++) {
                final var c = map(mc.world.getChunk(refPos.x + x, refPos.z + z));
                for (var nx = 0; nx < s; nx++)
                    for (var nz = 0; nz < s; nz++)
                        data[x * s + nx][z * s + nz] = c[nx][nz];
            }
        refPos = mc.player.getChunkPos();
    }
}
