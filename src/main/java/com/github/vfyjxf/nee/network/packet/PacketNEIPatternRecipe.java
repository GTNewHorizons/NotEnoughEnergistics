package com.github.vfyjxf.nee.network.packet;

import static com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler.OUTPUT_KEY;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ItemUtils;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PacketNEIPatternRecipe implements IMessage {

    NBTTagCompound input;
    NBTTagCompound output;

    public PacketNEIPatternRecipe() {}

    public PacketNEIPatternRecipe(@Nonnull NBTTagCompound input, NBTTagCompound output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.input = ByteBufUtils.readTag(buf);
        if (buf.readBoolean()) {
            this.output = ByteBufUtils.readTag(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.input);
        if (this.output != null) {
            buf.writeBoolean(true);
            ByteBufUtils.writeTag(buf, this.output);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static final class Handler implements IMessageHandler<PacketNEIPatternRecipe, IMessage> {

        @Override
        public IMessage onMessage(PacketNEIPatternRecipe message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;

            if (message.input != null && GuiUtils.isPatternContainer(container)
                    && container instanceof IContainerCraftingPacket) {
                ContainerPatternTerm baseContainer = (ContainerPatternTerm) container;
                setCraftingRecipe(baseContainer, message.output == null);

                if (message.output == null) {
                    craftingRecipe(baseContainer, message);
                } else {
                    processRecipe(baseContainer, message);
                }
            }

            return null;
        }

        private void setCraftingRecipe(AEBaseContainer container, boolean craftingMode) {

            if (container instanceof ContainerPatternTerm cpt) {
                cpt.getPatternTerminal().setCraftingRecipe(craftingMode);
            }

        }

        private ItemStack[] getMatrix(NBTTagCompound items, String prefix, int size) {
            ItemStack[] matrix = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                NBTTagCompound currentStack = (NBTTagCompound) items.getTag(prefix + i);
                matrix[i] = currentStack == null ? null : ItemUtils.loadItemStackFromNBT(currentStack);
            }

            return matrix;
        }

        private void craftingRecipe(ContainerPatternTerm container, PacketNEIPatternRecipe message) {
            final IGridNode node = container.getNetworkNode();

            if (node == null) {
                return;
            }

            final IGrid grid = node.getGrid();

            if (grid == null) {
                return;
            }

            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);

            if (inv != null && security != null) {
                var list = new Int2ObjectOpenHashMap<IAEStack<?>>();
                final ItemStack[] recipeInput = getMatrix(message.input, INPUT_KEY, 9);

                for (int i = 0; i < recipeInput.length; i++) {
                    IAEStack<?> aes = AEItemStack.create(recipeInput[i]);
                    list.put(i, aes);
                }

                container.receiveSlotStacks(StorageName.CRAFTING_INPUT, list);
            }
        }

        private void processRecipe(ContainerPatternTerm container, PacketNEIPatternRecipe message) {
            final IGridNode node = container.getNetworkNode();

            if (node == null) {
                return;
            }

            final IGrid grid = node.getGrid();

            if (grid == null) {
                return;
            }

            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);

            if (inv != null && security != null) {
                var input = new Int2ObjectOpenHashMap<IAEStack<?>>();
                var output = new Int2ObjectOpenHashMap<IAEStack<?>>();

                final ItemStack[] recipeInput = getMatrix(
                        message.input,
                        INPUT_KEY,
                        container.getPatternInputsWidth() * container.getPatternInputsHeigh()
                                * container.getPatternInputPages());
                final ItemStack[] recipeOutput = getMatrix(
                        message.output,
                        OUTPUT_KEY,
                        container.getPatternOutputsWidth() * container.getPatternOutputsHeigh()
                                * container.getPatternOutputPages());

                for (int i = 0; i < recipeInput.length; i++) {
                    final ItemStack nextStack = recipeInput[i];
                    final IAEStack<?> aes;

                    if (StackInfo.itemStackToNBT(nextStack).hasKey("gtFluidName")) {
                        aes = AEFluidStack.create(StackInfo.getFluid(nextStack));
                    } else {
                        aes = AEItemStack.create(recipeInput[i]);
                    }

                    input.put(i, aes);
                }

                for (int i = 0; i < recipeOutput.length; i++) {
                    IAEStack<?> aes = AEItemStack.create(recipeOutput[i]);
                    output.put(i, aes);
                }

                container.receiveSlotStacks(StorageName.CRAFTING_INPUT, input);
                container.receiveSlotStacks(StorageName.CRAFTING_OUTPUT, output);
            }
        }
    }
}
