package com.github.vfyjxf.nee.network.packet;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.github.vfyjxf.nee.NotEnoughEnergistics;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.util.item.AEItemStack;
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

    private IAEItemStack stack;
    private Int2ObjectMap<IAEItemStack> slotStacks;

    public PacketSlotStackChange() {}

    public PacketSlotStackChange(@Nonnull Int2ObjectMap<IAEItemStack> slotStacks) {
        this.slotStacks = slotStacks;
    }

    public IAEItemStack getStack() {
        return stack;
    }

    public Int2ObjectMap<IAEItemStack> getSlotStacks() {
        return slotStacks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int mapSize = buf.readInt();
        this.slotStacks = new Int2ObjectOpenHashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            int slotNumber = buf.readInt();
            try {
                IAEItemStack stack = AEItemStack.loadItemStackFromPacket(buf);
                this.slotStacks.put(slotNumber, stack);
            } catch (IOException e) {
                NotEnoughEnergistics.logger.error(e);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotStacks.size());
        for (Int2ObjectMap.Entry<IAEItemStack> entry : this.slotStacks.int2ObjectEntrySet()) {
            buf.writeInt(entry.getIntKey());
            try {
                entry.getValue().writeToPacket(buf);
            } catch (IOException e) {
                NotEnoughEnergistics.logger.error(e);
            }
        }
    }

    public static final class Handler implements IMessageHandler<PacketSlotStackChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSlotStackChange message, MessageContext ctx) {
            if (ctx.getServerHandler().playerEntity.openContainer instanceof ContainerPatternTerm cpt) {
                final Int2ObjectMap<IAEStack<?>> temp = new Int2ObjectOpenHashMap<>();
                for (Int2ObjectMap.Entry<IAEItemStack> entry : message.getSlotStacks().int2ObjectEntrySet()) {
                    temp.put(entry.getIntKey(), entry.getValue());
                }
                cpt.receiveSlotStacks(StorageName.CRAFTING_INPUT, temp);
            }
            return null;
        }
    }
}
