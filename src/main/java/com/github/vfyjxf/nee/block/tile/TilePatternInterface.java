package com.github.vfyjxf.nee.block.tile;

import java.util.*;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.core.settings.TickRates;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.GridAccessException;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.WrapperInvSlot;

import com.google.common.collect.ImmutableSet;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * A ME interface for storing temporary patterns.
 */
public class TilePatternInterface extends AENetworkInvTile
        implements IGridTickable, ICraftingProvider, IInventoryDestination {

    private final int[] sides = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private final AppEngInternalInventory gridInv;
    private final AppEngInternalInventory patterns;
    private final AppEngInternalInventory ejectInv;
    private List<ICraftingPatternDetails> craftingList;
    private List<ItemStack> waitingToSend;
    private final MachineSource source;
    private final WrapperInvSlot slotInv;

    private final boolean[] workStarted = new boolean[9];

    public TilePatternInterface() {
        this.gridInv = new AppEngInternalInventory(this, 9 + 1);
        this.gridInv.setMaxStackSize(1);
        this.patterns = new AppEngInternalInventory(this, 9);
        this.ejectInv = new AppEngInternalInventory(this, 9);
        this.craftingList = new ArrayList<>();
        this.source = new MachineSource(this);
        this.slotInv = new WrapperInvSlot(this.ejectInv);
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(30D);
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged c) {
        this.notifyNeighbors();
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange c) {
        this.notifyNeighbors();
    }

    private void notifyNeighbors() {
        if (this.getProxy().isActive()) {
            try {
                this.getProxy().getGrid()
                        .postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
                this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
            } catch (GridAccessException e) {
                // :P
            }
            Platform.notifyBlocksOfNeighbors(this.getWorldObj(), this.xCoord, this.yCoord, this.zCoord);
        }
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(TickRates.Interface.getMin(), TickRates.Interface.getMax(), !hasItemToSend(), true);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (!this.getProxy().isActive()) {
            return TickRateModulation.SLEEP;
        }

        if (hasItemToSend()) {
            ejectItems();
        }

        if (this.hasItemsToPush()) {
            this.pushItemsOut(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        }

        if (hasWorkFinished()) {
            updateWorks();
        }

        if (hasWorks()) {
            return TickRateModulation.URGENT;
        }

        return TickRateModulation.SLEEP;
    }

    @Nonnull
    @Override
    public IInventory getInternalInventory() {
        return this.ejectInv;
    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removed, ItemStack added) {
        if (inv == this.ejectInv || inv == patterns) {
            try {
                this.getProxy().getTick().alertDevice(this.getProxy().getNode());
            } catch (GridAccessException e) {
                // :P
            }
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updateCraftingList();
    }

    @Override
    public int[] getAccessibleSlotsBySide(ForgeDirection whichSide) {
        return this.sides;
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (this.getProxy().isActive()) {
            if (this.craftingList.isEmpty()) {
                for (int i = 0; i < patterns.getSizeInventory(); i++) {
                    ItemStack stack = patterns.getStackInSlot(i);
                    if (stack != null && stack.getItem() instanceof ICraftingPatternItem) {
                        ICraftingPatternDetails pattern = ((ICraftingPatternItem) stack.getItem())
                                .getPatternForItem(stack, worldObj);
                        if (pattern != null && pattern.isCraftable()) {
                            craftingList.add(pattern);
                        }
                    }
                }
            }
            for (ICraftingPatternDetails details : this.craftingList) {
                craftingTracker.addCraftingOption(this, details);
            }
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (!isBusy() && this.getProxy().isActive()
                && craftingList.contains(patternDetails)
                && !this.hasItemsToPush()) {
            final EnumSet<ForgeDirection> possibleDirections = EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN));
            for (final ForgeDirection direction : possibleDirections) {
                final TileEntity te = worldObj.getTileEntity(
                        xCoord + direction.offsetX,
                        yCoord + direction.offsetY,
                        zCoord + direction.offsetZ);

                if (te instanceof IInterfaceHost) {
                    try {
                        if (this.sameGrid(getGridFromInterface(((IInterfaceHost) te).getInterfaceDuality()))) {
                            continue;
                        }
                    } catch (final GridAccessException e) {
                        continue;
                    }
                }

                if (te instanceof ICraftingMachine) {
                    final ICraftingMachine cm = (ICraftingMachine) te;
                    if (cm.acceptsPlans()) {
                        if (cm.pushPattern(patternDetails, table, direction.getOpposite())) {
                            setWorkStart(patternDetails);
                            return true;
                        }
                        continue;
                    }
                }

                final InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, direction.getOpposite());
                if (ad != null) {

                    if (this.acceptsItems(ad, table)) {
                        for (int x = 0; x < table.getSizeInventory(); x++) {
                            final ItemStack is = table.getStackInSlot(x);
                            if (is != null) {
                                final ItemStack added = ad.addItems(is);
                                this.addToSendList(added);
                            }
                        }
                        this.pushItemsOut(possibleDirections);
                        setWorkStart(patternDetails);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isBusy() {
        return hasItemsToPush();
    }

    @Override
    public void getDrops(final World w, final int x, final int y, final int z, final List<ItemStack> drops) {
        if (hasItemsToPush()) {
            for (ItemStack stack : waitingToSend) {
                if (stack != null) {
                    drops.add(stack);
                }
            }
        }
        for (int i = 0; i < ejectInv.getSizeInventory(); i++) {
            ItemStack stack = ejectInv.getStackInSlot(i);
            if (stack != null) {
                drops.add(stack);
            }
        }
    }

    public AppEngInternalInventory getGirdInventory() {
        return gridInv;
    }

    public AppEngInternalInventory getPatternInventory() {
        return patterns;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.SMART;
    }

    @Override
    public boolean canInsert(ItemStack stack) {

        if (stack == null || stack.getItem() == null) {
            return false;
        }

        try {
            final IAEItemStack out = this.getProxy().getStorage().getItemInventory()
                    .injectItems(AEApi.instance().storage().createItemStack(stack), Actionable.SIMULATE, this.source);
            if (out == null) {
                return true;
            }
            return out.getStackSize() != stack.stackSize;
        } catch (GridAccessException e) {
            return false;
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public NBTTagCompound writeToNBT_TilePatternInterface(NBTTagCompound data) {
        this.patterns.writeToNBT(data, "patterns");
        this.ejectInv.writeToNBT(data, "ejectInv");

        final NBTTagList waitingToSend = new NBTTagList();
        if (this.waitingToSend != null) {
            for (final ItemStack is : this.waitingToSend) {
                final NBTTagCompound item = new NBTTagCompound();
                is.writeToNBT(item);
                waitingToSend.appendTag(item);
            }
        }
        data.setTag("waitingToSend", waitingToSend);
        return data;
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_TilePatternInterface(NBTTagCompound data) {
        this.waitingToSend = null;
        final NBTTagList waitingList = data.getTagList("waitingToSend", 10);
        if (waitingList != null) {
            for (int x = 0; x < waitingList.tagCount(); x++) {
                final NBTTagCompound c = waitingList.getCompoundTagAt(x);
                if (c != null) {
                    final ItemStack is = ItemStack.loadItemStackFromNBT(c);
                    this.addToSendList(is);
                }
            }
        }
        this.patterns.readFromNBT(data, "patterns");
        this.ejectInv.readFromNBT(data, "ejectInv");
        this.updateCraftingList();
    }

    private boolean hasItemsToPush() {
        return this.waitingToSend != null && !this.waitingToSend.isEmpty();
    }

    public boolean canPutPattern(ItemStack result) {
        if (result == null) {
            return false;
        }

        for (int i = 0; i < this.patterns.getSizeInventory(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);

            if (stack != null && stack.getItem() instanceof ICraftingPatternItem) {
                ICraftingPatternDetails pattern = ((ICraftingPatternItem) stack.getItem())
                        .getPatternForItem(stack, worldObj);
                if (result.isItemEqual(pattern.getOutputs()[0].getItemStack())) {
                    return false;
                }
            }
        }

        return IntStream.range(0, this.patterns.getSizeInventory())
                .anyMatch(slot -> patterns.getStackInSlot(slot) == null);
    }

    public int putPattern(ItemStack pattern) {
        for (int i = 0; i < this.patterns.getSizeInventory(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (stack == null) {
                patterns.setInventorySlotContents(i, pattern);
                updateCraftingList();
                return i;
            }
        }
        return -1;
    }

    public void updateCraftingList() {
        final Boolean[] accountedFor = { false, false, false, false, false, false, false, false, false };

        assert (accountedFor.length == this.patterns.getSizeInventory());

        if (!this.getProxy().isReady()) {
            return;
        }

        if (this.craftingList != null) {
            final Iterator<ICraftingPatternDetails> i = this.craftingList.iterator();
            while (i.hasNext()) {
                final ICraftingPatternDetails details = i.next();
                boolean found = false;

                for (int x = 0; x < accountedFor.length; x++) {
                    final ItemStack is = this.patterns.getStackInSlot(x);
                    if (details.getPattern() == is) {
                        accountedFor[x] = found = true;
                    }
                }

                if (!found) {
                    i.remove();
                }
            }
        }

        for (int x = 0; x < accountedFor.length; x++) {
            if (!accountedFor[x]) {
                this.addToCraftingList(this.patterns.getStackInSlot(x));
            }
        }

        try {
            this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
        } catch (final GridAccessException e) {
            // :P
        }
    }

    public void cancelWork(int recipeIndex) {
        IAEItemStack result = getResultStack(recipeIndex);
        if (result != null) {
            workStarted[recipeIndex] = false;
            try {
                final ICraftingGrid cg = this.getProxy().getCrafting();
                final ImmutableSet<ICraftingCPU> cpuSet = cg.getCpus();

                for (ICraftingCPU cpu : cpuSet) {
                    if (cpu instanceof CraftingCPUCluster) {
                        CraftingCPUCluster cluster = (CraftingCPUCluster) cpu;
                        final IItemList<IAEItemStack> pendingList = AEApi.instance().storage().createItemList();
                        cluster.getListOfItem(pendingList, CraftingItemList.PENDING);
                        for (IAEItemStack pendingStack : pendingList) {
                            if (pendingStack.isSameType(result)) {
                                cluster.cancel();
                                break;
                            }
                        }
                    }
                }

            } catch (GridAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void addToCraftingList(final ItemStack is) {
        if (is == null) {
            return;
        }

        if (is.getItem() instanceof ICraftingPatternItem) {
            final ICraftingPatternItem cpi = (ICraftingPatternItem) is.getItem();
            final ICraftingPatternDetails details = cpi.getPatternForItem(is, worldObj);

            if (details != null) {
                if (this.craftingList == null) {
                    this.craftingList = new LinkedList<>();
                }

                this.craftingList.add(details);
            }
        }
    }

    private void addToSendList(final ItemStack is) {
        if (is == null) {
            return;
        }

        if (this.waitingToSend == null) {
            this.waitingToSend = new LinkedList<>();
        }

        this.waitingToSend.add(is);

        try {
            this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
        } catch (final GridAccessException e) {
            // :P
        }
    }

    private boolean acceptsItems(final InventoryAdaptor ad, final InventoryCrafting table) {
        for (int x = 0; x < table.getSizeInventory(); x++) {
            final ItemStack is = table.getStackInSlot(x);
            if (is == null) {
                continue;
            }

            if (ad.simulateAdd(is.copy()) != null) {
                return false;
            }
        }

        return true;
    }

    private void pushItemsOut(final EnumSet<ForgeDirection> possibleDirections) {
        if (!this.hasItemsToPush()) {
            return;
        }

        final Iterator<ItemStack> i = this.waitingToSend.iterator();
        while (i.hasNext()) {
            ItemStack whatToSend = i.next();

            for (final ForgeDirection s : possibleDirections) {
                final TileEntity te = worldObj
                        .getTileEntity(this.xCoord + s.offsetX, this.yCoord + s.offsetY, this.zCoord + s.offsetZ);
                if (te == null) {
                    continue;
                }

                final InventoryAdaptor ad = InventoryAdaptor.getAdaptor(te, s.getOpposite());
                if (ad != null) {
                    final ItemStack Result = ad.addItems(whatToSend);

                    if (Result == null) {
                        whatToSend = null;
                    } else {
                        whatToSend.stackSize -= whatToSend.stackSize - Result.stackSize;
                    }

                    if (whatToSend == null) {
                        break;
                    }
                }
            }

            if (whatToSend == null) {
                i.remove();
            }
        }

        if (this.waitingToSend.isEmpty()) {
            this.waitingToSend = null;
        }
    }

    private boolean hasItemToSend() {
        for (int i = 0; i < ejectInv.getSizeInventory(); i++) {
            ItemStack stack = ejectInv.getStackInSlot(i);
            if (stack != null) {
                return true;
            }
        }
        return false;
    }

    private void ejectItems() {
        for (int i = 0; i < ejectInv.getSizeInventory(); i++) {
            if (ejectInv.getStackInSlot(i) != null) {
                ItemStack stack = ejectInv.getStackInSlot(i);
                int itemCount = stack.stackSize;
                final InventoryAdaptor adaptor = getAdaptor(i);

                final ItemStack canExtract = adaptor.simulateRemove(itemCount, stack, null);

                if (canExtract != null && canExtract.stackSize == itemCount) {

                    try {
                        final IMEMonitor<IAEItemStack> inv = this.getProxy().getStorage().getItemInventory();
                        final IEnergyGrid energy = this.getProxy().getEnergy();
                        final IAEItemStack aeStack = AEApi.instance().storage().createItemStack(stack);
                        if (aeStack != null) {
                            final IAEItemStack failed = Platform.poweredInsert(energy, inv, aeStack, this.source);

                            if (failed != null) {
                                itemCount -= failed.getStackSize();
                            }

                            if (itemCount != 0) {
                                final ItemStack removed = adaptor.removeItems(itemCount, null, null);
                                if (removed == null) {
                                    throw new IllegalStateException(
                                            "bad attempt at managing inventory. ( removeItems )");
                                } else if (removed.stackSize != itemCount) {
                                    throw new IllegalStateException(
                                            "bad attempt at managing inventory. ( removeItems )");
                                }
                            }
                        }
                    } catch (GridAccessException e) {
                        // :P
                    }
                }
            }
        }
    }

    private InventoryAdaptor getAdaptor(final int slot) {
        return new AdaptorIInventory(this.slotInv.getWrapper(slot));
    }

    private boolean hasWorks() {

        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            if (patterns.getStackInSlot(i) != null) {
                return true;
            }
        }

        return false;
    }

    private void setWorkStart(ICraftingPatternDetails pattern) {

        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ICraftingPatternItem) {
                ICraftingPatternDetails currentPattern = ((ICraftingPatternItem) stack.getItem())
                        .getPatternForItem(stack, worldObj);
                if (currentPattern != null && currentPattern.equals(pattern)) {
                    workStarted[i] = true;
                }
            }
        }
    }

    private boolean hasWorkFinished() {

        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            ItemStack stack = patterns.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ICraftingPatternItem) {
                ICraftingPatternDetails currentPattern = ((ICraftingPatternItem) stack.getItem())
                        .getPatternForItem(stack, worldObj);

                if (currentPattern != null) {
                    IAEItemStack what = currentPattern.getOutputs().length > 0 ? currentPattern.getOutputs()[0] : null;
                    if (isNotRequesting(what) && workStarted[i]) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void updateWorks() {

        for (int i = 0; i < patterns.getSizeInventory(); i++) {

            IAEItemStack what = getResultStack(i);
            if (isNotRequesting(what) && workStarted[i]) {
                patterns.setInventorySlotContents(i, null);
                workStarted[i] = false;
                updateCraftingList();
            }
        }
    }

    private boolean isNotRequesting(IAEItemStack stack) {

        if (stack == null) {
            return true;
        }

        try {
            final ICraftingGrid cg = this.getProxy().getCrafting();
            final ImmutableSet<ICraftingCPU> cpuSet = cg.getCpus();

            final IItemList<IAEItemStack> pendingList = AEApi.instance().storage().createItemList();

            for (ICraftingCPU cpu : cpuSet) {
                if (cpu instanceof CraftingCPUCluster) {
                    CraftingCPUCluster cluster = (CraftingCPUCluster) cpu;
                    cluster.getListOfItem(pendingList, CraftingItemList.PENDING);
                }
            }

            for (IAEItemStack pendingStack : pendingList) {
                if (pendingStack.isSameType(stack)) {
                    return false;
                }
            }

        } catch (GridAccessException e) {
            e.printStackTrace();
        }

        return true;
    }

    private IAEItemStack getResultStack(int pattern) {

        ItemStack stack = patterns.getStackInSlot(pattern);
        if (stack != null && stack.getItem() instanceof ICraftingPatternItem) {
            ICraftingPatternDetails currentPattern = ((ICraftingPatternItem) stack.getItem())
                    .getPatternForItem(stack, worldObj);
            if (currentPattern != null) {
                return currentPattern.getOutputs().length > 0 ? currentPattern.getOutputs()[0] : null;
            }
        }

        return null;
    }

    @Override
    public void gridChanged() {
        this.notifyNeighbors();
    }

    private boolean sameGrid(final IGrid grid) throws GridAccessException {
        return grid == this.getProxy().getGrid();
    }

    private IGrid getGridFromInterface(DualityInterface iface) {
        return ReflectionHelper.getPrivateValue(DualityInterface.class, iface, "gridProxy");
    }
}
