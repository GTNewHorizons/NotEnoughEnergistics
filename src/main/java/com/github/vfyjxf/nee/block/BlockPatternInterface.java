package com.github.vfyjxf.nee.block;

import java.util.EnumSet;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.block.AEBaseItemBlock;
import appeng.block.AEBaseTileBlock;
import appeng.core.features.AEFeature;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.github.vfyjxf.nee.network.NEEGuiHandler;

/**
 * @author vfyjxf
 */
public class BlockPatternInterface extends AEBaseTileBlock {

    public static BlockPatternInterface BLOCK_INSTANCE = new BlockPatternInterface();
    public static Item ITEM_INSTANCE = new AEBaseItemBlock(BLOCK_INSTANCE);

    public BlockPatternInterface() {
        super(Material.iron);
        this.setTileEntity(TilePatternInterface.class);
        this.setFeature(EnumSet.of(AEFeature.Core));
        this.setBlockName(NotEnoughEnergistics.MODID + "." + "pattern_interface");
        this.setBlockTextureName(NotEnoughEnergistics.MODID + ":" + "pattern_interface");
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY,
            float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        TilePatternInterface tile = this.getTileEntity(world, x, y, z);
        if (tile != null) {
            if (!world.isRemote) {
                NEEGuiHandler.openGui(player, NEEGuiHandler.PATTERN_INTERFACE_ID, tile, ForgeDirection.UNKNOWN);
            }
            return true;
        }

        return false;
    }
}
