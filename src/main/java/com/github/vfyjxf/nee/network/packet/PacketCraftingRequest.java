package com.github.vfyjxf.nee.network.packet;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.inventory.item.IWirelessTerminal;
import com.glodblock.github.util.BlockPos;

import appeng.api.AEApi;
import appeng.api.config.CraftingMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.core.sync.GuiBridge;
import appeng.me.cache.CraftingGridCache;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import thaumicenergistics.common.ThEGuiHandler;
import thaumicenergistics.common.container.ContainerPartArcaneCraftingTerminal;

public class PacketCraftingRequest implements IMessage {

    public static int COMMAND_CREATE_PATTERN = 0;
    public static int COMMAND_REMOVE_PATTERN = 1;
    public static int COMMAND_OPEN_CRAFT_CONFIRM = 2;

    private String modID = "";
    private int command = 3;
    private NBTTagCompound compound;
    private boolean isAutoStart;
    private int craftAmount;

    public PacketCraftingRequest() {}

    public PacketCraftingRequest(String modID, int command, NBTTagCompound compound, int craftAmount,
            boolean isAutoStart) {
        this.modID = modID;
        this.command = command;
        this.compound = compound;
        this.craftAmount = craftAmount;
        this.isAutoStart = isAutoStart;
    }

    public boolean isAutoStart() {
        return this.isAutoStart;
    }

    public void setIsAutoStart(boolean isAutoStart) {
        this.isAutoStart = isAutoStart;
    }

    public int getCraftAmount() {
        return Math.max(1, this.craftAmount);
    }

    public void setCraftAmount(int craftAmount) {
        this.craftAmount = craftAmount;
    }

