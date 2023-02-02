package com.github.vfyjxf.nee.processor;

import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class ImmersiveEngineeringRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "ieArcFurnace",
                        "ieBlastFurnace",
                        "ieBlueprintCrafting",
                        "ieBottlingMachine",
                        "ieCokeOven",
                        "ieCrusher",
                        "ieFermenter",
                        "ieHammerCrushing",
                        "ieMetalPress",
                        "ieRefinery",
                        "ieShaderBag",
                        "ieSqueezer"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "ImmersiveEngineering";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            if (!("ieArcFurnace".equals(identifier) || "ieCrusher".equals(identifier)
                    || "ieBlastFurnace".equals(identifier))) {
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
            if ("ieArcFurnace".equals(identifier) || "ieCrusher".equals(identifier)) {
                recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            }
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
