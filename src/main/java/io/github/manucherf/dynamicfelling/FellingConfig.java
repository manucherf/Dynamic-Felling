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
    public static final ModConfigSpec.DoubleValue WOODEN_AXE_SWINGS;
    public static final ModConfigSpec.DoubleValue SWINGS_SAVED_PER_TIER;
    public static final ModConfigSpec.DoubleValue SWINGS_SAVED_PER_EFFICIENCY;
    public static final ModConfigSpec.IntValue MAX_SWINGS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        EXCLUDED_TOOLS = builder
                .comment(
                        "Tools that skip Dynamic Felling and chop the normal way (for example chainsaws).",
                        "Use item IDs like \"somemod:chainsaw\", or item tags with a # like \"#somemod:saws\".")
                .defineListAllowEmpty("excludedTools", List.of(), () -> "", FellingConfig::isValidEntry);
        WOODEN_AXE_SWINGS = builder
                .comment("Swings a wooden axe needs to fell a trunk one block wide.")
                .defineInRange("woodenAxeSwings", 10.0, 1.0, 100.0);
        SWINGS_SAVED_PER_TIER = builder
                .comment("Swings saved for each axe tier above wood (stone 1, iron 2, diamond 3, netherite 4).")
                .defineInRange("swingsSavedPerTier", 1.0, 0.0, 20.0);
        SWINGS_SAVED_PER_EFFICIENCY = builder
                .comment("Swings saved for each level of Efficiency.")
                .defineInRange("swingsSavedPerEfficiency", 0.5, 0.0, 10.0);
        MAX_SWINGS = builder
                .comment("Most swings any trunk can take, however wide. 0 means no limit.")
                .defineInRange("maxSwings", 0, 0, 200);
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