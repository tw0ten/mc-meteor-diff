package twoten.meteor.diff.tabs;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static twoten.meteor.diff.Diff.s;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import twoten.meteor.diff.Diff;
import twoten.meteor.diff.Diff.paths;
import twoten.meteor.diff.modules.SaveDiff;

public class MapTab extends Tab {
    private static class DiffMap extends System<DiffMap> {
        private class MapScreen extends Screen {
            private int x = 0, z = 0;
            private int w = 0, h = 0;
            private final Map<Long, int[][]> cache = new HashMap<>();
            private boolean close = false;

            private final BlockPos.Mutable bp = new BlockPos.Mutable();

            public MapScreen() {
                super(Text.of(getName()));

                x = mc.player.getBlockX();
                z = mc.player.getBlockZ();
                w = mc.getWindow().getWidth();
                h = mc.getWindow().getHeight();

                // TODO: laggy af even thoruhg its a sep thread????
                // 😭😭😭😭😭😭😭😭🥶🥶🥶🤒🤒🤒🤒🤒🤒🤢🤒 <- me
                new Thread(getName()) {
                    private final BlockPos.Mutable bp = new BlockPos.Mutable();

                    public void run() {
                        while (!close) {
                            try {
                                synchronized (cache) {
                                    update();
                                }
                                sleep(1000);
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    private void update() {
                        for (var x1 = 0; x1 < w / s; x1++)
                            for (var z1 = 0; z1 < h / s; z1++) {
                                bp.set(x1 * s + x - w / 2, 0, z1 * s + z - h / 2);
                                final var pos = ChunkPos.toLong(bp);
                                if (cache.containsKey(pos))
                                    continue;
                                cache.put(pos, loadChunk(Diff.dimPath(), new ChunkPos(pos)));
                            }
                    }
                }.start();
            }

            @Override
            public void render(final DrawContext r, final int mouseX, final int mouseY, final float delta) {
                for (var x = 0; x < w; x++)
                    for (var z = 0; z < h; z++) {
                        final var ax = this.x - w / 2 + x;
                        final var az = this.z - h / 2 + z;
                        final var cp = new ChunkPos(bp.set(ax, 0, az));
                        final var c = cache.get(cp.toLong());
                        if (c == null)
                            continue;
                        r.fill(x, z, x + 1, z + 1, c[ax - cp.getStartX()][az - cp.getStartZ()]);
                    }
            }

            @Override
            public boolean mouseDragged(
                    final double x, final double y,
                    final int button,
                    final double dx, final double dy) {
                this.x -= dx;
                this.z -= dy;
                return true;
            }

            @Override
            public void close() {
                super.close();
                close = true;
            }
        }

        public static DiffMap get() {
            return Systems.get(DiffMap.class);
        }

        private static int unsign(final byte b) {
            return b < 0 ? 0x100 + b : b;
        }

        // private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private static int[][] loadChunk(final Path dim, final ChunkPos p) {
            final var chunk = new int[s][s];
            try {
                final var bytes = Files.readAllBytes(dim
                        .resolve(Diff.posToString(p))
                        .resolve(paths.pos.latest)
                        .resolve(paths.pos.chunk.map));
                for (var x = 0; x < chunk.length; x++)
                    for (var z = 0; z < chunk[x].length; z++) {
                        final var i = (x * s + z) * SaveDiff.colorBytes;
                        chunk[x][z] = new Color(
                                unsign(bytes[i + 0]),
                                unsign(bytes[i + 1]),
                                unsign(bytes[i + 2]))
                                .getPacked();
                    }
            } catch (final Exception e) {
                e.printStackTrace();
            }
            return chunk;
        }

        private final Settings settings = new Settings();

        private final SettingGroup sgKeybind = settings.createGroup("Bind");

        @SuppressWarnings("unused")
        private final Setting<Keybind> keybind = sgKeybind.add(new KeybindSetting.Builder()
                .name("bind")
                .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_M))
                .action(this::open)
                .build());

        public DiffMap() {
            super("diff-map");
        }

        @Override
        public NbtCompound toTag() {
            final NbtCompound tag = new NbtCompound();

            tag.putInt("__version__", 1);

            tag.put("settings", settings.toTag());

            return tag;
        }

        @Override
        public DiffMap fromTag(final NbtCompound tag) {
            if (!tag.contains("__version__"))
                return this;

            settings.fromTag(tag.getCompound("settings"));

            return this;
        }

        private void open() {
            if (mc.currentScreen != null)
                return;
            if (mc.world == null)
                return;
            mc.setScreen(new MapScreen());
        }
    }

    private static class TabScreen extends WindowTabScreen {
        private final DiffMap map = DiffMap.get();

        public TabScreen(final GuiTheme theme, final Tab tab) {
            super(theme, tab);
            map.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.button("Open")).expandX().widget().action = map::open;
            add(theme.settings(map.settings)).expandX();
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard("map-settings", map.settings.toTag());
        }

        @Override
        public boolean fromClipboard() {
            final NbtCompound clipboard = NbtUtils.fromClipboard(map.settings.toTag());

            if (clipboard != null) {
                map.settings.fromTag(clipboard);
                return true;
            }

            return false;
        }
    }

    public MapTab() {
        super("Map");
        Systems.add(new DiffMap());
    }

    @Override
    public TabScreen createScreen(final GuiTheme theme) {
        return new TabScreen(theme, this);
    }

    @Override
    public boolean isScreen(final Screen screen) {
        return screen instanceof TabScreen;
    }
}
