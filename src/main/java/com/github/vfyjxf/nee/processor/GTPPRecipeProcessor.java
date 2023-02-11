package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.util.GTPP_Recipe;

/**
 * @author vfyjxf
 */
public class GTPPRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        HashSet<String> identifiers = new HashSet<>(Collections.singletonList("GTPP_Decayables"));

        // GTNH version of GT++ deprecated this, but other versions still use it
        // noinspection deprecation
        for (GTPP_Recipe.GTPP_Recipe_Map_Internal gtppMap : GTPP_Recipe.GTPP_Recipe_Map_Internal.sMappingsEx) {
            if (gtppMap.mNEIAllowed) {
                identifiers.add(gtppMap.mNEIName);
            }
        }

        return identifiers;
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "GT++";
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
