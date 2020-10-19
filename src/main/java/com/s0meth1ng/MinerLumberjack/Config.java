package com.s0meth1ng.MinerLumberjack;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    private static final String CATEGORY_ORES = "ORES";
    private static final String ORES_TOOLS = "tools which can be used by mod for mining";
    private static final String ORES_BLOCKS = "blocks which can be grouped by mod for mining";

    private static final String CATEGORY_CHEATS = "CHEATS";
    private static final String CHEATS_COMMENT =
            "value \"false\" allows you to mine and chop any blocks with any tools.\n" +
            "For example you can mine redstone with a stone pickaxe.\n" +
            "If this value if \"true\", you can mine and chop only blocks, that could be harvested with your tool";

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static ForgeConfigSpec config;

    public static Set<Item> pickaxes;
    public static Set<Block> ores;
    public static boolean fairplay;

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> ores_tools;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> ores_blocks;
    private static ForgeConfigSpec.BooleanValue fair_play;

    private static final String[] defaultPickaxes =
            {
                Items.IRON_PICKAXE.getRegistryName().toString()
            };
    private static final String[] defaultOres =
            {
                Blocks.COAL_ORE.getRegistryName().toString()
            };

    static {
        BUILDER.comment("Ores settings").push(CATEGORY_ORES);
        ores_tools = BUILDER.comment(ORES_TOOLS).defineList("tools", Arrays.asList(defaultPickaxes), t -> t instanceof String);
        ores_blocks = BUILDER.comment(ORES_BLOCKS).defineList("ores", Arrays.asList(defaultOres), t -> t instanceof String);
        BUILDER.pop();

        BUILDER.comment("Cheats settings").push(CATEGORY_CHEATS);
        fair_play = BUILDER.comment(CHEATS_COMMENT).define("fairplay", true);
        BUILDER.pop();

        config = BUILDER.build();
    }

    public static void loadConfig(ForgeConfigSpec config, Path path) {
        final CommentedFileConfig configdata = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configdata.load();
        config.setConfig(configdata);
    }

    public static void setValues() {
        pickaxes = new HashSet<>();
        for (String name : ores_tools.get()) {
            pickaxes.add(ForgeRegistries.ITEMS.getValue(new ResourceLocation(name)));
        }
        ores = new HashSet<>();
        for (String name : ores_blocks.get()) {
            ores.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name)));
        }

        fairplay = fair_play.get();
    }
}
