package com.github.vfyjxf.nee.network.packet;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import thaumicenergistics.common.container.ContainerKnowledgeInscriber;
import appeng.container.slot.SlotFakeCraftingMatrix;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;

/**
 * @author vfyjxf
 */
public class PacketArcaneRecipe implements IMessage {

    NBTTagCompound input;

    public PacketArcaneRecipe() {}

    public PacketArcaneRecipe(NBTTagCompound input) {
        this.input = input;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.input = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.input);
    }

    public static final class Handler implements IMessageHandler<PacketArcaneRecipe, IMessage> {

        @Override
        public IMessage onMessage(PacketArcaneRecipe message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;
            if (container instanceof ContainerKnowledgeInscriber) {
                ItemStack[] recipeInput = new ItemStack[9];
                NBTTagCompound currentStack;
                for (int i = 0; i < recipeInput.length; i++) {
                    currentStack = (NBTTagCompound) message.input.getTag("#" + i);
                    recipeInput[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
                }

                Field craftingSlots = ReflectionHelper.findField(ContainerKnowledgeInscriber.class, "craftingSlots");
                SlotFakeCraftingMatrix[] craftMatrix = this
                        .getCraftingSlots(craftingSlots, (ContainerKnowledgeInscriber) container);
                if (craftMatrix != null && message.input != null) {
                    for (int i = 0; i < recipeInput.length; i++) {
                        ItemStack currentItem = null;
                        if (recipeInput[i] != null) {
                            currentItem = recipeInput[i].copy();
                        }
                        craftMatrix[i].putStack(currentItem);
                    }
                    container.onCraftMatrixChanged(craftMatrix[0].inventory);
                }
            }
            return null;
        }

        private SlotFakeCraftingMatrix[] getCraftingSlots(Field field, ContainerKnowledgeInscriber container) {
            try {
                return (SlotFakeCraftingMatrix[]) field.get(container);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
