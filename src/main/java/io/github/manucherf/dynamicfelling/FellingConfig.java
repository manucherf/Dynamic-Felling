package io.github.manucherf.dynamicfelling;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class FellingConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_TOOLS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        EXCLUDED_TOOLS = builder
                .comment(
                        "Tools that skip Dynamic Felling and chop the normal way (for example chainsaws).",
                        "Use item IDs like \"somemod:chainsaw\", or item tags with a # like \"#somemod:saws\".")
                .defineListAllowEmpty("excludedTools", List.of(), () -> "", FellingConfig::isValidEntry);
        SPEC = builder.build();
    }

    private FellingConfig() {}

    private static boolean isValidEntry(Object entry) {
        return entry instanceof String text
                && ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text) != null;
    }

    public static boolean isExcluded(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String entry : EXCLUDED_TOOLS.get()) {
            if (entry.startsWith("#")) {
                if (stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.substring(1))))) {
                    return true;
                }
            } else if (entry.equals(itemId)) {
                return true;
            }
        }
        return false;
    }
}