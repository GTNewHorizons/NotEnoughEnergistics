package com.github.vfyjxf.nee.nei;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.api.IStackStringifyHandler;
import gregapi.data.FL;
import gregapi.item.IItemGT;
import gregapi.item.ItemFluidDisplay;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.util.OM;
import gregapi.util.ST;

public class GT6StackStringifyHandler implements IStackStringifyHandler {

    @Override
    public boolean isFluidDisplayItem(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidDisplay;
    }

    @Override
    public NBTTagCompound convertItemStackToNBT(ItemStack stack, boolean saveStackSize) {
        if (stack == null) {
            return null;
        }
        if (isFluidDisplayItem(stack)) {
            FluidStack fluid = getFluid(stack);
            if (fluid != null && fluid.getFluid() != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("gt6FluidName", fluid.getFluid().getName());
                tag.setInteger("Count", saveStackSize ? fluid.amount : 1000);
                return tag;
            }
        } else if (ST.isGT(stack)) {
            String strId = Item.itemRegistry.getNameForObject(stack.getItem());
            if (strId != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("strId", strId);
                tag.setInteger("Count", saveStackSize ? stack.stackSize : 1);
                tag.setInteger("Damage", ST.meta_(stack));
                if (stack.hasTagCompound() && !stack.getTagCompound().hasNoTags()) {
                    tag.setTag("tag", stack.getTagCompound().copy());
                }
                OreDictItemData tData = OM.anyassociation_(stack);
                if (tData != null) {
                    tag.setString("od", tData.toString());
                }
                return tag;
            }
        }
        return null;
    }

    @Override
    public ItemStack convertNBTToItemStack(NBTTagCompound nbtTag) {
        if (nbtTag == null) {
            return null;
        }
        if (nbtTag.hasKey("gt6FluidName")) {
            String fluidName = nbtTag.getString("gt6FluidName");
            Fluid fluid = FluidRegistry.getFluid(fluidName);
            int amount = nbtTag.getInteger("Count");
            if (fluid != null) {
                return FL.display(new FluidStack(fluid, amount), false, false);
            }
        } else if (nbtTag.hasKey("strId")) {
            String strId = nbtTag.getString("strId");
            Item item = (Item) Item.itemRegistry.getObject(strId);
            if (item instanceof IItemGT) {
                int count = nbtTag.getInteger("Count");
                short damage = (short) nbtTag.getInteger("Damage");
                ItemStack stack = ST.make(item, count, damage);
                if (stack == null && nbtTag.hasKey("od")) {
                    stack = OreDictManager.INSTANCE.getStack(nbtTag.getString("od"), count);
                }
                if (stack != null) {
                    if (nbtTag.hasKey("tag")) {
                        stack.setTagCompound((NBTTagCompound) nbtTag.getCompoundTag("tag").copy());
                    }
                    return OM.get_(stack);
                }
            }
        }
        return null;
    }

    @Override
    public FluidStack getFluid(ItemStack stack) {
        if (stack != null) {
            return FL.getFluid(stack, true);
        }
        return null;
    }
}
