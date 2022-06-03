package com.github.vfyjxf.nee.processor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import forestry.factory.recipes.nei.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ForestryRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return Collections.singleton(RecipeProcessor.NULL_IDENTIFIER);
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "Forestry";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        if (recipe instanceof NEIHandlerBottler) {

            return bottlerHandler((NEIHandlerBottler) recipe, recipeIndex, true);

        } else if (recipe instanceof NEIHandlerCarpenter) {

            return carpenterHandler((NEIHandlerCarpenter) recipe, recipeIndex, true);

        } else if (recipe instanceof NEIHandlerCentrifuge) {

            return centrifugeHandler((NEIHandlerCentrifuge) recipe, recipeIndex, true);

        } else if (recipe instanceof NEIHandlerFabricator) {

            return fabricatorHandler((NEIHandlerFabricator) recipe, recipeIndex, true);

        } else if (recipe instanceof NEIHandlerMoistener) {

            return moistenerHandler((NEIHandlerMoistener) recipe, recipeIndex, true);

        } else if (recipe instanceof NEIHandlerSqueezer) {

            return squeezerHandler((NEIHandlerSqueezer) recipe, recipeIndex, true);

        }
        return new ArrayList<>();
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        if (recipe instanceof NEIHandlerBottler) {

            return bottlerHandler((NEIHandlerBottler) recipe, recipeIndex, false);

        } else if (recipe instanceof NEIHandlerCarpenter) {

            return carpenterHandler((NEIHandlerCarpenter) recipe, recipeIndex, false);

        } else if (recipe instanceof NEIHandlerCentrifuge) {

            return centrifugeHandler((NEIHandlerCentrifuge) recipe, recipeIndex, false);

        } else if (recipe instanceof NEIHandlerFabricator) {

            return fabricatorHandler((NEIHandlerFabricator) recipe, recipeIndex, false);

        } else if (recipe instanceof NEIHandlerMoistener) {

            return moistenerHandler((NEIHandlerMoistener) recipe, recipeIndex, false);

        } else if (recipe instanceof NEIHandlerSqueezer) {

            return squeezerHandler((NEIHandlerSqueezer) recipe, recipeIndex, false);

        }
        return new ArrayList<>();
    }

    private List<PositionedStack> bottlerHandler(NEIHandlerBottler base, int recipeIndex, boolean getInput) {
        return getInput ? base.getIngredientStacks(recipeIndex) : Collections.singletonList(base.getResultStack(recipeIndex));
    }

    private List<PositionedStack> carpenterHandler(NEIHandlerCarpenter base, int recipeIndex, boolean getInput) {
        return getInput ? base.getIngredientStacks(recipeIndex) : Collections.singletonList(base.getResultStack(recipeIndex));
    }

    private List<PositionedStack> centrifugeHandler(NEIHandlerCentrifuge base, int recipeIndex, boolean getInput) {
        return getInput ? base.getIngredientStacks(recipeIndex) : base.getOtherStacks(recipeIndex);
    }

    private List<PositionedStack> fabricatorHandler(NEIHandlerFabricator base, int recipeIndex, boolean getInput) {
        List<PositionedStack> recipeInput = new ArrayList<>(base.getIngredientStacks(recipeIndex));
        recipeInput.addAll(base.getOtherStacks(recipeIndex));
        return getInput ? recipeInput : Collections.singletonList(base.getResultStack(recipeIndex));
    }

    //Fermenter doesn't support, because it doesn't have an item output

    private List<PositionedStack> moistenerHandler(NEIHandlerMoistener base, int recipeIndex, boolean getInput) {
        List<PositionedStack> recipeInput = new ArrayList<>(base.getIngredientStacks(recipeIndex));
        recipeInput.addAll(base.getOtherStacks(recipeIndex));
        return getInput ? recipeInput : Collections.singletonList(base.getResultStack(recipeIndex));
    }

    private List<PositionedStack> squeezerHandler(NEIHandlerSqueezer base, int recipeIndex, boolean getInput) {
        return getInput ? base.getIngredientStacks(recipeIndex) : Collections.singletonList(base.getResultStack(recipeIndex));
    }
    //Still doesn't need support

}
