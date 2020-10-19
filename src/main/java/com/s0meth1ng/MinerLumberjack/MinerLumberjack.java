package com.s0meth1ng.MinerLumberjack;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("miner_lumberjack")
public class MinerLumberjack
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public MinerLumberjack() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onBreakingBlock(PlayerEvent.BreakSpeed breakSpeed) {

    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer().getHeldItemMainhand().getItem() == Items.IRON_PICKAXE) {
            tryMineOre(event);
        }
    }

    private void tryMineOre(BlockEvent.BreakEvent event) {
        Block mainBlock = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (mainBlock == Blocks.COAL_ORE) {
            destroyCollidedBlocks((World) event.getWorld(), event.getPlayer(), event.getPos());
        }
    }

    private void destroyCollidedBlocks(World world, PlayerEntity player, BlockPos mainPos) {
        ArrayList<BlockPos> collidedBlocks = getCollidedBlocks(world, mainPos);
        for (BlockPos pos : collidedBlocks) {
            Block currentBlock = world.getBlockState(pos).getBlock();
            currentBlock.harvestBlock(world, player, pos, world.getBlockState(pos), null, player.getHeldItemMainhand());
            currentBlock.dropXpOnBlockBreak(world, pos, currentBlock.getExpDrop(world.getBlockState(pos), world, pos, 0, 0));
            world.removeBlock(pos, true);
        }
        ItemStack tool = player.getHeldItemMainhand();
        Item toolItem = tool.getItem();
        if (!EnchantmentHelper.getEnchantments(tool).containsKey(Enchantments.MENDING)) {
            int damage = collidedBlocks.size();
            if (EnchantmentHelper.getEnchantments(tool).containsKey(Enchantments.UNBREAKING)) {
                damage /= EnchantmentHelper.getEnchantments(tool).get(Enchantments.UNBREAKING) + 1;
                if (damage == 0) damage = 1;
            }
            toolItem.setDamage(tool, tool.getDamage() + damage);
        }
    }

    private ArrayList<BlockPos> getCollidedBlocks(World world, BlockPos mainPos) {
        ArrayList<BlockPos> collidedBlocksPos = new ArrayList<>();
        collidedBlocksPos.add(mainPos);
        checkNearbyBlocks(world, collidedBlocksPos, mainPos);
        return collidedBlocksPos;
    }

    private void checkNearbyBlocks(World world, ArrayList<BlockPos> collidedBlocksPos, BlockPos mainPos) {
        ArrayList<BlockPos> temp = new ArrayList<>();
        for (int z = -1; z <= 1; z++) {
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    if (!((z == 0) && (y == 0) && (x == 0))) {
                        BlockPos newPos = mainPos.add(x, y, z);
                        if (world.getBlockState(mainPos).getBlock() == world.getBlockState(newPos).getBlock()) {
                            if (!collidedBlocksPos.contains(newPos)) {
                                collidedBlocksPos.add(newPos);
                                temp.add(newPos);
                            }
                        }
                    }
                }
            }
        }
        for (BlockPos pos : temp) {
            checkNearbyBlocks(world, collidedBlocksPos, pos);
        }
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }
}
