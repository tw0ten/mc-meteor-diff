package twoten.meteor.diff.tabs;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static twoten.meteor.diff.Diff.s;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import twoten.meteor.diff.Diff;
import twoten.meteor.diff.Diff.paths;
import twoten.meteor.diff.modules.SaveDiff;

public class MapTab extends Tab {
    private static class Map extends System<Map> {
        public static Map get() {
            if (true)
                return new Map();
            return Systems.get(Map.class); // how do i add a system?????????/
        }

        private static int unsign(final byte b) {
            return b < 0 ? 0x100 + b : b;
        }

        private static int[][] loadChunk(final Path dim, final ChunkPos p) {
            final var chunk = new int[s][s];
            try {
                final var bytes = Files.readAllBytes(dim
                        .resolve(p.x + " " + p.z)
                        .resolve(paths.latest)
                        .resolve(paths.chunk.map));
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

        // private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final Settings settings = new Settings();

        private final SettingGroup sgKeybind = settings.createGroup("Bind");

        @SuppressWarnings("unused")
        private final Setting<Keybind> keybind = sgKeybind.add(new KeybindSetting.Builder()
                .name("bind")
                .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_M))
                .action(this::open)
                .build());

        private int x, z;
        private final float scale = 2;
        private Thread chunkLoader;

        public Map() {
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
        public Map fromTag(final NbtCompound tag) {
            if (!tag.contains("__version__")) {
                return this;
            }

            settings.fromTag(tag.getCompound("settings"));

            return this;
        }

        private void open() {
            if (mc.world == null)
                return;
            {
                final var pos = mc.player.getBlockPos();
                this.x = pos.getX();
                this.z = pos.getZ();
            }
            this.chunkLoader = new Thread(getName() + " chunkLoader") {
                private final List<ChunkPos> chunks = new ArrayList<>();
                private final Path p = Diff.dimPath();

                @Override
                public void run() {
                    for (final var c : chunks)
                        loadChunk(p, c);
                    chunks.clear();
                }
            };
            mc.setScreen(new Screen(Text.of(getName())) {
            });
        }
    }

    private static class TabScreen extends WindowTabScreen {
        private final Map map = Map.get();

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
