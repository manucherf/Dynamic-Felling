package io.github.manucherf.dynamicfelling;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FellingItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DynamicFelling.MODID);

    public static final DeferredItem<WhetstoneItem> WHETSTONE = ITEMS.register("whetstone",
            () -> new WhetstoneItem(new Item.Properties().durability(64)));

    private FellingItems() {}

    static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WHETSTONE);
        }
    }
}