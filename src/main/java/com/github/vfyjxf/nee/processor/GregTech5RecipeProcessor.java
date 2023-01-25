package com.github.vfyjxf.nee.processor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.glodblock.github.client.gui.GuiFluidPatternTerminalEx;
import gregtech.api.enums.ItemList;
import gregtech.api.util.GT_Recipe;
import gregtech.nei.GT_NEI_DefaultHandler.FixedPositionedStack;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * @author vfyjxf
 */
public class GregTech5RecipeProcessor implements IRecipeProcessor {

    private static final Class<?> gtDefaultClz, gtAssLineClz;

    static {
        Class<?> gtDH = null;
        Class<?> gtAL = null;
        try {
            gtDH = Class.forName("gregtech.nei.GT_NEI_DefaultHandler");
            gtAL = Class.forName("gregtech.nei.GT_NEI_AssLineHandler");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        gtDefaultClz = gtDH;
        gtAssLineClz = gtAL;
    }

    /**
     * For resolving NoSuchMethodError
     * Copied from GTNewHorizons/GT5-Unofficial.
     */
    public static FluidStack getFluidFromDisplayStack(ItemStack aDisplayStack) {
        if (!isStackValid(aDisplayStack)
                || aDisplayStack.getItem() != ItemList.Display_Fluid.getItem()
                || !aDisplayStack.hasTagCompound()) {
            return null;
        }
        Fluid tFluid = FluidRegistry.getFluid(ItemList.Display_Fluid.getItem().getDamage(aDisplayStack));
        return new FluidStack(tFluid, (int) aDisplayStack.getTagCompound().getLong("mFluidDisplayAmount"));
    }

    public static boolean isStackValid(Object aStack) {
        return (aStack instanceof ItemStack)
                && ((ItemStack) aStack).getItem() != null
                && ((ItemStack) aStack).stackSize >= 0;
    }

    @Nonnull
    @Override
    public Set<String> getAllOverlayIdentifier() {

        Set<String> identifiers = new HashSet<>();
        for (GT_Recipe.GT_Recipe_Map tMap : GT_Recipe.GT_Recipe_Map.sMappings) {
            if (tMap.mNEIAllowed) {
                identifiers.add(tMap.mNEIName);
            }
        }
        identifiers.add("gt.recipe.fakeAssemblylineProcess");
        return identifiers;
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
        if (gtDefaultClz.isInstance(recipe) || gtAssLineClz.isInstance(recipe)) {
            if (GuiUtils.isFluidCraftPatternTermEx(Minecraft.getMinecraft().currentScreen)) {
                boolean priority =
                        ((GuiFluidPatternTerminalEx) Minecraft.getMinecraft().currentScreen).container.prioritize;
                if (priority) {
                    for (PositionedStack ps : recipe.getIngredientStacks(recipeIndex)) {
                        if (ps != null && getFluidFromDisplayStack(ps.item) != null) {
                            recipeInputs.add(ps);
                        }
                    }
                    for (PositionedStack ps : recipe.getIngredientStacks(recipeIndex)) {
                        if (ps != null && getFluidFromDisplayStack(ps.item) == null) {
                            recipeInputs.add(ps);
                        }
                    }
                } else {
                    recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
                }
            } else {
                recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
                recipeInputs.removeIf(positionedStack -> getFluidFromDisplayStack(positionedStack.items[0]) != null
                        || positionedStack.item.stackSize == 0);
            }
            if (!recipeInputs.isEmpty()) {
                ItemStack specialItem = recipeInputs.get(recipeInputs.size() - 1).items[0];
                if ((specialItem.isItemEqual(ItemList.Tool_DataStick.get(1))
                        || specialItem.isItemEqual(ItemList.Tool_DataOrb.get(1))
                                && (recipe.getRecipeName().equals("gt.recipe.scanner")
                                        || recipe.getRecipeName().equals("gt.recipe.fakeAssemblylineProcess"))))
                    recipeInputs.remove(recipeInputs.size() - 1);
            }
            return recipeInputs;
        }
        return recipeInputs;
    }

    @Nonnull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (gtDefaultClz.isInstance(recipe) || gtAssLineClz.isInstance(recipe)) {
            recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            recipeOutputs.removeIf(positionedStack -> getFluidFromDisplayStack(positionedStack.items[0]) != null);
            // remove output if it's chance != 10000
            recipeOutputs.removeIf(stack -> stack instanceof FixedPositionedStack
                    && !(((FixedPositionedStack) stack).mChance == 10000
                            || ((FixedPositionedStack) stack).mChance <= 0));
            return recipeOutputs;
        }
        return recipeOutputs;
    }

    @Override
    public boolean mergeStacks(IRecipeHandler recipe, int recipeIndex, String identifier) {
        return !"gt.recipe.fakeAssemblylineProcess".equals(identifier);
    }
}
