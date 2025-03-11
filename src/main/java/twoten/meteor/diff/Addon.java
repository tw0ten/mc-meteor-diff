package twoten.meteor.diff;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import twoten.meteor.diff.commands.DiffCommand;
import twoten.meteor.diff.hud.DiffHud;
import twoten.meteor.diff.hud.RadarHud;
import twoten.meteor.diff.modules.SaveDiff;
import twoten.meteor.diff.modules.VisualDiff;
import twoten.meteor.diff.tabs.MapTab;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Diff");
    public static final HudGroup HUD_GROUP = new HudGroup("Diff");

    @Override
    public void onInitialize() {
        LOG.info("Initializing " + getPackage());

        // Modules
        Modules.get().add(new VisualDiff());
        Modules.get().add(new SaveDiff());

        // Commands
        Commands.add(new DiffCommand());

        // HUD
        Hud.get().register(DiffHud.INFO);
        Hud.get().register(RadarHud.INFO);

        // Tabs
        Tabs.add(new MapTab());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "twoten.meteor.diff";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("tw0ten", "mc-meteor-diff");
    }
}
