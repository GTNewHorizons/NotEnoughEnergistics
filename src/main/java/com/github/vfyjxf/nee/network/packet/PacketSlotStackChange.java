package com.github.vfyjxf.nee.network.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * @author vfyjxf
 */
public class PacketSlotStackChange implements IMessage {

    private ItemStack stack;
    private List<Integer> craftingSlots;

    public PacketSlotStackChange() {}

    public PacketSlotStackChange(ItemStack stack, List<Integer> craftingSlots) {
        this.stack = stack;
        this.craftingSlots = craftingSlots;
    }

    public ItemStack getStack() {
        return stack;
    }

    public List<Integer> getCraftingSlots() {
        return craftingSlots;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.stack = ByteBufUtils.readItemStack(buf);
        int craftingSlotsSize = buf.readInt();
        this.craftingSlots = new ArrayList<>(craftingSlotsSize);
        for (int i = 0; i < craftingSlotsSize; i++) {
            int slotNumber = buf.readInt();
            craftingSlots.add(slotNumber);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, this.stack);
        buf.writeInt(this.craftingSlots.size());
        for (Integer craftingSlot : this.craftingSlots) {
            buf.writeInt(craftingSlot);
        }
    }

    public static final class Handler implements IMessageHandler<PacketSlotStackChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSlotStackChange message, MessageContext ctx) {
            Container container = ctx.getServerHandler().playerEntity.openContainer;
            ItemStack nextStack = message.getStack();
            if (nextStack != null) {
                for (Integer craftingSlot : message.getCraftingSlots()) {
                    Slot currentSlot = container.getSlot(craftingSlot);
                    if (currentSlot != null) {
                        currentSlot.putStack(nextStack);
                    }
                }
            }
            return null;
        }
    }
}
