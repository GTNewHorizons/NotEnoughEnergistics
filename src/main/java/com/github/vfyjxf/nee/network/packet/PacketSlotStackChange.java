package com.github.vfyjxf.nee.network.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.vfyjxf.nee.utils.ItemUtils;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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
        this.stack = ItemUtils.loadItemStackFromNBT(ByteBufUtils.readTag(buf));
        int craftingSlotsSize = buf.readInt();
        this.craftingSlots = new ArrayList<>(craftingSlotsSize);
        for (int i = 0; i < craftingSlotsSize; i++) {
            int slotNumber = buf.readInt();
            craftingSlots.add(slotNumber);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, ItemUtils.writeItemStackToNBT(this.stack, this.stack.stackSize));
        buf.writeInt(this.craftingSlots.size());
        for (Integer craftingSlot : this.craftingSlots) {
            buf.writeInt(craftingSlot);
        }
    }

    public static final class Handler implements IMessageHandler<PacketSlotStackChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSlotStackChange message, MessageContext ctx) {
            final IAEStack<?> nextStack = AEItemStack.create(message.getStack());
            if (nextStack != null
                    && ctx.getServerHandler().playerEntity.openContainer instanceof ContainerPatternTerm cpt) {
                final Int2ObjectMap<IAEStack<?>> temp = new Int2ObjectOpenHashMap<>();
                for (int craftingSlot : message.getCraftingSlots()) {
                    temp.put(craftingSlot, nextStack);
                }
                cpt.receiveSlotStacks(StorageName.CRAFTING_INPUT, temp);
            }
            return null;
        }
    }
}
