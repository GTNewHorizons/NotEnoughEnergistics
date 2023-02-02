package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public class AvaritiaRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(Arrays.asList("extreme", "extreme_compression"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "Avaritia";
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
        List<PositionedStack> recipeOutput = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutput.add(recipe.getResultStack(recipeIndex));
            return recipeOutput;
        }
        return recipeOutput;
    }
}
