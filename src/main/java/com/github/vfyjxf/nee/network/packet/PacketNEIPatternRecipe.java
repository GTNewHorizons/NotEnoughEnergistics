package com.github.vfyjxf.nee.network.packet;

import static com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler.OUTPUT_KEY;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

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

            if (message.input != null && container instanceof AEBaseContainer
                    && container instanceof IContainerCraftingPacket) {
                AEBaseContainer baseContainer = (AEBaseContainer) container;
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
            } else if (Loader.isModLoaded(ModIDs.FC) && container instanceof ContainerFluidPatternTerminal cfpt) {
                cfpt.getPatternTerminal().setCraftingRecipe(craftingMode);
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

        private void craftingRecipe(AEBaseContainer container, PacketNEIPatternRecipe message) {
            final IContainerCraftingPacket cct = (IContainerCraftingPacket) container;
            final IGridNode node = cct.getNetworkNode();

            if (node == null) {
                return;
            }

            final IGrid grid = node.getGrid();

            if (grid == null) {
                return;
            }

            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            final IInventory craftMatrix = cct.getInventoryByName("crafting");

            if (inv != null && craftMatrix != null && security != null) {
                final ItemStack[] recipeInput = getMatrix(message.input, INPUT_KEY, craftMatrix.getSizeInventory());

                for (int i = 0; i < recipeInput.length; i++) {
                    craftMatrix.setInventorySlotContents(i, recipeInput[i]);
                }

                container.onCraftMatrixChanged(craftMatrix);
            }
        }

        private void processRecipe(AEBaseContainer container, PacketNEIPatternRecipe message) {
            final IContainerCraftingPacket cct = (IContainerCraftingPacket) container;
            final IGridNode node = cct.getNetworkNode();

            if (node == null) {
                return;
            }

            final IGrid grid = node.getGrid();

            if (grid == null) {
                return;
            }

            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            final IInventory craftMatrix = cct.getInventoryByName("crafting");
            final IInventory outputMatrix = cct.getInventoryByName("output");

            if (inv != null && craftMatrix != null && outputMatrix != null && security != null) {
                final ItemStack[] recipeInput = getMatrix(message.input, INPUT_KEY, craftMatrix.getSizeInventory());
                final ItemStack[] recipeOutput = getMatrix(message.output, OUTPUT_KEY, outputMatrix.getSizeInventory());

                for (int i = 0; i < recipeInput.length; i++) {
                    craftMatrix.setInventorySlotContents(i, recipeInput[i]);
                }

                for (int i = 0; i < recipeOutput.length; i++) {
                    outputMatrix.setInventorySlotContents(i, recipeOutput[i]);
                }

                container.onCraftMatrixChanged(craftMatrix);
                container.onCraftMatrixChanged(outputMatrix);
            }
        }

    }

}
