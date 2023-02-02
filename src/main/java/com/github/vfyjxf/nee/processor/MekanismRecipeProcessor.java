package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class MekanismRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "chemicalinfuser",
                        "chemicalinjectionchamber",
                        "chemicaloxidizer",
                        "chemicalwasher",
                        "combiner",
                        "crusher",
                        "electrolyticseparator",
                        "chamber",
                        "infuser",
                        "compressor",
                        "prc",
                        "precisionsawmill",
                        "purificationchamber",
                        "rotarycondensentrator",
                        "solarneutron",
                        "thermalevaporation"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "Mekanism";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            if (!recipe.getOtherStacks(recipeIndex).isEmpty()) {
                recipeInputs.addAll(recipe.getOtherStacks(recipeIndex));
            }
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
