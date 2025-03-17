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
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import twoten.meteor.diff.Addon;
import twoten.meteor.diff.Diff;

public class RadarHud extends HudElement {
    // TODO: fucking malloc.c fails an assertion this is cooked.
    // has to be something with the eventbus
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
            .defaultValue(11 * s - 1)
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

        RainbowColors.register(() -> {
            selfColor.get().update();
        });

        calculateSize();

        toggle();
        toggle();
    }

    @Override
    public void toggle() {
        super.toggle();
        if (isActive()) {
            MeteorClient.EVENT_BUS.subscribe(this);
        } else {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            clear();
        }
    }

    @Override
    public void render(final HudRenderer r) {
        if (mc.world == null)
            return;

        final var size = this.size.get();
        final var opacity = this.opacity.get();
        final var scale = this.scale.get();

        final var sx = mc.player.getBlockX() - size / 2 - refPos.getStartPos().getX();
        final var sy = mc.player.getBlockZ() - size / 2 - refPos.getStartPos().getZ();

        for (var x = 0; x < size; x++)
            for (var z = 0; z < size; z++) {
                final var ax = x + sx;
                final var az = z + sy;
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
            final var angle = mc.player.headYaw / 180 * Math.PI;
            final var cos = Math.cos(angle);
            final var sin = Math.sin(angle);
            final var s = scale * scaleSelf.get();
            final var v = new double[][] { { 0, s * 2 }, { -s, -s }, { s, -s } };
            r.triangle(
                    mx + (v[0][0] * cos - v[0][1] * sin), my + (v[0][0] * sin + v[0][1] * cos),
                    mx + (v[1][0] * cos - v[1][1] * sin), my + (v[1][0] * sin + v[1][1] * cos),
                    mx + (v[2][0] * cos - v[2][1] * sin), my + (v[2][0] * sin + v[2][1] * cos),
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
        update.add(mc.world.getChunk(event.pos));
    }

    @EventHandler
    private void onPacketReceive(final PacketEvent.Receive event) {
        switch (event.packet) {
            case final UnloadChunkS2CPacket p:
                cache.remove(p.pos().toLong());
                break;
            case final PlayerRespawnS2CPacket p:
                clear();
                break;
            default:
                return;
        }
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        cache(event.chunk());
        update();
    }

    @EventHandler
    private void onGameLeft(final GameLeftEvent e) {
        clear();
    }

    @EventHandler
    private void onTick(final TickEvent.Pre event) {
        if (update.isEmpty())
            return;
        update.forEach(this::cache);
        update.clear();
        update();
    }

    private void update() {
        final var p = mc.player.getChunkPos();
        refPos = new ChunkPos(p.x - chunks / 2, p.z - chunks / 2);
        for (var x = 0; x < chunks; x++)
            for (var z = 0; z < chunks; z++)
                data[x][z] = cache.get(new ChunkPos(refPos.x + x, refPos.z + z).toLong());
    }

    private void clear() {
        update.clear();
        cache.clear();
    }
}
