package com.github.vfyjxf.nee.network;

import static com.github.vfyjxf.nee.NotEnoughEnergistics.instance;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.github.vfyjxf.nee.client.gui.GuiPatternInterface;
import com.github.vfyjxf.nee.container.ContainerPatternInterface;

import cpw.mods.fml.common.network.IGuiHandler;

public class NEEGuiHandler implements IGuiHandler {

    public static final int PATTERN_INTERFACE_ID = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
        final int guiId = ordinal >> 4;

        if (guiId == PATTERN_INTERFACE_ID) {
            final TileEntity tile = world.getTileEntity(x, y, z);

            if (tile instanceof TilePatternInterface pattern) {
                return new ContainerPatternInterface(player.inventory, pattern);
            }

        }

        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
        final int guiId = ordinal >> 4;

        if (guiId == PATTERN_INTERFACE_ID) {
            final TileEntity tile = world.getTileEntity(x, y, z);

            if (tile instanceof TilePatternInterface pattern) {
                return new GuiPatternInterface(player.inventory, pattern);
            }

        }

        return null;
    }

    public static void openGui(EntityPlayer player, int ID, TileEntity tile, ForgeDirection side) {
        int x = tile.xCoord;
        int y = tile.yCoord;
        int z = tile.zCoord;
        player.openGui(instance, ID << 4 | (side.ordinal()), tile.getWorldObj(), x, y, z);
    }

    public static void openGui(EntityPlayer player, int ID) {
        int x = (int) player.posX;
        int y = (int) player.posY;
        int z = (int) player.posZ;
        player.openGui(instance, (ID << 4), player.worldObj, x, y, z);
    }

}
