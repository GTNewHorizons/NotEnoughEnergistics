package com.github.vfyjxf.nee.processor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.NotEnoughEnergistics;

import WayofTime.alchemicalWizardry.client.nei.NEIAlchemyRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIAltarRecipeHandler;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class BloodMagicRecipeProcessor implements IRecipeProcessor {

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList("altarrecipes", "alchemicalwizardry.alchemy", "alchemicalwizardry.bindingritual"));
    }

    @Nonnull
    @Override
    public String getRecipeProcessorId() {
        return "BloodMagic";
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
            return recipeOutputs;
        }
        return recipeOutputs;
    }

    private static final MethodHandle tierGetterHandle;
    private static final MethodHandle lpAmountGetterHandle;
    private static final MethodHandle consumptionGetterHandle;
    private static final MethodHandle drainGetterHandle;
    private static final MethodHandle lpGetterHandle;
    static {
        MethodHandle tHandle = null;
        MethodHandle lHandle = null;
        MethodHandle cHandle = null;
        MethodHandle dHandle = null;
        MethodHandle lpHandle = null;
        try {
            var targetClass = NEIAltarRecipeHandler.CachedAltarRecipe.class;
            Field field = targetClass.getDeclaredField("tier");
            field.setAccessible(true);
            tHandle = MethodHandles.lookup().unreflectGetter(field);
            field = targetClass.getDeclaredField("lp_amount");
            field.setAccessible(true);
            lHandle = MethodHandles.lookup().unreflectGetter(field);
            field = targetClass.getDeclaredField("consumption");
            field.setAccessible(true);
            cHandle = MethodHandles.lookup().unreflectGetter(field);
            field = targetClass.getDeclaredField("drain");
            field.setAccessible(true);
            dHandle = MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            NotEnoughEnergistics.logger.error("Failed to get MethodHandles for CachedAltarRecipe!", e);
        }
        try {
            var targetClass = NEIAlchemyRecipeHandler.CachedAlchemyRecipe.class;
            Field field = targetClass.getDeclaredField("lp");
            field.setAccessible(true);
            lpHandle = MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            NotEnoughEnergistics.logger.error("Failed to get MethodHandles for CachedAlchemyRecipe!", e);
        }
        tierGetterHandle = tHandle;
        lpAmountGetterHandle = lHandle;
        consumptionGetterHandle = cHandle;
        drainGetterHandle = dHandle;
        lpGetterHandle = lpHandle;
    }

    @Override
    public void appendExtraTags(IRecipeHandler recipe, int recipeIndex, NBTTagCompound extraTags) {
        if (recipe instanceof NEIAltarRecipeHandler altarRecipeHandler) {
            try {
                var currentRecipe = altarRecipeHandler.arecipes.get(recipeIndex);
                if (tierGetterHandle != null) {
                    extraTags.setInteger("tier", (int) tierGetterHandle.invoke(currentRecipe));
                }
                if (lpAmountGetterHandle != null) {
                    extraTags.setInteger("lp_amount", (int) lpAmountGetterHandle.invoke(currentRecipe));
                }
                if (consumptionGetterHandle != null) {
                    extraTags.setInteger("consumption", (int) consumptionGetterHandle.invoke(currentRecipe));
                }
                if (drainGetterHandle != null) {
                    extraTags.setInteger("drain", (int) drainGetterHandle.invoke(currentRecipe));
                }
            } catch (Throwable e) {
                NotEnoughEnergistics.logger.error("Failed to append extra tags for BloodMagic!", e);
            }
        } else if (recipe instanceof NEIAlchemyRecipeHandler alchemyRecipeHandler) {
            try {
                var currentRecipe = alchemyRecipeHandler.arecipes.get(recipeIndex);
                if (lpGetterHandle != null) {
                    extraTags.setInteger("lp", (int) lpGetterHandle.invoke(currentRecipe));
                }
            } catch (Throwable e) {
                NotEnoughEnergistics.logger.error("Failed to append extra tags for BloodMagic!", e);
            }
        }
    }
}
