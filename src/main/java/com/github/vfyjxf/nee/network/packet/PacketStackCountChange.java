package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * @author vfyjxf
 */
public class PacketStackCountChange implements IMessage {

    private int slotIndex;
    private int changeCount;
    private StorageName sn;

    public PacketStackCountChange() {}

    public PacketStackCountChange(int slotIndex, StorageName sn, int changeCount) {
        this.slotIndex = slotIndex;
        this.changeCount = changeCount;
        this.sn = sn;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getChangeCount() {
        return changeCount;
    }

    public StorageName getStorageName() {
        return sn;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.changeCount = buf.readInt();
        this.sn = StorageName.values()[buf.readInt()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeInt(this.changeCount);
        buf.writeInt(this.sn.ordinal());
    }

    public static final class Handler implements IMessageHandler<PacketStackCountChange, IMessage> {

        @Override
        public IMessage onMessage(PacketStackCountChange message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;
            if (container instanceof ContainerPatternTerm cpt && !cpt.isCraftingMode()) {
                handleMessage(message, cpt);
            }
            return null;
        }

        private void handleMessage(PacketStackCountChange message, ContainerPatternTerm cpt) {
            final IAEStack<?> aes = cpt.getPatternTerminal().getAEInventoryByName(message.getStorageName())
                    .getAEStackInSlot(message.getSlotIndex());
            if (aes != null) {
                final IAEStack<?> newAes = aes.copy();
                newAes.setStackSize(Math.max(1, aes.getStackSize() + message.getChangeCount()));
                cpt.updateVirtualSlot(message.getStorageName(), message.getSlotIndex(), newAes);
            }
        }
    }
}