    public void getModID(String modID) {
        this.modID = modID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.modID = ByteBufUtils.readUTF8String(buf);
        this.command = buf.readInt();
        this.compound = ByteBufUtils.readTag(buf);
        this.isAutoStart = buf.readBoolean();
        this.craftAmount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.modID);
        buf.writeInt(this.command);
        ByteBufUtils.writeTag(buf, this.compound);
        buf.writeBoolean(this.isAutoStart);
        buf.writeInt(this.craftAmount);
    }

    public static final class Handler implements IMessageHandler<PacketCraftingRequest, IMessage> {

        @Override
        public IMessage onMessage(PacketCraftingRequest message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            final Container container = player.openContainer;
            final IGrid grid = GuiUtils.getGrid(container);

            if (grid == null) {
                return null;
            }

            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);

            if (security == null || !security.hasPermission(player, SecurityPermissions.CRAFT)) {
                return null;
            }

            if (message.command == COMMAND_REMOVE_PATTERN) {
                final ItemStack resultStack = ItemStack.loadItemStackFromNBT(message.compound.getCompoundTag("out"));

                if (resultStack != null) {
                    removeRecipePattern(grid, resultStack, message.compound, player);
                    container.detectAndSendChanges();
                }

            } else {

                if (message.command == COMMAND_OPEN_CRAFT_CONFIRM) {
                    final IStorageGrid inv = grid.getCache(IStorageGrid.class);
                    final ItemStack resultStack = ItemStack.loadItemStackFromNBT(message.compound);
                    final IAEItemStack requireToCraftStack = getrequireToCraftStack(
                            inv.getItemInventory(),
                            resultStack);

                    if (requireToCraftStack != null) {
                        requireToCraftStack.setStackSize(message.getCraftAmount());
                        message.openCraftConfirm(container, grid, requireToCraftStack, player);
                    }
                } else if (message.command == COMMAND_CREATE_PATTERN) {
                    final IStorageGrid inv = grid.getCache(IStorageGrid.class);
                    final ItemStack resultStack = ItemStack
                            .loadItemStackFromNBT(message.compound.getCompoundTag("out"));
                    createRecipePattern(grid, resultStack, message.compound, player);
                    final IAEItemStack requireToCraftStack = getrequireToCraftStack(
                            inv.getItemInventory(),
                            resultStack);

                    if (requireToCraftStack != null) {
                        requireToCraftStack.setStackSize(message.getCraftAmount());
                        message.openCraftConfirm(container, grid, requireToCraftStack, player);
                    }

                }

            }

            return null;
        }

        /*
         * For some reason,the output from jei is different from the actual one, so apply ItemStack.isItemEqual to
         * match.
         */
        private IAEItemStack getrequireToCraftStack(IMEMonitor<IAEItemStack> storage, ItemStack resultStack) {

            if (resultStack == null) {
                return null;
            }

            for (IAEItemStack aeStack : storage.getStorageList()) {
                if (resultStack.isItemEqual(aeStack.getItemStack()) && aeStack.isCraftable()) {
                    return aeStack.copy();
                }
            }

            return null;
        }

        private boolean createRecipePattern(IGrid grid, ItemStack resultStack, NBTTagCompound patternValue,
                EntityPlayer player) {
            ItemStack patternStack = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).orNull();

            if (patternStack == null) {
                return false;
            }

            for (IGridNode gridNode : grid.getMachines(TilePatternInterface.class)) {
                if (gridNode.getMachine() instanceof TilePatternInterface tpi) {
                    if (tpi.getProxy().isActive() && tpi.canPutPattern(resultStack)) {
                        patternStack = patternStack.copy();
                        patternStack.setTagCompound(patternValue);
                        tpi.putPattern(patternStack);
                        return true;
                    }
                }
            }

            return false;
        }

        private void removeRecipePattern(IGrid grid, ItemStack resultStack, NBTTagCompound patternValue,
                EntityPlayer player) {

            for (IGridNode gridNode : grid.getMachines(TilePatternInterface.class)) {
                if (gridNode.getMachine() instanceof TilePatternInterface tpi) {
                    final AppEngInternalInventory inventory = tpi.getPatternInventory();

                    for (int slotIndex = 0; slotIndex < inventory.getSizeInventory(); slotIndex++) {
                        final ItemStack patternStack = inventory.getStackInSlot(slotIndex);

                        if (patternStack != null && patternValue.equals(patternStack.getTagCompound())) {
                            inventory.setInventorySlotContents(slotIndex, null);
                            tpi.updateCraftingList();
                            return;
                        }
                    }

                }
            }

        }

    }

    public void openCraftConfirm(Container container, IGrid grid, IAEItemStack requireToCraftStack,
            EntityPlayerMP player) {

        if (Loader.isModLoaded(ModIDs.ThE) && this.modID.equals(ModIDs.ThE)
                && container instanceof ContainerPartArcaneCraftingTerminal cpact) {
            openTHContainerCraftConfirm(grid, requireToCraftStack, (IActionHost) cpact.terminal, player);
        } else if (container instanceof AEBaseContainer baseContainer) {
            if (Loader.isModLoaded(ModIDs.ThE) && this.modID.equals(ModIDs.ThE)) {
                openTHContainerCraftConfirm(grid, requireToCraftStack, (IActionHost) baseContainer.getTarget(), player);
            } else if (Loader.isModLoaded(ModIDs.FC) && this.modID.equals(ModIDs.FC)) {
                openFCContainerCraftConfirm(baseContainer, grid, requireToCraftStack, player);
            } else if (Loader.isModLoaded(ModIDs.WCT) && this.modID.equals(ModIDs.WCT)) {
                openWCTContainerCraftConfirm(baseContainer, grid, requireToCraftStack, player);
            } else {
                openAEContainerCraftConfirm(baseContainer, grid, requireToCraftStack, player);
            }
        }

    }

    private void openContainerCraftConfirm(IGrid grid, IAEItemStack requireToCraftStack, EntityPlayerMP player,
            BaseActionSource actionSource, Consumer<Future<ICraftingJob>> consumer) {
        Future<ICraftingJob> futureJob = null;

        try {
            final ICraftingGrid cg = grid.getCache(ICraftingGrid.class);

            if (cg instanceof CraftingGridCache cgc) {
                futureJob = cgc.beginCraftingJob(
                        player.worldObj,
                        grid,
                        actionSource,
                        requireToCraftStack,
                        CraftingMode.STANDARD,
                        null);
            } else {
                futureJob = cg.beginCraftingJob(player.worldObj, grid, actionSource, requireToCraftStack, null);
            }

            consumer.accept(futureJob);

            if (player.openContainer instanceof ContainerCraftConfirm ccc) {
                ccc.setItemToCraft(requireToCraftStack);
                ccc.setJob(futureJob);
                ccc.setAutoStart(this.isAutoStart);
                ccc.detectAndSendChanges();
            }

        } catch (final Throwable e) {
            if (futureJob != null) {
                futureJob.cancel(true);
            }
            AELog.debug(e);
        }

    }

    private void openAEContainerCraftConfirm(AEBaseContainer baseContainer, IGrid grid,
            IAEItemStack requireToCraftStack, EntityPlayerMP player) {

        openContainerCraftConfirm(grid, requireToCraftStack, player, baseContainer.getActionSource(), job -> {
            final ContainerOpenContext openContext = baseContainer.getOpenContext();
            Platform.openGUI(player, openContext.getTile(), openContext.getSide(), GuiBridge.GUI_CRAFTING_CONFIRM);
        });

    }

    @Optional.Method(modid = ModIDs.ThE)
    private void openTHContainerCraftConfirm(IGrid grid, IAEItemStack requireToCraftStack, IActionHost host,
            EntityPlayerMP player) {

        openContainerCraftConfirm(
                grid,
                requireToCraftStack,
                player,
                new PlayerSource(player, host),
                job -> {
                    ThEGuiHandler.launchGui(ThEGuiHandler.AUTO_CRAFTING_CONFIRM, player, player.worldObj, 0, 0, 0);
                });

    }

    @Optional.Method(modid = ModIDs.FC)
    private void openFCContainerCraftConfirm(AEBaseContainer baseContainer, IGrid grid,
            IAEItemStack requireToCraftStack, EntityPlayerMP player) {

        openContainerCraftConfirm(grid, requireToCraftStack, player, baseContainer.getActionSource(), job -> {
            final ContainerOpenContext openContext = baseContainer.getOpenContext();
            final TileEntity tileEntity = openContext.getTile();

            if (tileEntity != null) {
                InventoryHandler.openGui(
                        player,
                        player.worldObj,
                        new BlockPos(tileEntity),
                        Objects.requireNonNull(openContext.getSide()),
                        GuiType.FLUID_CRAFTING_CONFIRM);
            } else if (baseContainer.getTarget() instanceof IWirelessTerminal wireless) {
                InventoryHandler.openGui(
                        player,
                        player.worldObj,
                        new BlockPos(wireless.getInventorySlot(), 0, 0),
                        Objects.requireNonNull(openContext.getSide()),
                        GuiType.FLUID_CRAFTING_CONFIRM_ITEM);
            }

        });

    }

    @Optional.Method(modid = ModIDs.WCT)
    private void openWCTContainerCraftConfirm(AEBaseContainer baseContainer, IGrid grid,
            IAEItemStack requireToCraftStack, EntityPlayerMP player) {

        openContainerCraftConfirm(grid, requireToCraftStack, player, baseContainer.getActionSource(), job -> {
            final int x = (int) player.posX;
            final int y = (int) player.posY;
            final int z = (int) player.posZ;

            WCTGuiHandler.launchGui(Reference.GUI_CRAFT_CONFIRM, player, player.worldObj, x, y, z);
        });

    }

}
