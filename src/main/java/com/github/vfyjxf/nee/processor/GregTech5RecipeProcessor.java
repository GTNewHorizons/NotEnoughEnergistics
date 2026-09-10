package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.vfyjxf.nee.config.NEEConfig;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.recipe.RecipeCategory;

/**
 * @author vfyjxf
 */
public class GregTech5RecipeProcessor implements IRecipeProcessor {

    private static final Class<?> gtDefaultClz, gtAssLineClz;

    private final boolean isNH;

    static {
        Class<?> gtDH = null;
        Class<?> gtAL = null;
        try {
            gtDH = Class.forName("gregtech.nei.GTNEIDefaultHandler");
            gtAL = Class.forName("gregtech.nei.GT_NEI_AssLineHandler");
        } catch (ClassNotFoundException ignored) {}
        gtDefaultClz = gtDH;
        gtAssLineClz = gtAL;
    }

    public GregTech5RecipeProcessor(boolean isNH) {
        this.isNH = isNH;
    }

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        if (isNH) {
            return RecipeCategory.ALL_RECIPE_CATEGORIES.values().stream()
                    .filter(category -> category.recipeMap.getFrontend().getNEIProperties().registerNEI)
                    .map(category -> category.unlocalizedName).collect(Collectors.toSet());
        }

        try {
            Set<String> identifiers = new HashSet<>();
            Class<?> recipeMapClazz = Class.forName("gregtech.api.util.GT_Recipe$GT_Recipe_Map");
            Collection<?> sMappings = (Collection<?>) recipeMapClazz.getDeclaredField("sMappings").get(null);
            for (Object tMap : sMappings) {
                boolean mNEIAllowed = recipeMapClazz.getDeclaredField("mNEIAllowed").getBoolean(tMap);
                if (mNEIAllowed) {
                    String mNEIName = (String) recipeMapClazz.getDeclaredField("mNEIName").get(tMap);
                    identifiers.add(mNEIName);
                }
            }
            identifiers.add("gt.recipe.fakeAssemblylineProcess");
            return identifiers;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "GregTech5";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (canProcessRecipe(recipe)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            for (PositionedStack positionedStack : recipeInputs) {
                if (NEEConfig.includeNonConsumableIngredients && positionedStack.item.stackSize == 0) {
                    positionedStack.item.stackSize = 1;
                }
            }
            recipeInputs.removeIf(
                    positionedStack -> positionedStack.item.stackSize == 0
                            && !NEEConfig.includeNonConsumableIngredients);
        }

        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (canProcessRecipe(recipe)) {
            recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            return recipeOutputs;
        }
        return recipeOutputs;
    }

    @Override
    public boolean mergeStacks(IRecipeHandler recipe, int recipeIndex, String identifier) {
        return !"gt.recipe.fakeAssemblylineProcess".equals(identifier);
    }

    private boolean canProcessRecipe(IRecipeHandler recipe) {
        return (gtDefaultClz != null && gtDefaultClz.isInstance(recipe))
                || (gtAssLineClz != null && gtAssLineClz.isInstance(recipe));
    }
}
