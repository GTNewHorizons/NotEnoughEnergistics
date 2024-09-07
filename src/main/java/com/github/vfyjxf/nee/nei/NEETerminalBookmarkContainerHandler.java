package com.github.vfyjxf.nee.nei;

import java.util.ArrayList;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIBookmark;

import codechicken.nei.api.IBookmarkContainerHandler;

public class NEETerminalBookmarkContainerHandler implements IBookmarkContainerHandler {

    public static final NEETerminalBookmarkContainerHandler instance = new NEETerminalBookmarkContainerHandler();

    private NEETerminalBookmarkContainerHandler() {}

    @Override
    public void pullBookmarkItemsFromContainer(GuiContainer guiContainer, ArrayList<ItemStack> bookmarkItems) {
        NBTTagCompound nbtBookmarkItems = new NBTTagCompound();

        for (int i = 0; i < bookmarkItems.size(); i++) {
            nbtBookmarkItems.setTag("#" + i, packBookmarkItem(bookmarkItems.get(i)));
        }

        NEENetworkHandler.getInstance().sendToServer(new PacketNEIBookmark(nbtBookmarkItems));
    }

    private NBTTagCompound packBookmarkItem(ItemStack bookmarkItem) {
        return bookmarkItem.writeToNBT(new NBTTagCompound());
    }

}
