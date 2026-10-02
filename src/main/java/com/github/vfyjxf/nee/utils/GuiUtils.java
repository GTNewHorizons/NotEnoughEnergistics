package com.github.vfyjxf.nee.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import appeng.util.item.AEItemStackType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.me.ItemRepo;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper.UnableToFindFieldException;

/**
 * @author vfyjxf
 */
public class GuiUtils {

    public static boolean isCraftingSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (!isPatternContainer(container)) {
            return false;
        }
        IContainerCraftingPacket cct = (IContainerCraftingPacket) container;
        IInventory craftMatrix = cct.getInventoryByName("crafting");
        return craftMatrix.equals(slot.inventory);
    }

    public static boolean isPatternContainer(Container container) {
        return container instanceof ContainerPatternTerm;
    }

    public static boolean isPatternTerm(GuiScreen guiScreen) {
        return guiScreen instanceof GuiPatternTerm;
    }

    public static ItemRepo getItemRepo(GuiContainer termGui) {
        Class<?> clazz = termGui.getClass();
        ItemRepo repo = null;

        while (repo == null && clazz != null) {
            try {
                repo = (ItemRepo) ReflectionHelper.findField(clazz, "repo").get(termGui);
            } catch (UnableToFindFieldException | IllegalAccessException e) {
                clazz = clazz.getSuperclass();
            }
        }

        return repo;
    }

    public static List<IAEStack<?>> getStorageStacks(GuiContainer termGui, Predicate<? super IAEStack<?>> predicate) {
        final List<IAEStack<?>> storageStacks = new ArrayList<>();

        if (termGui != null) {
            final ItemRepo repo = GuiUtils.getItemRepo(termGui);

            if (repo != null) {

                try {
                    @SuppressWarnings("unchecked")
                    final Iterable<IAEStack<?>> list = (Iterable<IAEStack<?>>) ReflectionHelper
                            .findField(ItemRepo.class, "list").get(repo);

                    for (IAEStack<?> stack : list) {
                        if (predicate.test(stack)) {
                            storageStacks.add(stack.copy());
                        }
                    }

                } catch (Exception ignored) {}
            }
        }

        return storageStacks;
    }

    public static List<IAEItemStack> getStorageItemStacks(GuiContainer termGui, Predicate<IAEItemStack> predicate) {
        final List<IAEItemStack> storageItemStacks = new ArrayList<>();

        for (IAEStack<?> stack : getStorageStacks(termGui, s -> s instanceof IAEItemStack ias && predicate.test(ias))) {
            storageItemStacks.add((IAEItemStack) stack);
        }

        return storageItemStacks;
    }

    public static IGrid getGrid(Container container) {
        if (container instanceof AEBaseContainer baseContainer
                && baseContainer.getTarget() instanceof IGridHost gridHost) {
            final IGridNode gridNode = gridHost.getGridNode(ForgeDirection.UNKNOWN);

            if (gridNode == null) {
                return null;
            }

            return gridNode.getGrid();
        }

        return null;
    }

}
