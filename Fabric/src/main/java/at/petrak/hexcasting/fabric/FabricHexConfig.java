package at.petrak.hexcasting.fabric;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import io.github.fablabsmc.fablabs.api.fiber.v1.builder.ConfigTreeBuilder;
import io.github.fablabsmc.fablabs.api.fiber.v1.exception.ValueDeserializationException;
import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.derived.ConfigTypes;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.FiberSerialization;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.JanksonValueSerializer;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigTree;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.PropertyMirror;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.*;
import java.util.List;

// https://github.com/VazkiiMods/Botania/blob/1.18.x/Fabric/src/main/java/vazkii/botania/fabric/FiberBotaniaConfig.java
public class FabricHexConfig {
    private static final Common COMMON = new Common();
    private static final Client CLIENT = new Client();
    private static final Server SERVER = new Server();

    private static void writeDefaultConfig(ConfigTree config, Path path, JanksonValueSerializer serializer) {
        try (OutputStream s = new BufferedOutputStream(
            Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW))) {
            FiberSerialization.serialize(config, s, serializer);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            HexAPI.LOGGER.error("Error writing default config", e);
        }
    }

    private static void setupConfig(ConfigTree config, Path p, JanksonValueSerializer serializer) {
        writeDefaultConfig(config, p, serializer);

        try (InputStream s = new BufferedInputStream(
            Files.newInputStream(p, StandardOpenOption.READ, StandardOpenOption.CREATE))) {
            FiberSerialization.deserialize(config, s, serializer);
        } catch (IOException | ValueDeserializationException e) {
            HexAPI.LOGGER.error("Error loading config from {}", p, e);
        }
    }

    public static void setup() {
        try {
            Files.createDirectory(Paths.get("config"));
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            HexAPI.LOGGER.warn("Failed to make config dir", e);
        }

        var serializer = new JanksonValueSerializer(false);
        var common = COMMON.configure(ConfigTree.builder());
        setupConfig(common, Paths.get("config", HexAPI.MOD_ID + "-common.json5"), serializer);
        HexConfig.setCommon(COMMON);

        if (IXplatAbstractions.INSTANCE.isPhysicalClient()) {
            var client = CLIENT.configure(ConfigTree.builder());
            setupConfig(client, Paths.get("config", HexAPI.MOD_ID + "-client.json5"), serializer);
            HexConfig.setClient(CLIENT);
        } else {
            var server = SERVER.configure(ConfigTree.builder());
            setupConfig(server, Paths.get("config", HexAPI.MOD_ID + "-client.json5"), serializer);
            HexConfig.setServer(SERVER);
        }
    }

    private static final class Common implements HexConfig.CommonConfigAccess {
        private final PropertyMirror<Integer> dustManaAmount = PropertyMirror.create(ConfigTypes.NATURAL);
        private final PropertyMirror<Integer> shardManaAmount = PropertyMirror.create(ConfigTypes.NATURAL);
        private final PropertyMirror<Integer> chargedCrystalManaAmount = PropertyMirror.create(ConfigTypes.NATURAL);
        private final PropertyMirror<Double> manaToHealthRate = PropertyMirror.create(
            ConfigTypes.DOUBLE.withMinimum(0d));

        public ConfigTree configure(ConfigTreeBuilder bob) {
            bob.fork("Mana Amounts")
                .beginValue("dustManaAmount", ConfigTypes.NATURAL, DEFAULT_DUST_MANA_AMOUNT)
                .withComment("How much mana a single Amethyst Dust item is worth")
                .finishValue(dustManaAmount::mirror)

                .beginValue("shardManaAmount", ConfigTypes.NATURAL, DEFAULT_SHARD_MANA_AMOUNT)
                .withComment("How much mana a single Amethyst Shard item is worth")
                .finishValue(shardManaAmount::mirror)

                .beginValue("chargedCrystalManaAmount", ConfigTypes.NATURAL, DEFAULT_CHARGED_MANA_AMOUNT)
                .withComment("How much mana a single Charged Amethyst Crystal item is worth")
                .finishValue(chargedCrystalManaAmount::mirror)

                .beginValue("manaToHealthRate", ConfigTypes.DOUBLE, DEFAULT_MANA_TO_HEALTH_RATE)
                .withComment("How many points of mana a half-heart is worth when casting from HP")
                .finishValue(manaToHealthRate::mirror)
                .finishBranch();

            return bob.build();
        }

        @Override
        public int dustManaAmount() {
            return dustManaAmount.getValue();
        }

        @Override
        public int shardManaAmount() {
            return shardManaAmount.getValue();
        }

        @Override
        public int chargedCrystalManaAmount() {
            return chargedCrystalManaAmount.getValue();
        }

        @Override
        public double manaToHealthRate() {
            return manaToHealthRate.getValue();
        }
    }

    private static final class Client implements HexConfig.ClientConfigAccess {
        private final PropertyMirror<Double> patternPointSpeedMultiplier = PropertyMirror.create(
            ConfigTypes.DOUBLE.withMinimum(0d));
        private final PropertyMirror<Boolean> ctrlTogglesOffStrokeOrder = PropertyMirror.create(ConfigTypes.BOOLEAN);

        public ConfigTree configure(ConfigTreeBuilder bob) {
            bob
                .beginValue("patternPointSpeedMultiplier", ConfigTypes.DOUBLE, DEFAULT_PATTERN_POINT_SPEED_MULTIPLIER)
                .withComment("How fast the point showing you the stroke order on patterns moves")
                .finishValue(patternPointSpeedMultiplier::mirror)

                .beginValue("ctrlTogglesOffStrokeOrder", ConfigTypes.BOOLEAN, DEFAULT_CTRL_TOGGLES_OFF_STROKE_ORDER)
                .withComment("Whether the ctrl key will instead turn *off* the color gradient on patterns")
                .finishValue(ctrlTogglesOffStrokeOrder::mirror);

            return bob.build();
        }

        @Override
        public double patternPointSpeedMultiplier() {
            return patternPointSpeedMultiplier.getValue();
        }

        @Override
        public boolean ctrlTogglesOffStrokeOrder() {
            return ctrlTogglesOffStrokeOrder.getValue();
        }
    }

    private static final class Server implements HexConfig.ServerConfigAccess {
        private final PropertyMirror<Integer> opBreakHarvestLevel = PropertyMirror.create(
            ConfigTypes.INTEGER.withValidRange(0, 4, 1));
        private final PropertyMirror<Integer> maxRecurseDepth = PropertyMirror.create(ConfigTypes.NATURAL);
        private final PropertyMirror<Integer> maxSpellCircleLength = PropertyMirror.create(
            ConfigTypes.INTEGER.withMinimum(4));
        private final PropertyMirror<List<String>> actionDenyList = PropertyMirror.create(
            ConfigTypes.makeList(ConfigTypes.STRING));

        public ConfigTree configure(ConfigTreeBuilder bob) {
            bob.fork("Spells")
                .beginValue("maxRecurseDepth", ConfigTypes.NATURAL, DEFAULT_MAX_RECURSE_DEPTH)
                .withComment("How many times a spell can recursively cast other spells")
                .finishValue(maxRecurseDepth::mirror)

                .beginValue("opBreakHarvestLevel", ConfigTypes.NATURAL, DEFAULT_OP_BREAK_HARVEST_LEVEL)
                .withComment("The harvest level of the Break Block spell.\n" +
                    "0 = wood, 1 = stone, 2 = iron, 3 = diamond, 4 = netherite.")
                .finishValue(opBreakHarvestLevel::mirror)
                .finishBranch()

                .fork("Spell Circles")
                .beginValue("maxSpellCircleLength", ConfigTypes.NATURAL, DEFAULT_MAX_SPELL_CIRCLE_LENGTH)
                .withComment("The maximum number of slates in a spell circle")
                .finishValue(maxSpellCircleLength::mirror)
                .finishBranch()

                .beginValue("actionDenyList", ConfigTypes.makeList(ConfigTypes.STRING), List.of())
                .withComment("The maximum number of slates in a spell circle")
                .finishValue(actionDenyList::mirror);

            return bob.build();
        }

        @Override
        public int opBreakHarvestLevelBecauseForgeThoughtItWasAGoodIdeaToImplementHarvestTiersUsingAnHonestToGodTopoSort() {
            return opBreakHarvestLevel.getValue();
        }

        @Override
        public int maxRecurseDepth() {
            return maxRecurseDepth.getValue();
        }

        @Override
        public int maxSpellCircleLength() {
            return maxSpellCircleLength.getValue();
        }

        @Override
        public boolean isActionAllowed(ResourceLocation actionID) {
            return actionDenyList.getValue().contains(actionID.toString());
        }
    }
}
