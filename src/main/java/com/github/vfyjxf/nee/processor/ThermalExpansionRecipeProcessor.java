package com.github.vfyjxf.nee.processor;

import java.lang.reflect.Field;
import java.util.*;

import javax.annotation.Nonnull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerBase;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class ThermalExpansionRecipeProcessor implements IRecipeProcessor {

    private static Class<?> thermalNeiRecipeBaseClass;

    static {
        try {
            thermalNeiRecipeBaseClass = Class
                    .forName("cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerBase$NEIRecipeBase");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "thermalexpansion.charger",
                        "thermalexpansion.crucible",
                        "thermalexpansion.furnace",
                        "thermalexpansion.insolator",
                        "thermalexpansion.pulverizer",
                        "thermalexpansion.sawmill",
                        "thermalexpansion.smelter",
                        "thermalexpansion.transposer"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "ThermalExpansion";
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            if (recipe instanceof RecipeHandlerBase) {
                if (thermalNeiRecipeBaseClass != null) {
                    Field secondaryInputField = ReflectionHelper.findField(thermalNeiRecipeBaseClass, "secondaryInput");
                    PositionedStack secondaryInput = null;
                    try {
                        secondaryInput = (PositionedStack) secondaryInputField
                                .get(((RecipeHandlerBase) recipe).arecipes.get(recipeIndex));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if (secondaryInput != null) {
                        recipeInputs.add(secondaryInput);
                    }
                }
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
            if (recipe instanceof RecipeHandlerBase) {
                if (thermalNeiRecipeBaseClass != null) {
                    Field secondaryOutputField = ReflectionHelper
                            .findField(thermalNeiRecipeBaseClass, "secondaryOutput");
                    Field secondaryOutputChanceField = ReflectionHelper
                            .findField(thermalNeiRecipeBaseClass, "secondaryOutputChance");
                    PositionedStack secondaryOutput = null;
                    int secondaryOutputChance = 0;
                    try {
                        secondaryOutput = (PositionedStack) secondaryOutputField
                                .get(((RecipeHandlerBase) recipe).arecipes.get(recipeIndex));
                        secondaryOutputChance = (int) secondaryOutputChanceField
                                .get(((RecipeHandlerBase) recipe).arecipes.get(recipeIndex));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if (secondaryOutput != null && secondaryOutputChance >= 100) {
                        recipeOutputs.add(secondaryOutput);
                    }
                }
            }
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
