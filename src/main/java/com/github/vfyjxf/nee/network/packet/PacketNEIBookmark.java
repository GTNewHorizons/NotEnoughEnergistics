package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;

import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper.UnableToFindFieldException;
import io.netty.buffer.ByteBuf;
import thaumicenergistics.common.container.ContainerPartArcaneCraftingTerminal;

public class PacketNEIBookmark implements IMessage {

    private NBTTagCompound bookmarkItems;

    public PacketNEIBookmark() {}

    public PacketNEIBookmark(NBTTagCompound bookmarkItems) {
        this.bookmarkItems = bookmarkItems;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.bookmarkItems = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.bookmarkItems);
    }

    public static final class Handler implements IMessageHandler<PacketNEIBookmark, IMessage> {

        @Override
        public IMessage onMessage(PacketNEIBookmark message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final Container container = player.openContainer;

            if (message.bookmarkItems == null) {
                return null;
            }

            if (container instanceof ContainerMEMonitorable monitorable) {
                final IMEMonitor<IAEItemStack> monitor = monitorable.getMonitor();
                if (monitor != null) {
                    final IEnergySource energy = monitorable.getPowerSource();
                    final BaseActionSource actionSource = monitorable.getActionSource();

                    for (Object key : message.bookmarkItems.func_150296_c()) {
                        final ItemStack bookmarkItem = ItemUtils
                                .loadItemStackFromNBT(message.bookmarkItems.getCompoundTag((String) key));
                        bookmarkItem.stackSize = getFreeStackSize(player, bookmarkItem);

                        if (bookmarkItem.stackSize > 0) {
                            final AEItemStack requestStack = AEItemStack.create(bookmarkItem);
                            final IAEItemStack extractedStack = Platform
                                    .poweredExtraction(energy, monitor, requestStack, actionSource);

                            if (extractedStack != null) {
                                InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN)
                                        .addItems(extractedStack.getItemStack());
                            }
                        }
                    }
                }

            } else if (Loader.isModLoaded(ModIDs.ThE) && container instanceof ContainerPartArcaneCraftingTerminal act) {
                final IMEMonitor<IAEItemStack> monitor = getMonitor(container);
                final IGrid grid = act.getHostGrid();
                if (grid == null || monitor == null) return null;
                final IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
                final BaseActionSource actionSource = new PlayerSource(player, act.terminal);

                for (Object key : message.bookmarkItems.func_150296_c()) {
                    final ItemStack bookmarkItem = ItemUtils
                            .loadItemStackFromNBT(message.bookmarkItems.getCompoundTag((String) key));
                    bookmarkItem.stackSize = getFreeStackSize(player, bookmarkItem);

                    if (bookmarkItem.stackSize > 0) {
                        final AEItemStack requestStack = AEItemStack.create(bookmarkItem);
                        final IAEItemStack extractedStack = Platform
                                .poweredExtraction(energy, monitor, requestStack, actionSource);

                        if (extractedStack != null) {
                            InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN)
                                    .addItems(extractedStack.getItemStack());
                        }
                    }
                }

            }

            return null;
        }

        private int getFreeStackSize(EntityPlayer player, ItemStack itemStack) {
            int freeStackSize = 0;

            for (ItemStack slotStack : player.inventory.mainInventory) {
                if (slotStack == null) { // Empty slot, stack fits completely
                    freeStackSize += itemStack.getMaxStackSize();
                } else if (slotStack.isItemEqual(itemStack)) {
                    freeStackSize += itemStack.getMaxStackSize() - slotStack.stackSize;
                }
            }

            return Math.min(itemStack.stackSize, freeStackSize);
        }

        private IMEMonitor<IAEItemStack> getMonitor(Container container) {

            try {
                return (IMEMonitor<IAEItemStack>) ReflectionHelper.findField(container.getClass(), "monitor")
                        .get(container);
            } catch (UnableToFindFieldException | IllegalAccessException e) {}

            return null;
        }
    }
}
