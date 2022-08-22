package dev.galacticraft.dyndims.gametest;

import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;

@Mod("dyndims_test")
public final class DynamicDimensionsTest {
    public DynamicDimensionsTest() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerGametests);
    }

    public void registerGametests(@NotNull RegisterGameTestsEvent event) {
        event.register(DynamicDimensionsGametest.class);
    }
}
