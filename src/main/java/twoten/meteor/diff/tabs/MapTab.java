package twoten.meteor.diff.tabs;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static twoten.meteor.diff.Diff.s;

import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import twoten.meteor.diff.Diff;
import twoten.meteor.diff.Diff.paths;
import twoten.meteor.diff.modules.SaveDiff;

public class MapTab extends Tab {
    private static class MapScreen extends Screen {
        private int[][] loadChunk(final ChunkPos p) {
            final var chunk = new int[s][s];
            try {
                final var bytes = Files.readAllBytes(this.p
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

        private static int unsign(final byte b) {
            return b < 0 ? 0x100 + b : b;
        }

        private final Settings settings = new Settings();

        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final SettingGroup sgKeybind = settings.createGroup("Bind");

        @SuppressWarnings("unused")
        private final Setting<Keybind> keybind = sgKeybind.add(new KeybindSetting.Builder()
                .name("bind")
                .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_M))
                .action(this::open)
                .build());

        private Path p;
        private int[][][][] chunks;
        private int x, z;
        private int scale = 2;

        protected MapScreen() {
            super(Text.of("Map"));
        }

        @Override
        public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
            super.render(context, mouseX, mouseY, delta);
            HudRenderer.INSTANCE.drawContext = context;
            Diff.renderChunk(context, 0, 0, scale, chunks[0][0]);
        }

        private void open() {
            if (mc.world == null)
                return;
            p = Diff.dimPath();
            final var pos = mc.player.getBlockPos();
            this.x = pos.getX();
            this.z = pos.getZ();
            mc.setScreen(this);
            update();
        }

        private void update() {
            final var w = mc.getWindow().getWidth();
            final var h = mc.getWindow().getHeight();
            chunks = new int[1][1][][];
            chunks[0][0] = loadChunk(new ChunkPos(0, 0));
        }
    }

    private static class TabScreen extends WindowTabScreen {
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

    private static final MapScreen map = new MapScreen();

    @PostInit
    public static void postInit() {
        new MapTab().createScreen(GuiThemes.get());
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
