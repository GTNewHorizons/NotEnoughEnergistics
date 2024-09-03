package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class GTPPRecipeProcessor implements IRecipeProcessor {

    private final boolean isNH;

    public GTPPRecipeProcessor(boolean isNH) {
        this.isNH = isNH;
    }

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        HashSet<String> identifiers = new HashSet<>();
        identifiers.add("GTPP_Decayables");
        if (isNH) {
            return identifiers;
        }

        try {
            Class<?> gtRecipeMapClazz = Class.forName("gregtech.api.util.GTRecipe$GT_Recipe_Map");
            Class<?> gtppRecipeMapClazz = Class.forName("gregtech.api.util.GTPPRecipe$GTPP_Recipe_Map_Internal");
            Collection<?> sMappingsEx = (Collection<?>) gtppRecipeMapClazz.getDeclaredField("sMappingsEx").get(null);
            for (Object gtppMap : sMappingsEx) {
                boolean mNEIAllowed = gtRecipeMapClazz.getDeclaredField("mNEIAllowed").getBoolean(gtppMap);
                if (mNEIAllowed) {
                    String mNEIName = (String) gtRecipeMapClazz.getDeclaredField("mNEIName").get(gtppMap);
                    identifiers.add(mNEIName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
