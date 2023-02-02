package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

import com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect;

/**
 * @author vfyjxf
 */
public class TCNEIPluginRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "arcaneshapedrecipes",
                        "arcaneshapelessrecipes",
                        "aspectsRecipe",
                        "cruciblerecipe",
                        "infusionCrafting"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "TCNEIPlugin";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            recipeInputs.removeIf(positionedStack -> positionedStack.item.getItem() instanceof ItemAspect);
            return recipeInputs;
        }
        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutputs.add(recipe.getResultStack(recipeIndex));
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
