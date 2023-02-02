package com.github.vfyjxf.nee.container;

import static appeng.container.slot.SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotRestrictedInput;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.Platform;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;

public class ContainerPatternInterface extends AEBaseContainer implements IAEAppEngInventory {

    private final InventoryPlayer playerInventory;
    private final AppEngInternalInventory recipeInv;
    private final AppEngInternalInventory patterns;
    private final SlotRestrictedInput[] patternSlots = new SlotRestrictedInput[9];
    private final AppEngSlot[] recipeSlots = new AppEngSlot[10];

    @GuiSync(0)
    private int selectedSlotIndex = -1;

    public ContainerPatternInterface(InventoryPlayer playerInventory, TilePatternInterface tile) {
        super(playerInventory, tile);
        this.playerInventory = playerInventory;
        this.recipeInv = tile.getGirdInventory();
        this.patterns = tile.getPatternInventory();

        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            addSlotToContainer(
                    patternSlots[i] = new SlotRestrictedInput(
                            ENCODED_PATTERN,
                            patterns,
                            i,
                            8 + 18 * i,
                            90 + 5,
                            playerInventory) {

                        @Override
                        public boolean isItemValid(ItemStack i) {
                            return false;
                        }

                        @Override
                        public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
                            return false;
                        }
                    });
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlotToContainer(
                        recipeSlots[x + y * 3] = new SlotDisabled(recipeInv, x + y * 3, 29 + x * 18, 30 + y * 18));
            }
        }

        this.addSlotToContainer(recipeSlots[9] = new SlotDisabled(recipeInv, 9, 126, 48));

        this.bindPlayerInventory(playerInventory, 0, 184 - 69);
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        clearRecipe();
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return isValidContainer();
    }

    public Slot getSelectedSlot() {
        return selectedSlotIndex >= 0 && selectedSlotIndex <= 9 ? patternSlots[selectedSlotIndex] : null;
    }

    public AppEngInternalInventory getRecipeInventory() {
        return recipeInv;
    }

    public int getSelectedSlotIndex() {
        return selectedSlotIndex;
    }

    public void setSelectedSlotIndex(int index) {
        this.selectedSlotIndex = index;

        if (Platform.isServer()) {
            Slot slot = this.getSelectedSlot();
            if (slot instanceof SlotRestrictedInput) {
                if (slot.getHasStack()) {
                    ItemStack maybePattern = slot.getStack();
                    if (maybePattern.getItem() instanceof ICraftingPatternItem) {
                        ICraftingPatternDetails details = ((ICraftingPatternItem) maybePattern.getItem())
                                .getPatternForItem(maybePattern, getTileEntity().getWorldObj());

                        if ((details.getInputs().length == 9) && (details.getOutputs().length > 0)) {
                            InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
                            for (int i = 0; i < details.getInputs().length; i++) {
                                IAEItemStack stack = details.getInputs()[i];
                                Slot currentSlot = getSlotFromInventory(this.getRecipeInventory(), i);
                                if (currentSlot != null) {
                                    if (stack != null) {
                                        ItemStack is = stack.getItemStack();
                                        currentSlot.putStack(is);
                                        ic.setInventorySlotContents(i, is);
                                    } else {
                                        currentSlot.putStack(null);
                                    }
                                }
                            }
                            Slot outSlot = getSlotFromInventory(this.getRecipeInventory(), 9);
                            if (outSlot != null) {
                                outSlot.putStack(details.getOutput(ic, getTileEntity().getWorldObj()));
                            }
                        }
                    }
                }
            }
        }

        this.detectAndSendChanges();
    }

    public void removeCurrentRecipe() {
        Slot patternSlot = this.getSelectedSlot();
        if (patternSlot != null && patternSlot.getHasStack()) {
            patternSlot.putStack(null);
            clearRecipe();
        }
        this.detectAndSendChanges();
    }

    private void clearRecipe() {
        for (Slot slot : this.recipeSlots) {
            slot.putStack(null);
        }
    }

    @Override
    public void saveChanges() {}

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack,
            ItemStack newStack) {}
}
