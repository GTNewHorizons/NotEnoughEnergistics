package com.github.vfyjxf.nee.network.packet;

import static com.github.vfyjxf.nee.nei.NEECraftingHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.nei.NEECraftingHandler.OUTPUT_KEY;
import static com.github.vfyjxf.nee.nei.NEECraftingHelper.RECIPE_LENGTH;

import java.io.IOException;
import java.util.concurrent.Future;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.helpers.WirelessTerminalGuiObject;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.apache.commons.lang3.tuple.Pair;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.core.AELog;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.github.vfyjxf.nee.container.ContainerCraftingAmount;
import com.github.vfyjxf.nee.container.ContainerCraftingConfirm;
import com.github.vfyjxf.nee.container.WCTContainerCraftingConfirm;
import com.github.vfyjxf.nee.network.NEEGuiHandler;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.google.common.base.Optional;

import cpw.mods.fml.common.Loader;
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

            if (GuiUtils.isWirelessCraftingTermContainer(container)) {
                message.handlerWirelessCraftingRequest((ContainerWirelessCraftingTerminal) container, message, player);
            } else if (container instanceof AEBaseContainer) {
                AEBaseContainer baseContainer = (AEBaseContainer) container;
                Object target = baseContainer.getTarget();
                if (target instanceof IGridHost) {
                    final IGridHost gh = (IGridHost) target;
                    final IGridNode gn = gh.getGridNode(ForgeDirection.UNKNOWN);
                    if (gn != null) {
                        final IGrid grid = gn.getGrid();
                        if (grid != null) {
                            final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
                            if (security != null && security.hasPermission(player, SecurityPermissions.CRAFT)) {
                                if (container instanceof ContainerCraftingTerm) {
                                    message.handlerCraftingTermRequest(
                                            (ContainerCraftingTerm) container,
                                            message,
                                            grid,
                                            player);
                                }

                                if (container instanceof ContainerCraftingAmount) {
                                    message.handlerCraftingAmountRequest(
                                            (ContainerCraftingAmount) container,
                                            message,
                                            grid,
                                            player);
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    private void handlerCraftingTermRequest(ContainerCraftingTerm container, PacketCraftingRequest message, IGrid grid,
            EntityPlayerMP player) {
        if (message.getRequireToCraftStack() != null) {
            Future<ICraftingJob> futureJob = null;
            try {
                final ICraftingGrid cg = grid.getCache(ICraftingGrid.class);
                futureJob = cg.beginCraftingJob(
                        player.worldObj,
                        grid,
                        container.getActionSource(),
                        message.getRequireToCraftStack(),
                        null);

                final ContainerOpenContext context = container.getOpenContext();
                if (context != null) {
                    final TileEntity te = context.getTile();
                    Platform.openGUI(player, te, container.getOpenContext().getSide(), GuiBridge.GUI_CRAFTING_CONFIRM);
                    if (player.openContainer instanceof ContainerCraftConfirm) {
                        final ContainerCraftConfirm ccc = (ContainerCraftConfirm) player.openContainer;
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

    private void handlerCraftingAmountRequest(ContainerCraftingAmount container, PacketCraftingRequest message,
            IGrid grid, EntityPlayerMP player) {
        if (container.getResultStack() != null) {

            Pair<TilePatternInterface, Integer> pair = setRecipe(grid, container.getRecipe(), player);
            if (pair != null) {
                final IStorageGrid inv = grid.getCache(IStorageGrid.class);
                final IMEMonitor<IAEItemStack> storage = inv.getItemInventory();
                final IItemList<IAEItemStack> all = storage.getStorageList();
                ItemStack resultStack = container.getResultStack().copy();
                IAEItemStack result = null;
                /*
                 * For some reason,the output from jei is different from the actual one, so apply ItemStack.isItemEqual
                 * to match.
                 */
                for (IAEItemStack aeStack : all) {
                    if (resultStack.isItemEqual(aeStack.getItemStack()) && aeStack.isCraftable()) {
                        result = aeStack.copy();
                        break;
                    }
                }

                if (result != null) {
                    result.setStackSize(message.getCraftAmount());
                    Future<ICraftingJob> futureJob = null;
                    try {
                        final ICraftingGrid cg = grid.getCache(ICraftingGrid.class);
                        futureJob = cg
                                .beginCraftingJob(player.worldObj, grid, container.getActionSource(), result, null);

                        final ContainerOpenContext context = container.getOpenContext();
                        if (context != null) {
                            final TileEntity te = context.getTile();
                            NEEGuiHandler.openGui(player, NEEGuiHandler.CRAFTING_CONFIRM_ID, te, context.getSide());
                            if (player.openContainer instanceof ContainerCraftingConfirm) {
                                final ContainerCraftingConfirm ccc = (ContainerCraftingConfirm) player.openContainer;
                                ccc.setAutoStart(message.isAutoStart());
                                ccc.setJob(futureJob);
                                ccc.setTile(pair.getLeft());
                                ccc.setPatternIndex(pair.getRight());
                                ccc.detectAndSendChanges();
                            }
                        } else if (Loader.isModLoaded(ModIDs.WCT)) {

                            NEEGuiHandler.openGui(player, NEEGuiHandler.CRAFTING_CONFIRM_WIRELESS_ID, player.worldObj);

                            if (player.openContainer instanceof WCTContainerCraftingConfirm) {
                                final WCTContainerCraftingConfirm ccc = (WCTContainerCraftingConfirm) player.openContainer;
                                ccc.setJob(futureJob);
                                ccc.setAutoStart(message.isAutoStart());
                                ccc.setTile(pair.getLeft());
                                ccc.setPatternIndex(pair.getRight());
                                ccc.detectAndSendChanges();
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
    }

    private void handlerWirelessCraftingRequest(ContainerWirelessCraftingTerminal container,
            PacketCraftingRequest message, EntityPlayerMP player) {
        Object target = container.getTarget();
        if (target instanceof WirelessTerminalGuiObject) {
            IGrid grid = ((WirelessTerminalGuiObject) target).getTargetGrid();
            if (grid != null) {
                final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
                if (security != null && security.hasPermission(player, SecurityPermissions.CRAFT)
                        && message.getRequireToCraftStack() != null) {
                    Future<ICraftingJob> futureJob = null;
                    try {
                        final ICraftingGrid cg = grid.getCache(ICraftingGrid.class);
                        futureJob = cg.beginCraftingJob(
                                player.worldObj,
                                grid,
                                container.getActionSource(),
                                message.getRequireToCraftStack(),
                                null);

                        int x = (int) player.posX;
                        int y = (int) player.posY;
                        int z = (int) player.posZ;

                        WCTGuiHandler.launchGui(Reference.GUI_CRAFT_CONFIRM, player, player.worldObj, x, y, z);

                        if (player.openContainer instanceof net.p455w0rd.wirelesscraftingterminal.common.container.ContainerCraftConfirm) {
                            final net.p455w0rd.wirelesscraftingterminal.common.container.ContainerCraftConfirm ccc = (net.p455w0rd.wirelesscraftingterminal.common.container.ContainerCraftConfirm) player.openContainer;
                            ccc.setJob(futureJob);
                            ccc.setAutoStart(message.isAutoStart());
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
    }

    private Pair<TilePatternInterface, Integer> setRecipe(IGrid grid, NBTTagCompound recipe, EntityPlayer player) {

        for (IGridNode gridNode : grid.getMachines(TilePatternInterface.class)) {

            if (gridNode.getMachine() instanceof TilePatternInterface) {

                TilePatternInterface tpi = (TilePatternInterface) gridNode.getMachine();
                NBTTagCompound currentTag = recipe.getCompoundTag(OUTPUT_KEY);
                ItemStack result = currentTag == null ? null : ItemStack.loadItemStackFromNBT(currentTag);

                if (tpi.getProxy().isActive() && tpi.canPutPattern(result)) {

                    ItemStack patternStack = getPatternStack(player, recipe);

                    if (patternStack != null) {

                        int patternIndex = tpi.putPattern(patternStack);
                        if (patternIndex >= 0) {
                            return Pair.of(tpi, patternIndex);
                        }
                    }
                }
            }
        }
        return null;
    }

    private ItemStack getPatternStack(EntityPlayer player, NBTTagCompound recipe) {
        ItemStack[] recipeInputs = new ItemStack[RECIPE_LENGTH];
        NBTTagCompound currentStack;
        for (int i = 0; i < recipeInputs.length; i++) {
            currentStack = recipe.getCompoundTag(INPUT_KEY + i);
            recipeInputs[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }
        InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int i = 0; i < RECIPE_LENGTH; i++) {
            ic.setInventorySlotContents(i, recipeInputs[i]);
        }

        ItemStack result = CraftingManager.getInstance().findMatchingRecipe(ic, player.worldObj);
        if (result != null) {
            ItemStack patternStack = null;
            Optional<ItemStack> maybePattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1);
            if (maybePattern.isPresent()) {
                patternStack = maybePattern.get();
            }
            if (patternStack != null) {
                final NBTTagCompound patternValue = new NBTTagCompound();
                final NBTTagList tagIn = new NBTTagList();
                for (ItemStack stack : recipeInputs) {
                    tagIn.appendTag(crateItemTag(stack));
                }
                patternValue.setTag("in", tagIn);
                patternValue.setTag("out", result.writeToNBT(new NBTTagCompound()));
                patternValue.setBoolean("crafting", true);
                patternValue.setBoolean("substitute", false);

                patternStack.setTagCompound(patternValue);
                return patternStack;
            }
        }

        return null;
    }

    private NBTTagCompound crateItemTag(ItemStack itemStack) {
        NBTTagCompound tag = new NBTTagCompound();
        if (itemStack != null) {
            itemStack.writeToNBT(tag);
        }
        return tag;
    }
}
