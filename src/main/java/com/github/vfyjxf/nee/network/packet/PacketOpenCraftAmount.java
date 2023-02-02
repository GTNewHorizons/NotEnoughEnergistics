package com.github.vfyjxf.nee.network.packet;

import static com.github.vfyjxf.nee.nei.NEECraftingHandler.OUTPUT_KEY;
import static com.github.vfyjxf.nee.network.NEEGuiHandler.CRAFTING_AMOUNT_ID;
import static com.github.vfyjxf.nee.network.NEEGuiHandler.CRAFTING_AMOUNT_WIRELESS_ID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftingTerm;

import com.github.vfyjxf.nee.container.ContainerCraftingAmount;
import com.github.vfyjxf.nee.network.NEEGuiHandler;
import com.github.vfyjxf.nee.utils.GuiUtils;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketOpenCraftAmount implements IMessage {

    private NBTTagCompound recipe;

    public PacketOpenCraftAmount() {}

    public PacketOpenCraftAmount(NBTTagCompound recipe) {
        this.recipe = recipe;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.recipe = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.recipe);
    }

    public static final class Handler implements IMessageHandler<PacketOpenCraftAmount, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenCraftAmount message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;
            if (container instanceof ContainerCraftingTerm) {
                final ContainerOpenContext context = ((ContainerCraftingTerm) container).getOpenContext();
                if (context != null) {
                    final TileEntity tile = context.getTile();

                    NEEGuiHandler.openGui(player, CRAFTING_AMOUNT_ID, tile, context.getSide());

                    if (player.openContainer instanceof ContainerCraftingAmount) {
                        ContainerCraftingAmount cca = (ContainerCraftingAmount) player.openContainer;
                        if (message.recipe != null) {
                            NBTTagCompound tag = message.recipe.getCompoundTag(OUTPUT_KEY);
                            ItemStack result = tag == null ? null : ItemStack.loadItemStackFromNBT(tag);
                            if (result != null) {
                                cca.setResultStack(result);
                                cca.getResultSlot().putStack(result);
                                cca.setRecipe(message.recipe);
                            }
                        }
                        cca.detectAndSendChanges();
                    }
                }
            } else if (GuiUtils.isWirelessCraftingTermContainer(container)) {

                NEEGuiHandler.openGui(player, CRAFTING_AMOUNT_WIRELESS_ID, player.worldObj);

                if (player.openContainer instanceof ContainerCraftingAmount) {
                    ContainerCraftingAmount cca = (ContainerCraftingAmount) player.openContainer;
                    if (message.recipe != null) {
                        NBTTagCompound tag = message.recipe.getCompoundTag(OUTPUT_KEY);
                        ItemStack result = tag == null ? null : ItemStack.loadItemStackFromNBT(tag);
                        cca.setResultStack(result);
                        cca.getResultSlot().putStack(result);
                        cca.setRecipe(message.recipe);
                    }
                    cca.detectAndSendChanges();
                }
            }
            return null;
        }
    }
}
