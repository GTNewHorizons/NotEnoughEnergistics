package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import crazypants.enderio.nei.SagMillRecipeHandler;

public class EnderIORecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "EnderIOAlloySmelter",
                        "EIOEnchanter",
                        "EnderIOSagMill",
                        "EnderIOSliceAndSplice",
                        "EnderIOSoulBinder",
                        "EnderIOVat"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "EnderIO";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            return recipeInputs;
        }
        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (identifier != null) {
            if (this.getAllOverlayIdentifier().contains(identifier)) {
                recipeOutputs.add(recipe.getResultStack(recipeIndex));
                recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
                // remove output if it's chance != 1
                if (recipe instanceof SagMillRecipeHandler) {
                    SagMillRecipeHandler.MillRecipe millRecipe = (SagMillRecipeHandler.MillRecipe) ((SagMillRecipeHandler) recipe).arecipes
                            .get(recipeIndex);
                    recipeOutputs
                            .removeIf(positionedStack -> millRecipe.getChanceForOutput(positionedStack.item) != 1.0F);
                }
                return recipeOutputs;
            }
        }

        return recipeOutputs;
    }
}
