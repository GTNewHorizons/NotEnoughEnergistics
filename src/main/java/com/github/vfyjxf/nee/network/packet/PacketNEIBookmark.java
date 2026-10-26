package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.vfyjxf.nee.utils.ItemUtils;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

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
                final IMEMonitor<IAEItemStack> monitor = monitorable.getItemMonitor();
                if (monitor != null) {
                    final IEnergySource energy = monitorable.getPowerSource();
                    final BaseActionSource actionSource = monitorable.getActionSource();
                    final InventoryAdaptor playerInventory = InventoryAdaptor
                            .getAdaptor(player, ForgeDirection.UNKNOWN);

                    for (String key : message.bookmarkItems.func_150296_c()) {
                        final ItemStack bookmarkItem = ItemUtils
                                .loadItemStackFromNBT(message.bookmarkItems.getCompoundTag(key));
                        if (bookmarkItem == null || bookmarkItem.stackSize <= 0) {
                            continue;
                        }

                        final AEItemStack requestStack = AEItemStack.create(bookmarkItem);
                        final ItemStack overflow = playerInventory.simulateAdd(bookmarkItem);
                        if (overflow != null) {
                            requestStack.decStackSize(overflow.stackSize);
                        }
                        if (requestStack.getStackSize() <= 0) {
                            continue;
                        }

                        final IAEItemStack extractedStack = Platform
                                .poweredExtraction(energy, monitor, requestStack, actionSource);
                        if (extractedStack != null) {
                            final ItemStack leftover = playerInventory.addItems(extractedStack.getItemStack());
                            if (leftover != null) {
                                final IAEItemStack rejected = monitor
                                        .injectItems(AEItemStack.create(leftover), Actionable.MODULATE, actionSource);
                                if (rejected != null) {
                                    Platform.addToPlayerInvOrDrop(player, rejected.getItemStack());
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }
}
