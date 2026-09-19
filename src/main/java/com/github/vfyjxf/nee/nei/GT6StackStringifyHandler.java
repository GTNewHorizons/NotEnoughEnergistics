package com.github.vfyjxf.nee.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.api.IStackStringifyHandler;
import gregapi.data.FL;
import gregapi.item.ItemFluidDisplay;

public class GT6StackStringifyHandler implements IStackStringifyHandler {

    @Override
    public boolean isFluidDisplayItem(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidDisplay;
    }

    @Override
    public NBTTagCompound convertItemStackToNBT(ItemStack stack, boolean saveStackSize) {
        if (stack != null && stack.getItem() instanceof ItemFluidDisplay) {
            FluidStack fluid = getFluid(stack);
            if (fluid != null && fluid.getFluid() != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("gt6FluidName", fluid.getFluid().getName());
                tag.setInteger("Count", saveStackSize ? fluid.amount : 1000);
                return tag;
            }
        }
        return null;
    }

    @Override
    public ItemStack convertNBTToItemStack(NBTTagCompound nbtTag) {
        if (nbtTag != null && nbtTag.hasKey("gt6FluidName")) {
            String fluidName = nbtTag.getString("gt6FluidName");
            Fluid fluid = FluidRegistry.getFluid(fluidName);
            int amount = nbtTag.getInteger("Count");
            if (fluid != null) {
                return FL.display(new FluidStack(fluid, amount), false, false);
            }
        }
        return null;
    }

    @Override
    public FluidStack getFluid(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemFluidDisplay) {
            return FL.getFluid(stack, true);
        }
        return null;
    }

    @Override
    public ItemStack normalizeRecipeQueryStack(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemFluidDisplay) {
            FluidStack fluid = getFluid(stack);
            if (fluid != null) {
                return codechicken.nei.item.ItemFluidDisplay.createStack(fluid);
            }
        }
        return null;
    }
}
