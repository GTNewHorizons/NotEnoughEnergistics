package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;

import com.github.vfyjxf.nee.network.NEEGuiHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketOpenGui implements IMessage {

    private int guiId;

    public PacketOpenGui() {}

    public PacketOpenGui(int guiId) {
        this.guiId = guiId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.guiId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.guiId);
    }

    public static final class Handler implements IMessageHandler<PacketOpenGui, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;
            if (container instanceof AEBaseContainer) {
                final ContainerOpenContext context = ((AEBaseContainer) container).getOpenContext();
                if (context != null) {
                    final TileEntity tile = context.getTile();
                    NEEGuiHandler.openGui(player, message.guiId, tile, context.getSide());
                }
            }
            return null;
        }
    }
}
