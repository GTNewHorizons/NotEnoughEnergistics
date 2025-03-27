package com.github.vfyjxf.nee.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIBookmark;
import com.github.vfyjxf.nee.utils.GuiUtils;

import codechicken.nei.api.IBookmarkContainerHandler;

public class NEETerminalBookmarkContainerHandler implements IBookmarkContainerHandler {

    public static final NEETerminalBookmarkContainerHandler instance = new NEETerminalBookmarkContainerHandler();

    private NEETerminalBookmarkContainerHandler() {}

    @Override
    public List<ItemStack> getStorageStacks(GuiContainer guiContainer) {
        return GuiUtils.getStorageStacks(guiContainer, stack -> stack.getStackSize() > 0).stream()
                .map(stack -> stack.getItemStack().copy()).collect(Collectors.toList());
    }

    @Override
    public void pullBookmarkItemsFromContainer(GuiContainer guiContainer, ArrayList<ItemStack> bookmarkItems) {
        final NBTTagCompound nbtBookmarkItems = new NBTTagCompound();

        for (int i = 0; i < bookmarkItems.size(); i++) {
            nbtBookmarkItems.setTag("#" + i, packBookmarkItem(bookmarkItems.get(i)));
        }

        NEENetworkHandler.getInstance().sendToServer(new PacketNEIBookmark(nbtBookmarkItems));
    }

    private NBTTagCompound packBookmarkItem(ItemStack bookmarkItem) {
        return bookmarkItem.writeToNBT(new NBTTagCompound());
    }
}
