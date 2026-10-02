package com.github.vfyjxf.nee.utils;

import java.util.ArrayList;
import java.util.List;

import appeng.api.storage.data.IAEStack;
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
    private List<IAEStack<?>> requireStacks;
    private final int recipeIndex;
    private int currentIndex = 0;

    public IngredientTracker(GuiContainer termGui, IRecipeHandler recipe, int recipeIndex) {
        this.termGui = termGui;
        this.recipeIndex = recipeIndex;

        for (PositionedStack requiredIngredient : recipe.getIngredientStacks(recipeIndex)) {
            this.ingredients.add(new Ingredient(requiredIngredient));
        }

        final List<IAEStack<?>> craftableStacks = GuiUtils.getStorageStacks(this.termGui, IAEStack::isCraftable);

        for (Ingredient ingredient : this.ingredients) {
            for (IAEStack<?> stack : craftableStacks) {
                if (ingredient.matches(stack)) {
                    ingredient.setCraftableIngredient(stack);
                }
            }
        }

        this.calculateIngredients();
    }

    public List<Ingredient> getIngredients() {
        return this.ingredients;
    }

    public List<IAEStack<?>> getRequireToCraftStacks() {
        List<IAEStack<?>> requireToCraftStacks = new ArrayList<>();
        for (Ingredient ingredient : this.getIngredients()) {
            if (ingredient.isCraftable() && ingredient.requiresToCraft()) {
                final IAEStack<?> craftableStack = ingredient.getCraftableIngredient();
                final long missingCount = ingredient.getMissingCount();

                IAEStack<?> foundStack = null;
                for (IAEStack<?> stack : requireToCraftStacks) {
                    if (stack.isSameType(craftableStack)) {
                        foundStack = stack;
                        break;
                    }
                }

                if (foundStack != null) {
                    foundStack.incStackSize(missingCount);
                } else {
                    final IAEStack<?> requestStack = craftableStack.copy();
                    requestStack.setStackSize(missingCount);
                    requireToCraftStacks.add(requestStack);
                }
            }
        }
        return requireToCraftStacks;
    }

    public List<IAEStack<?>> getRequireStacks() {
        return this.requireStacks;
    }

    public boolean hasNext() {
        return this.currentIndex < getRequireStacks().size();
    }

    public IAEStack<?> getNextIngredient() {
        return getRequiredStack(this.currentIndex++);
    }

    public IAEStack<?> getRequiredStack(int index) {
        return getRequireStacks().get(index);
    }

    public int getRecipeIndex() {
        return this.recipeIndex;
    }

    public void addAvailableStack(ItemStack stack) {
        for (Ingredient ingredient : this.ingredients) {
            if (ingredient.isItem() && ingredient.requiresToCraft()) {

                final boolean found;
                if (NEEConfig.matchOtherItems) {
                    found = ingredient.getIngredient().contains(stack);
                } else {
                    found = ingredient.getCraftableIngredient() instanceof IAEItemStack ais && ais.isSameType(stack);
                }

                if (stack.stackSize > 0 && found) {
                    final long used = Math.min(stack.stackSize, ingredient.getMissingCount());
                    ingredient.addCount(used);
                    stack.stackSize -= (int) used;
                    break;
                }
            }
        }
    }

    public void calculateIngredients() {
        final List<IAEStack<?>> stacks = GuiUtils.getStorageStacks(
                this.termGui,
                s -> s.getStackSize() > 0
                        && (!(s instanceof IAEItemStack) || NEEConfig.matchOtherItems || s.isCraftable()));

        for (Ingredient ingredient : this.ingredients) {
            ingredient.setCurrentCount(0);
            for (IAEStack<?> stack : stacks) {
                if (!ingredient.requiresToCraft()) {
                    break;
                }

                if (stack.getStackSize() > 0 && ingredient.matches(stack)) {
                    final long used = Math.min(stack.getStackSize(), ingredient.getMissingCount());
                    ingredient.addCount(used);
                    stack.setStackSize(stack.getStackSize() - used);
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
