package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class ICRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "blastfurnace",
                        "BlockCutter",
                        "centrifuge",
                        "compressor",
                        "extractor",
                        "fluidcanner",
                        "macerator",
                        "metalformer",
                        "oreWashing",
                        "solidcanner"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "IC2";
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
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutputs.add(recipe.getResultStack(recipeIndex));
            recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
