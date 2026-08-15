package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public class BetterQuestingRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(Arrays.asList("bq_quest"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "betterquesting";
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
        recipeOutput.add(recipe.getOtherStacks(recipeIndex).get(0));
        return recipeOutput;
    }
}
