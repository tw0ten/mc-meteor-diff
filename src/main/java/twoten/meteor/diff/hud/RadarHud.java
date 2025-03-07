package twoten.meteor.diff.hud;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static twoten.meteor.diff.Diff.s;

import java.nio.file.Files;
import java.nio.file.Path;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff.paths;
import twoten.meteor.diff.modules.SaveDiff;

public class RadarHud extends HudElement {
    public static final HudElementInfo<RadarHud> INFO = new HudElementInfo<>(
            Addon.HUD_GROUP,
            "radar",
            "Diff minimap.",
            RadarHud::new);

    public static Color[][] map(final Chunk c) {
        final var out = new Color[s][s];

        final var pos = new BlockPos(c.getPos().x * s, -1, c.getPos().z * s);
        final var height = c.getHeightmap(Heightmap.Type.WORLD_SURFACE);

        for (var x = 0; x < s; x++) {
            for (var z = 0; z < s; z++) {
                final var p = pos.add(x, height.get(x, z), z);
                final var block = c.getBlockState(p);
                final var color = new Color(block.getMapColor(mc.world, p).color);
                out[x][z] = color;
            }
        }

        return out;
    }

    private static int unsign(final byte b) {
        return b < 0 ? 0x100 + b : b;
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> chunks = sgGeneral.add(new IntSetting.Builder()
            .name("chunks")
            .description("The width and height of the radar in chunks")
            .defaultValue(10)
            .onChanged((i) -> {
                data = new Color[i][i][s][s];
                calculateSize();
            })
            .build());

    private final Setting<Integer> opacity = sgGeneral.add(new IntSetting.Builder()
            .name("opacity")
            .defaultValue(200)
            .range(0, 0xFF)
            .sliderRange(0, 0xFF)
            .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .defaultValue(1)
            .onChanged((i) -> calculateSize())
            .build());

    private Color[][][][] data;

    public RadarHud() {
        super(INFO);

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void render(final HudRenderer r) {
        for (var x = 0; x < data.length; x++)
            for (var z = 0; z < data[x].length; z++) {
                final var chunk = data[x][z];
                if(chunk == null) continue;
                renderChunk(r, getX() + x * s * scale.get(), getY() + z * s * scale.get(), chunk);
            }
    }

    private void calculateSize() {
        final var chunks = this.chunks.get();
        final var scale = this.scale.get();
        setSize(s * scale * chunks, s * scale * chunks);
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        for (var x = 0; x < data.length; x++)
            for (var z = 0; z < data[x].length; z++) {
                final var chunkpos = new ChunkPos(
                        (int) mc.player.getPos().x / s + x - data.length / 2,
                        (int) mc.player.getPos().z / s + z - data[x].length / 2);
                if (event.chunk().getPos().equals(chunkpos)) {
                    data[x][z] = map(event.chunk());
                    return;
                }
            }
    }

    private Color[][] loadChunk(final Path p) {
        final var chunk = new Color[s][s];
        try {
            final var bytes = Files.readAllBytes(p.resolve(paths.chunk.map));
            for (var x = 0; x < chunk.length; x++)
                for (var z = 0; z < chunk[x].length; z++) {
                    final var i = (x * s + z) * SaveDiff.colorBytes;
                    chunk[x][z] = new Color(
                            unsign(bytes[i + 0]),
                            unsign(bytes[i + 1]),
                            unsign(bytes[i + 2]));
                }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return chunk;
    }

    private void renderChunk(final HudRenderer r, final double x, final double y, final Color[][] colors) {
        final var scale = this.scale.get();
        final var opacity = this.opacity.get();
        for (var i = 0; i < s; i++)
            for (var j = 0; j < s; j++) {
                final var color = colors[i][j];
                r.quad(x + i * scale, y + j * scale,
                        scale, scale,
                        color.a(opacity));
            }
    }
}
