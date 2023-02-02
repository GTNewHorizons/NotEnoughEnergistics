package com.github.vfyjxf.nee.utils;

import net.minecraft.item.ItemStack;

import codechicken.nei.PositionedStack;

public class Ingredient {

    private long requireCount;
    private final long defaultRequireCount;
    private final PositionedStack ingredient;
    private ItemStack craftableIngredient;
    private long currentCount = 0;

    public Ingredient(PositionedStack ingredients) {
        this.ingredient = ingredients;
        this.requireCount = ingredients.items[0].stackSize;
        this.defaultRequireCount = ingredients.items[0].stackSize;
    }

    public PositionedStack getIngredient() {
        return ingredient;
    }

    public ItemStack getCraftableIngredient() {
        return craftableIngredient;
    }

    public void setCraftableIngredient(ItemStack craftableIngredient) {
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
