package com.github.vfyjxf.nee.processor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.github.vfyjxf.nee.config.NEEConfig;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import gregapi.NEI_RecipeMap;
import gregapi.NEI_RecipeMap.FixedPositionedStack;
import gregapi.recipes.Recipe;

/**
 * @author vfyjxf
 */
public class GregTech6RecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        Set<String> identifiers = new HashSet<>();
        for (Recipe.RecipeMap tMap : Recipe.RecipeMap.RECIPE_MAPS.values()) {
            if (tMap.mNEIAllowed) {
                identifiers.add(tMap.mNameNEI);
            }
        }
        return identifiers;
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "GregTech6";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            for (PositionedStack positionedStack : recipeInputs) {
                if (NEEConfig.includeNonConsumableIngredients && positionedStack.item.stackSize == 0) {
                    positionedStack.item.stackSize = 1;
                }
            }
            recipeInputs.removeIf(
                    positionedStack -> positionedStack.item.stackSize == 0
                            && !NEEConfig.includeNonConsumableIngredients);
            if (recipe instanceof NEI_RecipeMap) {
                Field mRecipeMapField = ReflectionHelper.findField(NEI_RecipeMap.class, "mRecipeMap");
                Recipe.RecipeMap mRecipeMap = null;
                try {
                    mRecipeMap = (Recipe.RecipeMap) mRecipeMapField.get(recipe);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                final List<ItemStack> machineList = mRecipeMap != null ? mRecipeMap.mRecipeMachineList : null;
                if (machineList != null && !machineList.isEmpty()) {
                    recipeInputs.removeIf(positionedStack -> {
                        if (positionedStack == null || positionedStack.items == null) return false;
                        for (ItemStack item : positionedStack.items) {
                            if (item == null) continue;
                            for (ItemStack machine : machineList) {
                                if (machine == null) continue;
                                if (item.isItemEqual(machine) && ItemStack.areItemStackTagsEqual(item, machine)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
                }
            }
            return recipeInputs;
        }
        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutput = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutput.addAll(recipe.getOtherStacks(recipeIndex));
            recipeOutput.removeIf(positionedStack -> positionedStack.item.stackSize == 0);
            recipeOutput.removeIf(
                    positionedStack -> positionedStack instanceof FixedPositionedStack
                            && ((FixedPositionedStack) positionedStack).mChance > 0
                            && ((FixedPositionedStack) positionedStack).mChance
                                    != ((FixedPositionedStack) positionedStack).mMaxChance);
            return recipeOutput;
        }
        return recipeOutput;
    }
}
