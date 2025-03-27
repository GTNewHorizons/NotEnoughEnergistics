package com.github.vfyjxf.nee.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.github.vfyjxf.nee.utils.IngredientTracker;

import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.container.implementations.ContainerCraftConfirm;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class NEECraftingPreviewHandler {

    public static final NEECraftingPreviewHandler instance = new NEECraftingPreviewHandler();

    private IngredientTracker tracker = null;
    private boolean noPreview = false;

    private NEECraftingPreviewHandler() {}

    public boolean handle(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        this.noPreview = NEIClientConfig.isKeyHashDown("nee.nopreview");
        boolean doCraftingHelp = this.noPreview || NEIClientConfig.isKeyHashDown("nee.preview");

        if (this.noPreview || NEIClientConfig.isKeyHashDown("nee.preview")) {
            this.tracker = new IngredientTracker(firstGui, recipe, recipeIndex);

            if (!this.tracker.getRequireStacks().isEmpty()) {
                this.tracker.requestNextIngredient(this.noPreview);
                return true;
            }
        }

        this.tracker = null;
        return false;
    }

    @SubscribeEvent
    public void onGuiCraftConfirmOpen(GuiOpenEvent event) {
        final GuiScreen old = Minecraft.getMinecraft().currentScreen;

        if (old != null && this.tracker != null
                && old instanceof GuiCraftConfirm guiConfirm
                && isContainerCraftConfirm(guiConfirm.inventorySlots)) {

            if (this.tracker.hasNext()) {
                this.tracker.requestNextIngredient(this.noPreview);
            } else {
                this.tracker = null;
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (this.tracker != null && event.gui instanceof GuiCraftConfirm guiConfirm
                && getCancelButton(guiConfirm) == event.button) {
            this.tracker = null;
        }
    }

    private GuiButton getCancelButton(GuiCraftConfirm gui) {
        return ReflectionHelper.getPrivateValue(GuiCraftConfirm.class, gui, "cancel");
    }

    private boolean isContainerCraftConfirm(Container container) {
        return container instanceof ContainerCraftConfirm;
    }

}
