package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.github.vfyjxf.nee.utils.GuiUtils;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import codechicken.nei.recipe.StackInfo;
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

    public PacketStackCountChange() {}

    public PacketStackCountChange(int slotIndex, int changeCount) {
        this.slotIndex = slotIndex;
        this.changeCount = changeCount;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getChangeCount() {
        return changeCount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.changeCount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeInt(this.changeCount);
    }

    public static final class Handler implements IMessageHandler<PacketStackCountChange, IMessage> {

        @Override
        public IMessage onMessage(PacketStackCountChange message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;
            if (container instanceof AEBaseContainer) {
                handleMessage(message, container);
            }
            return null;
        }

        private void handleMessage(PacketStackCountChange message, Container container) {

            if (container instanceof ContainerPatternTerm && ((ContainerPatternTerm) container).isCraftingMode()) {
                return;
            }

            Slot currentSlot = container.getSlot(message.getSlotIndex());

            if (currentSlot.getHasStack()) {
                ItemStack stack = currentSlot.getStack();

                if (GuiUtils.isFluidCraftPatternContainer(container)
                        && StackInfo.itemStackToNBT(stack).hasKey("gtFluidName")) {
                    FluidStack fluid = StackInfo.getFluid(stack);
                    fluid.amount = Math.max(1, fluid.amount + message.getChangeCount());
                    currentSlot.putStack(ItemFluidPacket.newStack(fluid));
                } else {
                    stack = stack.copy();
                    stack.stackSize = Math.max(1, stack.stackSize + message.getChangeCount());
                    currentSlot.putStack(stack);
                }
            }
        }
    }
}
