package com.github.vfyjxf.nee.network.packet;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.helpers.IContainerCraftingPacket;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

import static com.github.vfyjxf.nee.nei.NEECraftingHandler.OUTPUT_KEY;


public class PacketNEIPatternRecipe implements IMessage{

    NBTTagCompound input;
    NBTTagCompound output;

    public PacketNEIPatternRecipe() {
    }

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
            if (container instanceof ContainerPatternTerm && message.output == null) {
                ((ContainerPatternTerm) container).getPatternTerminal().setCraftingRecipe(true);
                message.craftingTableRecipeHandler((ContainerPatternTerm) container, message);
            } else if (container instanceof ContainerPatternTerm) {
                ((ContainerPatternTerm) container).getPatternTerminal().setCraftingRecipe(false);
                message.processRecipeHandler((ContainerPatternTerm) container, message);
            } else if (container instanceof ContainerPatternTermEx && message.output != null) {
                ((ContainerPatternTermEx) container).getPatternTerminal().setInverted(false);
                message.processRecipeHandler((ContainerPatternTermEx) container, message);
            }
            return null;
        }

    }

    private void craftingTableRecipeHandler(ContainerPatternTerm container, PacketNEIPatternRecipe message) {
        ItemStack[] recipeInput = new ItemStack[9];
        NBTTagCompound currentStack;


        for (int i = 0; i < recipeInput.length; i++) {
            currentStack = (NBTTagCompound) message.input.getTag("#" + i);
            recipeInput[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }

        final IContainerCraftingPacket cct = container;
        final IGridNode node = cct.getNetworkNode();

        if (node != null) {
            final IGrid grid = node.getGrid();
            if (grid == null) {
                return;
            }

            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            final IInventory craftMatrix = cct.getInventoryByName("crafting");

            if (inv != null && message.input != null && security != null) {
                for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
                    ItemStack currentItem = null;
                    if (recipeInput[i] != null) {
                        currentItem = recipeInput[i].copy();
                    }
                    craftMatrix.setInventorySlotContents(i, currentItem);
                }
                container.onCraftMatrixChanged(craftMatrix);
            }

        }

    }

    private void processRecipeHandler(ContainerPatternTerm container, PacketNEIPatternRecipe message) {

        ItemStack[] recipeInput = new ItemStack[9];
        ItemStack[] recipeOutput = new ItemStack[3];


        for (int i = 0; i < recipeInput.length; i++) {
            NBTTagCompound currentStack = (NBTTagCompound) message.input.getTag("#" + i);
            recipeInput[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }

        for (int i = 0; i < recipeOutput.length; i++) {
            NBTTagCompound currentStack = (NBTTagCompound) message.output.getTag(OUTPUT_KEY + i);
            recipeOutput[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }

        final IContainerCraftingPacket cct = container;
        final IGridNode node = cct.getNetworkNode();

        if (node != null) {
            final IGrid grid = node.getGrid();
            if (grid == null) {
                return;
            }
            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            final IInventory craftMatrix = cct.getInventoryByName("crafting");
            final IInventory outputMatrix = cct.getInventoryByName("output");

            if (inv != null && message.input != null && security != null) {
                for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
                    ItemStack currentItem = null;
                    if (recipeInput[i] != null) {
                        currentItem = recipeInput[i].copy();
                    }
                    craftMatrix.setInventorySlotContents(i, currentItem);
                }

                for (int i = 0; i < outputMatrix.getSizeInventory(); i++) {
                    ItemStack currentItem = null;
                    if (recipeOutput[i] != null) {
                        currentItem = recipeOutput[i].copy();
                    }
                    outputMatrix.setInventorySlotContents(i, currentItem);
                }
                container.onCraftMatrixChanged(craftMatrix);
            }
        }
    }

    private void processRecipeHandler(ContainerPatternTermEx container, PacketNEIPatternRecipe message) {

        ItemStack[] recipeInput = new ItemStack[16];
        ItemStack[] recipeOutput = new ItemStack[4];


        for (int i = 0; i < recipeInput.length; i++) {
            NBTTagCompound currentStack = (NBTTagCompound) message.input.getTag("#" + i);
            recipeInput[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }

        for (int i = 0; i < recipeOutput.length; i++) {
            NBTTagCompound currentStack = (NBTTagCompound) message.output.getTag(OUTPUT_KEY + i);
            recipeOutput[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }

        final IContainerCraftingPacket cct = container;
        final IGridNode node = cct.getNetworkNode();

        if (node != null) {
            final IGrid grid = node.getGrid();
            if (grid == null) {
                return;
            }
            final IStorageGrid inv = grid.getCache(IStorageGrid.class);
            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            final IInventory craftMatrix = cct.getInventoryByName("crafting");
            final IInventory outputMatrix = cct.getInventoryByName("output");

            if (inv != null && message.input != null && security != null) {
                for (int i = 0; i < Math.min(craftMatrix.getSizeInventory(), recipeInput.length); i++) {
                    ItemStack currentItem = null;
                    if (recipeInput[i] != null) {
                        currentItem = recipeInput[i].copy();
                    }
                    craftMatrix.setInventorySlotContents(i, currentItem);
                }

                for (int i = 0; i < recipeOutput.length; i++) {
                    ItemStack currentItem = null;
                    if (recipeOutput[i] != null) {
                        currentItem = recipeOutput[i].copy();
                    }
                    outputMatrix.setInventorySlotContents(i, currentItem);
                }
                container.onCraftMatrixChanged(craftMatrix);
            }
        }
    }


}
