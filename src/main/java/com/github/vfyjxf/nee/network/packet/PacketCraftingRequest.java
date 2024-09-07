package com.github.vfyjxf.nee.network.packet;

import java.io.IOException;
import java.util.concurrent.Future;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketCraftingRequest implements IMessage {

    private IAEItemStack requireToCraftStack;
    private boolean isAutoStart;
    private int craftAmount;

    public PacketCraftingRequest() {}

    public PacketCraftingRequest(IAEItemStack requireToCraftStack, boolean isAutoStart) {
        this.requireToCraftStack = requireToCraftStack;
        this.isAutoStart = isAutoStart;
    }

    public PacketCraftingRequest(int craftAmount, boolean isAutoStart) {
        this.craftAmount = craftAmount;
        this.isAutoStart = isAutoStart;
    }

    public IAEItemStack getRequireToCraftStack() {
        return requireToCraftStack;
    }

    public boolean isAutoStart() {
        return isAutoStart;
    }

    public int getCraftAmount() {
        return craftAmount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readBoolean()) {
            try {
                this.requireToCraftStack = AEItemStack.loadItemStackFromPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.isAutoStart = buf.readBoolean();
        this.craftAmount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (this.requireToCraftStack != null) {
            try {
                buf.writeBoolean(true);
                this.requireToCraftStack.writeToPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            buf.writeBoolean(false);
        }
        buf.writeBoolean(this.isAutoStart);
        buf.writeInt(this.craftAmount);
    }

    public static final class Handler implements IMessageHandler<PacketCraftingRequest, IMessage> {

        @Override
        public IMessage onMessage(PacketCraftingRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;

            if (container instanceof AEBaseContainer && message.getRequireToCraftStack() != null) {
                AEBaseContainer baseContainer = (AEBaseContainer) container;
                Object target = baseContainer.getTarget();

                if (target instanceof IGridHost) {
                    final IGridNode gridNode = ((IGridHost) target).getGridNode(ForgeDirection.UNKNOWN);

                    if (gridNode != null) {
                        final IGrid grid = gridNode.getGrid();

                        if (grid != null) {
                            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);

                            if (security != null && security.hasPermission(player, SecurityPermissions.CRAFT)) {

                                handlerCraftingTermRequest(
                                        (IContainerCraftingPacket) container,
                                        baseContainer.getOpenContext(),
                                        grid,
                                        message,
                                        player);

                            }
                        }

                    }
                }
            }

            return null;
        }

        private void handlerCraftingTermRequest(IContainerCraftingPacket craftingPacket, ContainerOpenContext context,
                IGrid grid, PacketCraftingRequest message, EntityPlayerMP player) {
            Future<ICraftingJob> futureJob = null;

            try {
                final ICraftingGrid cg = grid.getCache(ICraftingGrid.class);

                futureJob = cg.beginCraftingJob(
                        player.worldObj,
                        grid,
                        craftingPacket.getActionSource(),
                        message.getRequireToCraftStack(),
                        null);

                if (context != null) {
                    Platform.openGUI(player, context.getTile(), context.getSide(), GuiBridge.GUI_CRAFTING_CONFIRM);

                    if (player.openContainer instanceof ContainerCraftConfirm) {
                        final ContainerCraftConfirm ccc = (ContainerCraftConfirm) player.openContainer;
                        ccc.setItemToCraft(message.getRequireToCraftStack());
                        ccc.setJob(futureJob);
                        ccc.setAutoStart(message.isAutoStart());
                    }
                }

            } catch (final Throwable e) {
                if (futureJob != null) {
                    futureJob.cancel(true);
                }
                AELog.debug(e);
            }

        }

    }
}
