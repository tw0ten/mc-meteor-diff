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
import twoten.meteor.diff.Diff;

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

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> chunks = sgGeneral.add(new IntSetting.Builder()
            .name("chunks")
            .description("The width and height of the radar in chunks")
            .defaultValue(11)
            .min(1)
            .onChanged((i) -> {
                data = new Color[i][i][s][s];
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

    private final Setting<Integer> opacity2 = sgColor.add(new IntSetting.Builder()
            .name("opacity2")
            .description("Opacity 2: electric boogaloo.")
            .defaultValue(195)
            .range(0, 0xFF)
            .sliderRange(0, 0xFF)
            .build());

    private final Setting<SettingColor> selfColor = sgColor.add(new ColorSetting.Builder()
            .name("self")
            .defaultValue(Color.WHITE)
            .build());

    private Color[][][][] data; // TODO: blocks not chunks
    private boolean update = false;

    public RadarHud() {
        super(INFO);

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void render(final HudRenderer r) {
        final var scale = this.scale.get();
        for (var x = 0; x < data.length; x++)
            for (var z = 0; z < data[x].length; z++) {
                final var chunk = data[x][z];
                final var opacity = (x + z) % 2 == 0 ? this.opacity.get() : this.opacity2.get();
                Diff.renderChunk(r, getX() + x * s * scale, getY() + z * s * scale, scale, opacity, chunk);
            }
        if (true)
            return;
        final var x = getX() + scale * (data.length / 2 * s + mc.player.getPos().x % s);
        final var z = getY() + scale * (data[0].length / 2 * s + mc.player.getPos().z % s);
        r.triangle(x, z, x + 4, z + 4, x + 4, z + 0, selfColor.get());
    }

    private void calculateSize() {
        final var chunks = this.chunks.get();
        final var scale = this.scale.get();
        setSize(s * scale * chunks, s * scale * chunks);
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
            update();
            update = false;
        }
    }

    private void update() {
        if (!isActive())
            return;

        final var chunkpos = new ChunkPos(
                mc.player.getChunkPos().x - chunks.get() / 2,
                mc.player.getChunkPos().z - chunks.get() / 2);
        for (var x = 0; x < data.length; x++)
            for (var z = 0; z < data[x].length; z++)
                data[x][z] = map(mc.world.getChunk(chunkpos.x + x, chunkpos.z + z));
    }

}
