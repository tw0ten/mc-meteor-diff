package twoten.meteor.diff.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import twoten.meteor.diff.Addon;

@Mixin(MinecraftClient.class)
public abstract class ExampleMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onGameLoaded(RunArgs args, CallbackInfo ci) {
        Addon.LOG.info("Hello from ExampleMixin!");
    }
}
