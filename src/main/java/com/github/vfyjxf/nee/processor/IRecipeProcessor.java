package com.github.vfyjxf.nee.processor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public interface IRecipeProcessor {

    @Nonnull
    default Set<String> getAllOverlayIdentifier() {
        return Collections.emptySet();
    }

    @Nonnull
    String getRecipeProcessorId();

    @Nonnull
    List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier);

    @Nonnull
    List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier);

    default boolean mergeStacks(IRecipeHandler recipe, int recipeIndex, String identifier) {
        return true;
    }
}
