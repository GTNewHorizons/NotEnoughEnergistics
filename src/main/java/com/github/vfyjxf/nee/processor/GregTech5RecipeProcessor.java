package com.github.vfyjxf.nee.processor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.config.NEEConfig;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.enums.ItemList;
import gregtech.api.objects.overclockdescriber.OverclockDescriber;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.nei.GTNEIDefaultHandler;

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

    /**
     * For resolving NoSuchMethodError Copied from GTNewHorizons/GT5-Unofficial.
     */
    public static FluidStack getFluidFromDisplayStack(ItemStack aDisplayStack) {
        if (!isStackValid(aDisplayStack) || aDisplayStack.getItem() != ItemList.Display_Fluid.getItem()
                || !aDisplayStack.hasTagCompound()) {
            return null;
        }
        Fluid tFluid = FluidRegistry.getFluid(ItemList.Display_Fluid.getItem().getDamage(aDisplayStack));
        return new FluidStack(tFluid, (int) aDisplayStack.getTagCompound().getLong("mFluidDisplayAmount"));
    }

    public static boolean isStackValid(Object aStack) {
        return (aStack instanceof ItemStack) && ((ItemStack) aStack).getItem() != null
                && ((ItemStack) aStack).stackSize >= 0;
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

    private static final MethodHandle currentRecipeGetterHandle;
    private static final MethodHandle overclockDescriberGetterHandle;
    private static final MethodHandle identifierGetterHandle;
    static {
        MethodHandle crHandle = null;
        MethodHandle odHandle = null;
        MethodHandle idHandle = null;
        try {
            var targetClass = GTNEIDefaultHandler.class;
            Field field = targetClass.getDeclaredField("currentRecipe");
            field.setAccessible(true);
            crHandle = MethodHandles.lookup().unreflectGetter(field);
            field = targetClass.getDeclaredField("overclockDescriber");
            field.setAccessible(true);
            odHandle = MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            NotEnoughEnergistics.logger.error("Failed to get MethodHandles for GTNEIDefaultHandler!", e);
        }
        try {
            var targetClass = RecipeMetadataKey.class;
            Field field = targetClass.getDeclaredField("identifier");
            field.setAccessible(true);
            idHandle = MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            NotEnoughEnergistics.logger.error("Failed to get MethodHandles for RecipeMetadataKey!", e);
        }
        currentRecipeGetterHandle = crHandle;
        overclockDescriberGetterHandle = odHandle;
        identifierGetterHandle = idHandle;
    }

    @Override
    public void appendExtraTags(IRecipeHandler recipe, int recipeIndex, NBTTagCompound extraTags) {
        if (currentRecipeGetterHandle == null || overclockDescriberGetterHandle == null) return;
        if (recipe instanceof GTNEIDefaultHandler r) {
            try {
                var currentRecipe = (GTNEIDefaultHandler.CachedDefaultRecipe) currentRecipeGetterHandle.invoke(r);
                var overclockDescriber = (OverclockDescriber) overclockDescriberGetterHandle.invoke(r);
                var mRecipe = currentRecipe.mRecipe;
                extraTags.setInteger("EUt", mRecipe.mEUt);
                extraTags.setInteger("Tier", overclockDescriber.getTier());
                var metadataStorage = new NBTTagCompound();
                mRecipe.getMetadataStorage().getEntries().forEach((entry) -> {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    String identifier = "";
                    if (identifierGetterHandle != null) {
                        try {
                            identifier = (String) identifierGetterHandle.invoke(key);
                        } catch (Throwable ignored) {}
                    }
                    if (identifier.isEmpty()) {
                        identifier = String.valueOf(key);
                    }
                    if (value instanceof Integer i) {
                        metadataStorage.setInteger(identifier, i);
                    } else if (value instanceof Boolean b) {
                        metadataStorage.setBoolean(identifier, b);
                    } else if (value instanceof Float f) {
                        metadataStorage.setFloat(identifier, f);
                    } else if (value instanceof Double d) {
                        metadataStorage.setDouble(identifier, d);
                    } else if (value instanceof Long l) {
                        metadataStorage.setLong(identifier, l);
                    } else {
                        metadataStorage.setString(identifier, String.valueOf(value));
                    }
                });
                if (!metadataStorage.hasNoTags()) {
                    extraTags.setTag("metadataStorage", metadataStorage);
                }
            } catch (Throwable e) {
                NotEnoughEnergistics.logger.error("Failed to append extra tags for GT5!", e);
            }
        }
    }

    private boolean canProcessRecipe(IRecipeHandler recipe) {
        return (gtDefaultClz != null && gtDefaultClz.isInstance(recipe))
                || (gtAssLineClz != null && gtAssLineClz.isInstance(recipe));
    }
}
