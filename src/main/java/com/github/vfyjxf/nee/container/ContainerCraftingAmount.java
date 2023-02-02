package com.github.vfyjxf.nee.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.tile.inventory.AppEngInternalInventory;

public class ContainerCraftingAmount extends AEBaseContainer {

    private final Slot resultSlot;
    private ItemStack resultStack;
    private NBTTagCompound recipe;

    public ContainerCraftingAmount(InventoryPlayer ip, Object anchor) {
        super(ip, anchor);
        this.resultSlot = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);
        this.addSlotToContainer(resultSlot);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.verifyPermissions(SecurityPermissions.CRAFT, false);
    }

    public IGrid getGrid() {
        final IActionHost h = ((IActionHost) this.getTarget());
        return h.getActionableNode().getGrid();
    }

    public World getWorld() {
        return this.getPlayerInv().player.worldObj;
    }

    public BaseActionSource getActionSrc() {
        return new PlayerSource(this.getPlayerInv().player, (IActionHost) this.getTarget());
    }

    public NBTTagCompound getRecipe() {
        return recipe;
    }

    public Slot getResultSlot() {
        return resultSlot;
    }

    public ItemStack getResultStack() {
        return resultStack;
    }

    public void setResultStack(ItemStack resultStack) {
        this.resultStack = resultStack;
    }

    public void setRecipe(NBTTagCompound recipe) {
        this.recipe = recipe;
    }
}
