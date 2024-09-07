package com.github.vfyjxf.nee.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.me.ItemRepo;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper.UnableToFindFieldException;

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
            for (IAEItemStack stack : getStorageStacks(IAEItemStack::isCraftable)) {
                if (ingredient.getIngredient().contains(stack.getItemStack())) {
                    ingredient.setCraftableIngredient(stack.getItemStack());
                }
            }
        }

        this.calculateIngredients();
    }

    private List<IAEItemStack> getStorageStacks(Predicate<IAEItemStack> predicate) {
        List<IAEItemStack> storageStacks = new ArrayList<>();

        if (termGui != null) {
            ItemRepo repo = getItemRepo(termGui);

            if (repo != null) {
                try {
                    @SuppressWarnings("unchecked")
                    IItemList<IAEItemStack> list = (IItemList<IAEItemStack>) ReflectionHelper
                            .findField(ItemRepo.class, "list").get(repo);

                    for (IAEItemStack stack : list) {
                        if (predicate.test(stack)) {
                            storageStacks.add(stack.copy());
                        }
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return storageStacks;
    }

    private ItemRepo getItemRepo(GuiContainer termGui) {
        Class<?> clazz = termGui.getClass();
        ItemRepo repo = null;

        while (repo == null && clazz != null) {
            try {
                repo = (ItemRepo) ReflectionHelper.findField(clazz, "repo").get(termGui);
            } catch (UnableToFindFieldException | IllegalAccessException e) {
                clazz = clazz.getSuperclass();
            }
        }

        return repo;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
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
        return requireStacks;
    }

    public boolean hasNext() {
        return currentIndex < getRequireStacks().size();
    }

    public void requestNextIngredient(boolean noPreview) {
        IAEItemStack stack = AEItemStack.create(this.getRequiredStack(currentIndex));
        if (stack != null) {
            NEENetworkHandler.getInstance().sendToServer(new PacketCraftingRequest(stack, noPreview));
        }
        currentIndex++;
    }

    public ItemStack getRequiredStack(int index) {
        return this.getRequireStacks().get(index);
    }

    public int getRecipeIndex() {
        return recipeIndex;
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

    @SuppressWarnings("unchecked")
    public void calculateIngredients() {
        List<IAEItemStack> stacks = getStorageStacks(stack -> NEEConfig.matchOtherItems || stack.isCraftable());

        for (Ingredient ingredient : this.ingredients) {
            for (IAEItemStack stack : stacks) {
                if (ingredient.getIngredient().contains(stack.getItemStack()) && stack.getStackSize() > 0) {
                    ingredient.addCount(stack.getStackSize());
                    if (ingredient.requiresToCraft()) {
                        stack.setStackSize(0);
                    } else {
                        stack.setStackSize(stack.getStackSize() - ingredient.getRequireCount());
                    }
                }
            }
        }

        List<ItemStack> inventoryStacks = new ArrayList<>();

        for (Slot slot : (List<Slot>) termGui.inventorySlots.inventorySlots) {
            boolean canGetStack = slot != null && slot.getHasStack()
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
