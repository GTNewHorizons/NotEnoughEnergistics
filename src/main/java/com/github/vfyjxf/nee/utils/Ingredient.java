package com.github.vfyjxf.nee.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import codechicken.nei.PositionedStack;

public class Ingredient {

    private long requireCount;
    private final long defaultRequireCount;
    private final PositionedStack ingredient;
    private IAEStack<?> craftableIngredient;
    private long currentCount = 0;
    private final List<IAEStack<?>> typedStacks = new ArrayList<>();

    public Ingredient(PositionedStack ingredients) {
        this.ingredient = ingredients;

        for (ItemStack is : ingredients.items) {
            final IAEStack<?> typed = ItemUtils.toTypedStack(is);
            if (typed != null) {
                this.typedStacks.add(typed);
            }
        }

        final long count = isItem() ? ingredients.items[0].stackSize : typedStacks.get(0).getStackSize();
        this.requireCount = count;
        this.defaultRequireCount = count;
    }

    public boolean isItem() {
        return typedStacks.isEmpty();
    }

    public PositionedStack getIngredient() {
        return ingredient;
    }

    public boolean matches(IAEStack<?> stack) {
        if (isItem()) {
            return stack instanceof IAEItemStack ias && ingredient.contains(ias.getItemStack());
        }
        return typedStacks.stream().anyMatch(t -> t.isSameType(stack));
    }

    public IAEStack<?> getCraftableIngredient() {
        return craftableIngredient;
    }

    public void setCraftableIngredient(IAEStack<?> craftableIngredient) {
        this.craftableIngredient = craftableIngredient;
    }

    public long getMissingCount() {
        return requireCount - currentCount;
    }

    public long getDefaultRequireCount() {
        return defaultRequireCount;
    }

    public long getRequireCount() {
        return requireCount;
    }

    public void setRequireCount(long requireCount) {
        this.requireCount = requireCount;
    }

    public void setCurrentCount(long currentCount) {
        this.currentCount = currentCount;
    }

    public boolean isCraftable() {
        return this.craftableIngredient != null;
    }

    public boolean requiresToCraft() {
        return this.getMissingCount() > 0;
    }

    public void addCount(long count) {
        if (currentCount + count < requireCount) {
            currentCount += count;
        } else {
            currentCount = requireCount;
        }
    }
}
