package com.github.vfyjxf.nee.processor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerLexicaBotania;

public class BotaniaRecipeProcessor implements IRecipeProcessor {
    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return Collections.singleton(RecipeProcessor.NULL_IDENTIFIER);
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return " Botania";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (recipe instanceof RecipeHandlerLexicaBotania) {
            // we don't need to get recipe from a book.
            // but botania doesn't provide an identifier for each recipe handler, so the button will still show.
            return recipeInputs;
        }
        recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (recipe instanceof RecipeHandlerLexicaBotania) {
            // we don't need to get recipe from a book.
            // but botania doesn't provide an identifier for each recipe handler, so the button will still show.
            return recipeOutputs;
        }
        recipeOutputs.add(recipe.getResultStack(recipeIndex));
        return recipeOutputs;
    }
}
