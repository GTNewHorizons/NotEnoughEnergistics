package com.github.vfyjxf.nee.processor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

}
