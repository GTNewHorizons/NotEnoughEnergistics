package com.github.vfyjxf.nee.utils;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import com.github.vfyjxf.nee.config.NEEConfig;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;

import java.util.ArrayList;
import java.util.List;

public class IngredientTracker {

    private final List<Ingredient> ingredients = new ArrayList<>();
    private int craftingAmount = 1;
    private final GuiContainer termGui;
    private List<ItemStack> requireStacks;

    /**
     * For NEECraftingHelper
     */
    public IngredientTracker(GuiContainer termGui, IRecipeHandler recipe, int recipeIndex) {
        this.termGui = termGui;

        for (PositionedStack requiredIngredient : recipe.getIngredientStacks(recipeIndex)) {
            Ingredient ingredient = new Ingredient(requiredIngredient);
            ingredients.add(ingredient);
        }

        for (Ingredient ingredient : this.ingredients) {
            for (IAEItemStack stack : getCraftableStacks()) {
                if (ingredient.getIngredient().contains(stack.getItemStack())) {
                    ingredient.setCraftableIngredient(stack.getItemStack());
                }
            }
        }

        this.calculateIngredients();
    }


    @SuppressWarnings("unchecked")
    private List<IAEItemStack> getCraftableStacks() {
        List<IAEItemStack> craftableStacks = new ArrayList<>();
        if (termGui != null) {
            IItemList<IAEItemStack> list = null;
            try {
                if (!GuiUtils.isGuiWirelessCrafting(termGui)) {
                    ItemRepo repo = (ItemRepo) ReflectionHelper.findField(GuiMEMonitorable.class, "repo").get(termGui);
                    list = (IItemList<IAEItemStack>) ReflectionHelper.findField(ItemRepo.class, "list").get(repo);
                } else {
                    //wireless crafting terminal support
                    net.p455w0rd.wirelesscraftingterminal.client.me.ItemRepo repo = (net.p455w0rd.wirelesscraftingterminal.client.me.ItemRepo) ReflectionHelper.findField(GuiWirelessCraftingTerminal.class, "repo").get(termGui);
                    list = (IItemList<IAEItemStack>) ReflectionHelper.findField(net.p455w0rd.wirelesscraftingterminal.client.me.ItemRepo.class,
                            "list").get(repo);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (list != null) {
                for (IAEItemStack stack : list) {
                    if (stack.isCraftable()) {
                        craftableStacks.add(stack.copy());
                    }
                }
            }
        }
        return craftableStacks;
    }

    @SuppressWarnings("unchecked")
    private List<IAEItemStack> getStorageStacks() {
        List<IAEItemStack> list = new ArrayList<>();
        if (termGui != null) {
            try {
                if (!GuiUtils.isGuiWirelessCrafting(termGui)) {
                    ItemRepo repo = (ItemRepo) ReflectionHelper.findField(GuiMEMonitorable.class, "repo").get(termGui);
                    for (IAEItemStack stack : (IItemList<IAEItemStack>) ReflectionHelper.findField(ItemRepo.class, "list").get(repo)) {
                        list.add(stack.copy());
                    }
                } else {
                    //wireless crafting terminal support
                    net.p455w0rd.wirelesscraftingterminal.client.me.ItemRepo repo = (net.p455w0rd.wirelesscraftingterminal.client.me.ItemRepo) ReflectionHelper.findField(GuiWirelessCraftingTerminal.class, "repo").get(termGui);
                    for (IAEItemStack stack : (IItemList<IAEItemStack>) ReflectionHelper.findField(net.p455w0rd.wirelesscraftingterminal.client.me.ItemRepo.class,
                            "list").get(repo)) {
                        list.add(stack.copy());
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<ItemStack> getRequireToCraftStacks() {
        List<ItemStack> requireToCraftStacks = new ArrayList<>();
        if (NEEConfig.enableCraftAmountSettingGui) {
            for (Ingredient ingredient : this.getIngredients()) {
                boolean find = false;
                if (ingredient.isCraftable()) {
                    for (ItemStack stack : requireToCraftStacks) {
                        boolean areStackEqual = stack.isItemEqual(ingredient.getCraftableIngredient()) && ItemStack.areItemStackTagsEqual(stack, ingredient.getCraftableIngredient());
                        if (areStackEqual) {
                            stack.stackSize += ingredient.getRequireCount();
                            find = true;
                            break;
                        }

                    }

                    if (!find) {
                        ItemStack requireStack = ingredient.getCraftableIngredient().copy();
                        requireStack.stackSize = ((int) ingredient.getRequireCount());
                        requireToCraftStacks.add(requireStack);
                    }
                }
            }
        } else {
            for (Ingredient ingredient : this.getIngredients()) {
                boolean find = false;
                if (ingredient.isCraftable() && ingredient.requiresToCraft()) {
                    for (ItemStack stack : requireToCraftStacks) {
                        boolean areStackEqual = stack.isItemEqual(ingredient.getCraftableIngredient()) && ItemStack.areItemStackTagsEqual(stack, ingredient.getCraftableIngredient());
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
        }
        return requireToCraftStacks;
    }

    public List<ItemStack> getRequireStacks() {
        return requireStacks;
    }

    public ItemStack getRequiredStack(int index) {
        return this.getRequireStacks().get(index);
    }

    public void setCraftingAmount(int craftingAmount) {
        this.craftingAmount = craftingAmount;
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
                    if (craftableStack != null && craftableStack.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(craftableStack, stack) && stack.stackSize > 0) {
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

        //set requireCount and reset currentCount
        for (Ingredient ingredient : this.getIngredients()) {
            ingredient.setRequireCount(ingredient.getDefaultRequireCount() * craftingAmount);
            ingredient.setCurrentCount(0);
        }

        List<IAEItemStack> stacks = NEEConfig.matchOtherItems ? getStorageStacks() : getCraftableStacks();
        for (Ingredient ingredient : this.ingredients) {
            for (IAEItemStack stack : stacks) {
                if (ingredient.getIngredient().contains(stack.getItemStack())) {
                    if (stack.getStackSize() > 0) {
                        ingredient.addCount(stack.getStackSize());
                        if (ingredient.requiresToCraft()) {
                            stack.setStackSize(0);
                        } else {
                            stack.setStackSize(stack.getStackSize() - ingredient.getRequireCount());
                        }
                    }
                }
            }
        }

        List<ItemStack> inventoryStacks = new ArrayList<>();

        for (Slot slot : (List<Slot>) termGui.inventorySlots.inventorySlots) {
            boolean canGetStack = slot != null &&
                    slot.getHasStack() &&
                    slot.getStack().stackSize > 0 &&
                    slot.isItemValid(slot.getStack()) &&
                    slot.canTakeStack(Minecraft.getMinecraft().thePlayer);
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
