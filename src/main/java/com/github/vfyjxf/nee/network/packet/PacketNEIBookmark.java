package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.vfyjxf.nee.utils.ItemUtils;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketNEIBookmark implements IMessage {

    NBTTagCompound bookmarkItems;

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
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;

            if (container instanceof AEBaseContainer && message.bookmarkItems != null) {
                AEBaseContainer baseContainer = (AEBaseContainer) container;

                for (Object key : message.bookmarkItems.func_150296_c()) {
                    final IAEItemStack request = AEItemStack
                            .create(ItemUtils.loadItemStackFromNBT(message.bookmarkItems.getCompoundTag((String) key)));
                    final IAEItemStack extracted = Platform.poweredExtraction(
                            baseContainer.getPowerSource(),
                            baseContainer.getCellInventory(),
                            request,
                            baseContainer.getActionSource());

                    if (extracted != null) {
                        InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN).addItems(extracted.getItemStack());
                    }
                }
            }

            return null;
        }
    }
}
