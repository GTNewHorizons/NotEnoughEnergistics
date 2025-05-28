package com.github.vfyjxf.nee.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.github.vfyjxf.nee.config.NEEConfig;

import appeng.api.storage.data.IAEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public class IngredientTracker {

    private final List<Ingredient> ingredients = new ArrayList<>();
    private final GuiContainer termGui;
    private List<ItemStack> requireStacks;
    private final int recipeIndex;
    private int currentIndex = 0;

    public IngredientTracker(GuiContainer termGui, IRecipeHandler recipe, int recipeIndex) {
        this.termGui = termGui;
        this.recipeIndex = recipeIndex;

        for (PositionedStack requiredIngredient : recipe.getIngredientStacks(recipeIndex)) {
            this.ingredients.add(new Ingredient(requiredIngredient));
        }

        for (Ingredient ingredient : this.ingredients) {
            for (IAEItemStack stack : GuiUtils.getStorageStacks(this.termGui, IAEItemStack::isCraftable)) {
                if (ingredient.getIngredient().contains(stack.getItemStack())) {
                    ingredient.setCraftableIngredient(stack.getItemStack());
                }
            }
        }

        this.calculateIngredients();
    }

    public List<Ingredient> getIngredients() {
        return this.ingredients;
    }

    public List<ItemStack> getRequireToCraftStacks() {
        List<ItemStack> requireToCraftStacks = new ArrayList<>();
        for (Ingredient ingredient : this.getIngredients()) {
            boolean find = false;
            if (ingredient.isCraftable() && ingredient.requiresToCraft()) {
                for (ItemStack stack : requireToCraftStacks) {
                    boolean areStackEqual = stack.isItemEqual(ingredient.getCraftableIngredient())
                            && ItemStack.areItemStackTagsEqual(stack, ingredient.getCraftableIngredient());
                    if (areStackEqual) {
                        stack.stackSize = (int) (stack.stackSize + ingredient.getMissingCount());
                        find = true;
                    }
                }

                if (!find) {
                    ItemStack requireStack = ingredient.getCraftableIngredient().copy();
                    requireStack.stackSize = ((int) ingredient.getMissingCount());
                    requireToCraftStacks.add(requireStack);
                }
            }
        }
        return requireToCraftStacks;
    }

    public List<ItemStack> getRequireStacks() {
        return this.requireStacks;
    }

    public boolean hasNext() {
        return this.currentIndex < getRequireStacks().size();
    }

    public ItemStack getNextIngredient() {
        return getRequiredStack(this.currentIndex++);
    }

    public ItemStack getRequiredStack(int index) {
        return getRequireStacks().get(index);
    }

    public int getRecipeIndex() {
        return this.recipeIndex;
    }

    public void addAvailableStack(ItemStack stack) {
        for (Ingredient ingredient : this.ingredients) {
            if (ingredient.requiresToCraft()) {
                if (NEEConfig.matchOtherItems) {
                    if (stack.stackSize > 0 && ingredient.getIngredient().contains(stack)) {
                        int missingCount = (int) ingredient.getMissingCount();
                        ingredient.addCount(stack.stackSize);
                        if (ingredient.requiresToCraft()) {
                            stack.stackSize = 0;
                        } else {
                            stack.stackSize -= missingCount;
                        }
                        break;
                    }
                } else {
                    ItemStack craftableStack = ingredient.getCraftableIngredient();
                    if (craftableStack != null && craftableStack.isItemEqual(stack)
                            && ItemStack.areItemStackTagsEqual(craftableStack, stack)
                            && stack.stackSize > 0) {
                        int missingCount = (int) ingredient.getMissingCount();
                        ingredient.addCount(stack.stackSize);
                        if (ingredient.requiresToCraft()) {
                            stack.stackSize = 0;
                        } else {
                            stack.stackSize -= missingCount;
                        }
                        break;
                    }
                }
            }
        }
    }

    public void calculateIngredients() {
        final List<IAEItemStack> stacks = GuiUtils
                .getStorageStacks(this.termGui, stack -> NEEConfig.matchOtherItems || stack.isCraftable());

        for (Ingredient ingredient : this.ingredients) {
            ingredient.setCurrentCount(0);
            for (IAEItemStack stack : stacks) {
                if (stack.getStackSize() > 0 && ingredient.getIngredient().contains(stack.getItemStack())) {
                    ingredient.addCount(stack.getStackSize());
                    if (ingredient.requiresToCraft()) {
                        stack.setStackSize(0);
                    } else {
                        stack.setStackSize(stack.getStackSize() - ingredient.getRequireCount());
                    }
                }
            }
        }

        final List<ItemStack> inventoryStacks = new ArrayList<>();

        for (Slot slot : (List<Slot>) termGui.inventorySlots.inventorySlots) {
            final boolean canGetStack = slot != null && slot.getHasStack()
                    && slot.getStack().stackSize > 0
                    && slot.isItemValid(slot.getStack())
                    && slot.canTakeStack(Minecraft.getMinecraft().thePlayer);
            if (canGetStack) {
                inventoryStacks.add(slot.getStack().copy());
            }
        }

        for (int i = 0; i < getIngredients().size(); i++) {
            for (ItemStack stack : inventoryStacks) {
                addAvailableStack(stack);
            }
        }

        this.requireStacks = this.getRequireToCraftStacks();
    }
}
