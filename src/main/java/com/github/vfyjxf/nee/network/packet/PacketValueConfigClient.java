package com.github.vfyjxf.nee.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import codechicken.nei.recipe.GuiRecipe;

import com.github.vfyjxf.nee.nei.NEECraftingHelper;
import com.github.vfyjxf.nee.utils.GuiUtils;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * @author vfyjxf
 */
public class PacketValueConfigClient implements IMessage {

    private String name;
    private String value;

    public PacketValueConfigClient() {}

    public PacketValueConfigClient(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public PacketValueConfigClient(String name) {
        this.name = name;
        this.value = "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.value = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name);
        ByteBufUtils.writeUTF8String(buf, this.value);
    }

    public static final class Handler implements IMessageHandler<PacketValueConfigClient, IMessage> {

        @Override
        public IMessage onMessage(PacketValueConfigClient message, MessageContext ctx) {
            if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                handMessage(message, ctx);
            }
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handMessage(PacketValueConfigClient message, MessageContext ctx) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if ("PatternInterface.check".equals(message.name)) {
                if (gui instanceof GuiRecipe) {
                    GuiRecipe guiRecipe = (GuiRecipe) gui;
                    if (GuiUtils.isGuiCraftingTerm(guiRecipe.firstGui)) {
                        boolean value = Boolean.parseBoolean(message.value);
                        NEECraftingHelper.setIsPatternInterfaceExists(value);
                    }
                }
            }
        }
    }
}
