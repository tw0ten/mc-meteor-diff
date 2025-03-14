package twoten.meteor.diff.hud;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static twoten.meteor.diff.Diff.s;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
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
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff;

public class RadarHud extends HudElement {
    public static final HudElementInfo<RadarHud> INFO = new HudElementInfo<>(
            Addon.HUD_GROUP,
            "radar",
            "Diff minimap.",
            RadarHud::new);

    // TODO: textures or something or a shader idk
    private final Map<Long, Color[][]> cache = new HashMap<>();

    private int chunks;
    private Color[][][][] data;
    private ChunkPos refPos = ChunkPos.ORIGIN;
    private final Set<Chunk> update = new HashSet<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
            .name("size")
            .description("The width and height of the radar in blocks.")
            .defaultValue(11 * s)
            .min(1)
            .onChanged((i) -> {
                chunks = (i + s - 1) / s + 2;
                data = new Color[chunks][chunks][s][s];
                calculateSize();
                if (mc.player != null)
                    update();
            })
            .build());

    private final SettingGroup sgScale = settings.createGroup("Scale");

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The global scale.")
            .defaultValue(1)
            .onChanged((i) -> calculateSize())
            .build());

    private final Setting<Double> scaleSelf = sgScale.add(new DoubleSetting.Builder()
            .name("self")
            .defaultValue(2)
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
        calculateSize();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void render(final HudRenderer r) {
        if (mc.world == null)
            return;

        final var size = this.size.get();
        final var opacity = this.opacity.get();
        final var scale = this.scale.get();

        for (var x = 0; x < size; x++)
            for (var z = 0; z < size; z++) {
                final var ax = x + mc.player.getBlockX() - size / 2 - refPos.getStartPos().getX();
                final var az = z + mc.player.getBlockZ() - size / 2 - refPos.getStartPos().getZ();
                if (ax < 0 || az < 0 || ax >= chunks * s || az >= chunks * s)
                    continue;
                final var chunk = data[ax / s][az / s];
                if (chunk == null)
                    continue;
                final var color = chunk[ax % s][az % s];
                r.quad(getX() + x * scale, getY() + z * scale, scale, scale, color.a(opacity));
            }

        {
            final var mx = getX() + getWidth() / 2;
            final var my = getY() + getHeight() / 2;
            final var s = scale * scaleSelf.get();
            final var angle = mc.player.headYaw / 180 * Math.PI;
            final var vertices = new double[][] {
                    { 0, s * 2 },
                    { -s, -s },
                    { s, -s }
            };
            r.triangle(
                    mx + (vertices[0][0] * scale * Math.cos(angle) - vertices[0][1] * scale * Math.sin(angle)),
                    my + (vertices[0][0] * scale * Math.sin(angle) + vertices[0][1] * scale * Math.cos(angle)),
                    mx + (vertices[1][0] * scale * Math.cos(angle) - vertices[1][1] * scale * Math.sin(angle)),
                    my + (vertices[1][0] * scale * Math.sin(angle) + vertices[1][1] * scale * Math.cos(angle)),
                    mx + (vertices[2][0] * scale * Math.cos(angle) - vertices[2][1] * scale * Math.sin(angle)),
                    my + (vertices[2][0] * scale * Math.sin(angle) + vertices[2][1] * scale * Math.cos(angle)),
                    selfColor.get());
        }
    }

    private void cache(final Chunk c) {
        cache.put(c.getPos().toLong(), Diff.map(c));
    }

    private void calculateSize() {
        final var scale = this.scale.get();
        final var size = this.size.get();
        setSize(scale * size, scale * size);
    }

    @EventHandler
    private void onBlockUpdate(final BlockUpdateEvent event) {
        if (!isActive())
            return;
        update.add(mc.world.getChunk(event.pos));
    }

    @EventHandler
    private void onPacketReceive(final PacketEvent.Receive event) {
        if (!isActive())
            return;
        if (!(event.packet instanceof final UnloadChunkS2CPacket p))
            return;
        cache.remove(p.pos().toLong());
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        if (!isActive())
            return;
        update.add(event.chunk());
        cache(event.chunk());
        update();
    }

    @EventHandler
    private void onGameLeft(final GameLeftEvent e) {
        update.clear();
        cache.clear();
    }

    @EventHandler
    private void onTick(final TickEvent.Post event) {
        if (!isActive())
            return;
        if (!update.isEmpty()) {
            update.forEach(this::cache);
            update.clear();
            update();
        }
    }

    private void update() {
        final var p = mc.player.getChunkPos();
        refPos = new ChunkPos(p.x - chunks / 2, p.z - chunks / 2);
        for (var x = 0; x < chunks; x++)
            for (var z = 0; z < chunks; z++)
                data[x][z] = cache.get(new ChunkPos(refPos.x + x, refPos.z + z).toLong());
    }
}
