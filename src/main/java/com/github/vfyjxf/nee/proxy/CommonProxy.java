package com.github.vfyjxf.nee.proxy;

import static com.github.vfyjxf.nee.block.BlockPatternInterface.BLOCK_INSTANCE;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IMaterials;
import appeng.api.definitions.IParts;
import appeng.api.util.AEColor;
import appeng.core.features.ActivityState;
import appeng.core.features.BlockStackSrc;
import appeng.tile.AEBaseTile;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.block.BlockPatternInterface;
import com.github.vfyjxf.nee.block.tile.TilePatternInterface;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void init(FMLInitializationEvent event) {
        AEBaseTile.registerTileItem(
                TilePatternInterface.class,
                new BlockStackSrc(BLOCK_INSTANCE, 0, ActivityState.Enabled));
        registerRecipe();
    }

    public void preInit(FMLPreInitializationEvent event) {
        registerBlocks();
        registerItems();
        registerTileEntities();
    }

    public void registerItems() {
        GameRegistry.registerItem(BlockPatternInterface.ITEM_INSTANCE, "tile.pattern_interface");
    }

    public void registerBlocks() {
        GameRegistry.registerBlock(BLOCK_INSTANCE, null, "tile.pattern_interface");
    }

    public void registerTileEntities() {
        GameRegistry.registerTileEntity(
                TilePatternInterface.class,
                NotEnoughEnergistics.MODID + "." + "tile.pattern_interface");
    }

    public void registerRecipe() {
        final IDefinitions definitions = AEApi.instance().definitions();
        final IMaterials materials = definitions.materials();
        final IParts parts = definitions.parts();
        final IBlocks blocks = definitions.blocks();

        ItemStack blankPattern = null;
        ItemStack patternTerm = null;
        ItemStack meInterface = null;
        ItemStack monitor = null;
        ItemStack cableSmart = parts.cableSmart().stack(AEColor.Transparent, 1);
        if (materials.blankPattern().maybeStack(1).isPresent()) {
            blankPattern = materials.blankPattern().maybeStack(1).get();
        }
        if (parts.patternTerminal().maybeStack(1).isPresent()) {
            patternTerm = parts.patternTerminal().maybeStack(1).get();
        }
        if (blocks.iface().maybeStack(1).isPresent()) {
            meInterface = blocks.iface().maybeStack(1).get();
        }
        if (blocks.craftingMonitor().maybeStack(1).isPresent()) {
            monitor = blocks.craftingMonitor().maybeStack(1).get();
        }

        GameRegistry.addRecipe(
                new ShapedOreRecipe(
                        BLOCK_INSTANCE,
                        "aba",
                        "cdc",
                        "eee",
                        'a',
                        blankPattern,
                        'b',
                        patternTerm,
                        'c',
                        meInterface,
                        'd',
                        monitor,
                        'e',
                        cableSmart));
    }
}
