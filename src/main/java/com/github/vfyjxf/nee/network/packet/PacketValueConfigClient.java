package com.github.vfyjxf.nee.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.github.vfyjxf.nee.nei.NEECraftingPreviewHandler;

import codechicken.nei.recipe.GuiRecipe;
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
    private boolean value;

    public PacketValueConfigClient() {}

    public PacketValueConfigClient(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    public PacketValueConfigClient(String name) {
        this.name = name;
        this.value = false;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.value = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name);
        buf.writeBoolean(this.value);
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
            final GuiScreen gui = Minecraft.getMinecraft().currentScreen;

            if ("PatternInterface.check".equals(message.name) && gui instanceof GuiRecipe<?>) {
                NEECraftingPreviewHandler.instance.setIsPatternInterfaceExists(message.value);
            }
        }
    }
}
