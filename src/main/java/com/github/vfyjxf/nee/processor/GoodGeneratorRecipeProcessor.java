package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public class GoodGeneratorRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(Arrays.asList("gg.recipe.neutron_activator", "gg.recipe.precise_assembler"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "Good Generator";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            recipeInputs.removeIf(
                    positionedStack -> GregTech5RecipeProcessor.getFluidFromDisplayStack(positionedStack.items[0])
                            != null || positionedStack.item.stackSize == 0);
            return recipeInputs;
        }
        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            recipeOutputs.removeIf(
                    positionedStack -> GregTech5RecipeProcessor.getFluidFromDisplayStack(positionedStack.items[0])
                            != null);
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
