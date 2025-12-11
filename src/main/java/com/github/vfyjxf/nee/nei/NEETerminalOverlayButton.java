package com.github.vfyjxf.nee.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.lwjgl.opengl.GL11;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.utils.Ingredient;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.Image;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.drawable.DrawableBuilder;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.RecipeHandlerRef;

public class NEETerminalOverlayButton extends GuiOverlayButton {

    public static class NEEItemOverlayState extends ItemOverlayState {

        private static final Image crossIcon = new DrawableBuilder(
                "neenergistics:textures/gui/states.png",
                16,
                36,
                8,
                8).build();

        private static final Image checkIcon = new DrawableBuilder(
                "neenergistics:textures/gui/states.png",
                24,
                36,
                8,
                8).build();

        protected Ingredient ingredient;
        protected boolean isCraftingTerm = true;

        public NEEItemOverlayState(Ingredient ingredient, boolean isCraftingTerm) {
            super(ingredient.getIngredient(), !ingredient.requiresToCraft());
            this.ingredient = ingredient;
            this.isCraftingTerm = isCraftingTerm;
        }

        public Ingredient getIngredient() {
            return this.ingredient;
        }

        public boolean isCraftingTerm() {
            return this.isCraftingTerm;
        }

        public void setIsCraftingTerm(boolean isCraftingTerm) {
            this.isCraftingTerm = isCraftingTerm;
        }

        public void draw(ItemOverlayFormat format) {
            final boolean doCraftingHelp = NEIClientConfig.isKeyHashDown("nee.nopreview")
                    || NEIClientConfig.isKeyHashDown("nee.preview");

            if (this.ingredient.isCraftable()
                    && (!this.isCraftingTerm || doCraftingHelp && this.ingredient.requiresToCraft())) {
                final Image icon = this.isPresent ? checkIcon : crossIcon;

                if (format == ItemOverlayFormat.BACKGROUND) {
                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GuiDraw.drawRect(this.slot.relx, this.slot.rely, 16, 16, 0x660000AA);
                    GL11.glPopAttrib();
                } else {
                    LayoutManager.drawIcon(this.slot.relx + 16 - icon.width, this.slot.rely + 16 - icon.height, icon);
                }

            } else {
                super.draw(format);
            }
        }
    }

    public NEETerminalOverlayButton(GuiContainer firstGui, RecipeHandlerRef handlerRef, int xPosition, int yPosition) {
        super(firstGui, handlerRef, xPosition, yPosition);
        if (NEEConfig.noShift) {
            setRequireShiftForOverlayRecipe(false);
        }
    }

    public NEETerminalOverlayButton(GuiOverlayButton button) {
        this(button.firstGui, button.handlerRef, button.xPosition, button.yPosition);
    }

    @Override
    public void mouseReleased(int mousex, int mousey) {
        if (this.firstGui != null && !NEECraftingPreviewHandler.instance
                .handle(this.firstGui, this.handlerRef.handler, this.handlerRef.recipeIndex)) {
            super.mouseReleased(mousex, mousey);
        }
    }

    private static boolean showCraftingHotkeys(NEEItemOverlayState button) {
        return button.getIngredient().isCraftable() || !button.getIngredient().requiresToCraft();
    }

    public static void updateRecipeButtons(GuiRecipe<?> guiRecipe, List<GuiRecipeButton> buttonList) {
        for (int i = 0; i < buttonList.size(); i++) {
            if (buttonList.get(i) instanceof GuiOverlayButton btn) {
                buttonList.set(i, new NEETerminalOverlayButton(btn));
            }
        }
    }

}
