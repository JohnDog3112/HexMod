package at.petrak.hexcasting.forge.datagen;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.datagen.HexAdvancements;
import at.petrak.hexcasting.datagen.HexLootTables;
import at.petrak.hexcasting.datagen.IXplatIngredients;
import at.petrak.hexcasting.datagen.recipe.HexplatRecipes;
import at.petrak.hexcasting.datagen.recipe.builders.FarmersDelightToolIngredient;
import at.petrak.hexcasting.datagen.tag.HexActionTagProvider;
import at.petrak.hexcasting.datagen.tag.HexBlockTagProvider;
import at.petrak.hexcasting.datagen.tag.HexItemTagProvider;
import at.petrak.hexcasting.forge.datagen.xplat.HexBlockStatesAndModels;
import at.petrak.hexcasting.forge.datagen.xplat.HexItemModels;
import at.petrak.hexcasting.forge.recipe.ForgeModConditionalIngredient;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.google.gson.JsonObject;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumMap;
import java.util.stream.Stream;

public class ForgeHexDataGenerators {
    @SubscribeEvent
    public static void generateData(GatherDataEvent ev) {
        if (System.getProperty("hexcasting.xplat_datagen") != null) {
            configureXplatDatagen(ev);
        }
        if (System.getProperty("hexcasting.forge_datagen") != null) {
            configureForgeDatagen(ev);
        }
    }

    private static void configureXplatDatagen(GatherDataEvent ev) {
        HexAPI.LOGGER.info("Starting cross-platform datagen");

        DataGenerator gen = ev.getGenerator();
        var output = gen.getPackOutput();
        ExistingFileHelper efh = ev.getExistingFileHelper();
        gen.addProvider(ev.includeClient(), new HexItemModels(output, efh));
        gen.addProvider(ev.includeClient(), new HexBlockStatesAndModels(output, efh));
        gen.addProvider(ev.includeServer(), new HexAdvancements());
    }

    private static void configureForgeDatagen(GatherDataEvent ev) {
        HexAPI.LOGGER.info("Starting Forge-specific datagen");

        DataGenerator gen = ev.getGenerator();
        var pack = gen.getPackOutput();
        var lookup = ev.getLookupProvider();
        ExistingFileHelper efh = ev.getExistingFileHelper();
        gen.addProvider(ev.includeServer(), new HexLootTables());
        gen.addProvider(ev.includeServer(), new HexplatRecipes(pack, INGREDIENTS, ForgeHexConditionsBuilder::new));

        var xtags = IXplatAbstractions.INSTANCE.tags();
        var blockTagProvider = new HexBlockTagProvider(pack, lookup, xtags);
        gen.addProvider(ev.includeServer(), blockTagProvider);
        var itemTagProvider = new HexItemTagProvider(pack, lookup, blockTagProvider, IXplatAbstractions.INSTANCE.tags());
        gen.addProvider(ev.includeServer(), itemTagProvider);
        gen.addProvider(ev.includeServer(), new HexActionTagProvider(pack, lookup));

        gen.addProvider(ev.includeServer(), new ForgeHexLootModGen(gen));
    }

    private static final IXplatIngredients INGREDIENTS = new IXplatIngredients() {
        @Override
        public Ingredient glowstoneDust() {
            return Ingredient.of(Tags.Items.DUSTS_GLOWSTONE);
        }

        @Override
        public Ingredient leather() {
            return Ingredient.of(Tags.Items.LEATHER);
        }

        @Override
        public Ingredient ironNugget() {
            return Ingredient.of(Tags.Items.NUGGETS_IRON);
        }

        @Override
        public Ingredient goldNugget() {
            return Ingredient.of(Tags.Items.NUGGETS_GOLD);
        }

        @Override
        public Ingredient copperIngot() {
            return Ingredient.of(Tags.Items.INGOTS_COPPER);
        }

        @Override
        public Ingredient ironIngot() {
            return Ingredient.of(Tags.Items.INGOTS_IRON);
        }

        @Override
        public Ingredient goldIngot() {
            return Ingredient.of(Tags.Items.INGOTS_GOLD);
        }

        @Override
        public EnumMap<DyeColor, Ingredient> dyes() {
            var out = new EnumMap<DyeColor, Ingredient>(DyeColor.class);
            for (var col : DyeColor.values()) {
                out.put(col, Ingredient.of(col.getTag()));
            }
            return out;
        }

        @Override
        public Ingredient stick() {
            return Ingredient.fromValues(Stream.of(
                new Ingredient.ItemValue(new ItemStack(Items.STICK)),
                new Ingredient.TagValue(ItemTags.create(new ResourceLocation("forge", "rods/wooden")))
            ));
        }

        @Override
        public Ingredient whenModIngredient(Ingredient defaultIngredient, String modid, Ingredient modIngredient) {
            return ForgeModConditionalIngredient.of(defaultIngredient, modid, modIngredient);
        }

        // https://github.com/vectorwing/FarmersDelight/blob/1.18.2/src/generated/resources/data/farmersdelight/recipes/cutting/amethyst_block.json
        @Override
        public FarmersDelightToolIngredient axeStrip() {
            return () -> {
                JsonObject object = new JsonObject();
                object.addProperty("type", "farmersdelight:tool_action");
                object.addProperty("action", ToolActions.AXE_STRIP.name());
                return object;
            };
        }

        @Override
        public FarmersDelightToolIngredient axeDig() {
            return () -> {
                JsonObject object = new JsonObject();
                object.addProperty("type", "farmersdelight:tool_action");
                object.addProperty("action", ToolActions.AXE_DIG.name());
                return object;
            };
        }
    };
}
