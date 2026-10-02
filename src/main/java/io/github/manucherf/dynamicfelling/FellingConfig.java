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
    public static final ModConfigSpec.DoubleValue TIER_REDUCTION;
    public static final ModConfigSpec.DoubleValue EFFICIENCY_REDUCTION;
    public static final ModConfigSpec.IntValue MAX_SWINGS;
    public static final ModConfigSpec.DoubleValue BACKFALL_CHANCE;
    public static final ModConfigSpec.DoubleValue BEE_ANGER_CHANCE;
    public static final ModConfigSpec.DoubleValue CHOP_REACH;
    public static final ModConfigSpec.DoubleValue FALLEN_SWINGS;

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
        TIER_REDUCTION = builder
                .comment("Fraction of swings removed by each axe tier above wood (stone 1, iron 2, diamond 3, netherite 4). 0.15 means each tier needs 15% fewer swings than the one below.")
                .defineInRange("swingReductionPerTier", 0.15, 0.0, 0.9);
        EFFICIENCY_REDUCTION = builder
                .comment("Fraction of swings removed by each level of Efficiency. 0.1 means 10% fewer swings per level.")
                .defineInRange("swingReductionPerEfficiency", 0.1, 0.0, 0.9);
        MAX_SWINGS = builder
                .comment("Most swings any trunk can take, however wide. 0 means no limit.")
                .defineInRange("maxSwings", 0, 0, 200);
        BACKFALL_CHANCE = builder
                .comment("Chance (0 to 1) that a felled tree falls back toward the player instead of away")
                .defineInRange("backfallChance", 0.05, 0.0, 1.0);
        BEE_ANGER_CHANCE = builder
                .comment("Chance (0 to 1) per hit that each bee nest in the tree releases angry bees (a campfire under the nest keeps them calm)")
                .defineInRange("beeAngerChance", 0.25, 0.0, 1.0);
        CHOP_REACH = builder
                .comment("How far away (in blocks) a trunk can be chopped from; vanilla block reach is 4.5")
                .defineInRange("chopReach", 3.0, 1.0, 6.0);
        FALLEN_SWINGS = builder
                .comment("Swing multiplier for trunks and branches lying on the ground as physics objects (needs Sable,Tree Physics, and Dynamic Trees Physics). 0.5 means half the swings of a standing tree.")
                .defineInRange("fallenSwings", 0.5, 0.1, 1.0);
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