package com.github.vfyjxf.nee.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.helpers.WirelessTerminalGuiObject;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotRestrictedInput;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.github.vfyjxf.nee.container.ContainerPatternInterface;
import com.github.vfyjxf.nee.network.NEENetworkHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketValueConfigServer implements IMessage {

    private String name;
    private String value;

    public PacketValueConfigServer() {}

    public PacketValueConfigServer(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public PacketValueConfigServer(String name) {
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

    public static final class Handler implements IMessageHandler<PacketValueConfigServer, IMessage> {

        @Override
        public IMessage onMessage(PacketValueConfigServer message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;

            if ("Container.selectedSlot".equals(message.name)) {
                if (container instanceof ContainerPatternInterface) {
                    ContainerPatternInterface cpc = (ContainerPatternInterface) container;
                    int slotIndex = Integer.parseInt(message.value);
                    Slot slot = container.getSlot(slotIndex);
                    if (slot instanceof SlotRestrictedInput) {
                        cpc.setSelectedSlotIndex(slot.slotNumber);
                    }
                }
            } else if ("Gui.PatternInterface".equals(message.name)) {
                if (container instanceof ContainerPatternInterface) {
                    ContainerPatternInterface cpc = (ContainerPatternInterface) container;
                    TilePatternInterface tile = (TilePatternInterface) cpc.getTileEntity();
                    tile.cancelWork(cpc.getSelectedSlotIndex());
                    cpc.removeCurrentRecipe();
                    tile.updateCraftingList();
                }
            } else if ("PatternInterface.check".equals(message.name)) {
                if (container instanceof AEBaseContainer) {
                    IGrid grid = message.getNetwork((AEBaseContainer) container);
                    if (grid != null) {

                        for (IGridNode gridNode : grid.getMachines(TilePatternInterface.class)) {

                            if (gridNode.getMachine() instanceof TilePatternInterface) {
                                NEENetworkHandler.getInstance()
                                        .sendTo(new PacketValueConfigClient("PatternInterface.check", "true"), player);
                                return null;
                            }
                        }

                        NEENetworkHandler.getInstance()
                                .sendTo(new PacketValueConfigClient("PatternInterface.check", "false"), player);
                    }
                }
            }

            return null;
        }
    }

    private IGrid getNetwork(AEBaseContainer container) {
        if (container.getTarget() instanceof IActionHost) {
            IActionHost ah = (IActionHost) container.getTarget();
            IGridNode gn = ah.getActionableNode();
            return gn.getGrid();
        }
        return null;
    }

    private IGrid getNetwork(ContainerWirelessCraftingTerminal container) {

        if (container.getTarget() instanceof WirelessTerminalGuiObject) {
            return ((WirelessTerminalGuiObject) container.getTarget()).getTargetGrid();
        }

        return null;
    }
}
