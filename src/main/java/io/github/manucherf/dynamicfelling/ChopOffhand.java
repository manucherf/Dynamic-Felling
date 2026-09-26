package io.github.manucherf.dynamicfelling;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

public class ChopOffhand {
    //hide offhand 3rd person
    private static Player hiddenFrom;
    private static ItemStack hidden = ItemStack.EMPTY;

    static void onRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player instanceof AbstractClientPlayer client && ChopAnimation.isPlaying(client) && !player.getOffhandItem().isEmpty()) {
            hiddenFrom = player;
            hidden = player.getOffhandItem();
            // write the slot directly so no equip sound or event fires
            player.getInventory().offhand.set(0, ItemStack.EMPTY);
        }
    }

    static void onRenderPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() == hiddenFrom) {
            hiddenFrom.getInventory().offhand.set(0, hidden);
            hiddenFrom = null;
            hidden = ItemStack.EMPTY;
        }
    }
}