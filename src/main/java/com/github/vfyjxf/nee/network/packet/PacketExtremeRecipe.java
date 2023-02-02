package com.github.vfyjxf.nee.network.packet;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import wanion.avaritiaddons.block.extremeautocrafter.ContainerExtremeAutoCrafter;
import wanion.avaritiaddons.block.extremeautocrafter.TileEntityExtremeAutoCrafter;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;

/**
 * @author vfyjxf
 */
public class PacketExtremeRecipe implements IMessage {

    private NBTTagCompound input;

    public PacketExtremeRecipe() {}

    public PacketExtremeRecipe(@Nonnull NBTTagCompound input) {
        this.input = input;
    }

    public NBTTagCompound getInput() {
        return input;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.input = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.input);
    }

    public static final class Handler implements IMessageHandler<PacketExtremeRecipe, IMessage> {

        @Override
        public IMessage onMessage(PacketExtremeRecipe message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;
            if (container instanceof ContainerExtremeAutoCrafter) {
                ItemStack[] recipeInputs = new ItemStack[81];
                NBTTagCompound currentStack;
                for (int i = 0; i < recipeInputs.length; i++) {
                    currentStack = (NBTTagCompound) message.input.getTag("#" + i);
                    recipeInputs[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
                }
                int inputIndex = 0;
                for (int i = 81; i < 162; i++) {
                    Slot fakeSlot = container.getSlot(i);
                    ItemStack currentItem = recipeInputs[inputIndex];
                    fakeSlot.putStack(currentItem);
                    inputIndex++;
                }
                InventoryCrafting craftingMatrix = getCraftingMatrix((ContainerExtremeAutoCrafter) container);
                if (craftingMatrix != null) {
                    container.onCraftMatrixChanged(craftingMatrix);
                }
            }
            return null;
        }

        private InventoryCrafting getCraftingMatrix(ContainerExtremeAutoCrafter container) {
            Field tileEntityExtremeAutoCrafterField = ReflectionHelper
                    .findField(ContainerExtremeAutoCrafter.class, "tileEntityExtremeAutoCrafter");
            TileEntityExtremeAutoCrafter tileEntityExtremeAutoCrafter = null;

            try {
                tileEntityExtremeAutoCrafter = (TileEntityExtremeAutoCrafter) tileEntityExtremeAutoCrafterField
                        .get(container);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (tileEntityExtremeAutoCrafter != null) {
                Field craftingMatrixField = ReflectionHelper
                        .findField(TileEntityExtremeAutoCrafter.class, "craftingMatrix");
                InventoryCrafting craftingMatrix = null;
                try {
                    craftingMatrix = (InventoryCrafting) craftingMatrixField.get(tileEntityExtremeAutoCrafter);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return craftingMatrix;
            }
            return null;
        }
    }
}
