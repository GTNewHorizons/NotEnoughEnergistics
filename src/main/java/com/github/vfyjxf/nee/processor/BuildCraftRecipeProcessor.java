package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import buildcraft.compat.nei.RecipeHandlerAssemblyTable;
import buildcraft.compat.nei.RecipeHandlerIntegrationTable;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class BuildCraftRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "BuildCraft";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        if (recipe instanceof RecipeHandlerAssemblyTable) {
            return handlerAssemblyTableRecipe((RecipeHandlerAssemblyTable) recipe, recipeIndex, true);
        } else if (recipe instanceof RecipeHandlerIntegrationTable) {
            return handlerIntegrationTableRecipe((RecipeHandlerIntegrationTable) recipe, recipeIndex, true);
        }
        return new ArrayList<>();
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        if (recipe instanceof RecipeHandlerAssemblyTable) {
            return handlerAssemblyTableRecipe((RecipeHandlerAssemblyTable) recipe, recipeIndex, false);
        } else if (recipe instanceof RecipeHandlerIntegrationTable) {
            return handlerIntegrationTableRecipe((RecipeHandlerIntegrationTable) recipe, recipeIndex, false);
        }
        return new ArrayList<>();
    }

    private List<PositionedStack> handlerAssemblyTableRecipe(RecipeHandlerAssemblyTable recipe, int recipeIndex,
            boolean getInput) {
        return getInput ? recipe.getIngredientStacks(recipeIndex)
                : Collections.singletonList(recipe.getResultStack(recipeIndex));
    }

    private List<PositionedStack> handlerIntegrationTableRecipe(RecipeHandlerIntegrationTable recipe, int recipeIndex,
            boolean getInput) {
        return getInput ? recipe.getIngredientStacks(recipeIndex)
                : Collections.singletonList(recipe.getResultStack(recipeIndex));
    }
}
