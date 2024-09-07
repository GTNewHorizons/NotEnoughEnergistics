package com.github.vfyjxf.nee.nei;

import java.util.Map;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.utils.Ingredient;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.Image;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.drawable.DrawableBuilder;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayFormat;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;
import codechicken.nei.recipe.IRecipeHandler;

public class NEEGuiOverlayButton extends GuiOverlayButton {

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
            boolean doCraftingHelp = Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.nopreview"))
                    || Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.preview"));

            if (this.ingredient.isCraftable()
                    && (!this.isCraftingTerm || doCraftingHelp && this.ingredient.requiresToCraft())) {
                Image icon = this.isPresent ? checkIcon : crossIcon;

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

    public NEEGuiOverlayButton(GuiContainer firstGui, IRecipeHandler handler, int recipeIndex, int xPosition,
            int yPosition, int width, int height) {
        super(firstGui, handler, recipeIndex, xPosition, yPosition, width, height);
        if (NEEConfig.noShift) {
            setRequireShiftForOverlayRecipe(false);
        }
    }

    public NEEGuiOverlayButton(GuiOverlayButton button) {
        this(
                button.firstGui,
                button.handler,
                button.recipeIndex,
                button.xPosition,
                button.yPosition,
                button.width,
                button.height);
    }

    @Override
    public Map<String, String> handleHotkeys(GuiContainer gui, int mousex, int mousey, Map<String, String> hotkeys) {
        hotkeys = super.handleHotkeys(gui, mousex, mousey, hotkeys);

        if (ingredientsOverlay().stream().anyMatch(
                btn -> btn instanceof NEEItemOverlayState && showCraftingHotkeys((NEEItemOverlayState) btn))) {
            final String previewKeyName = NEIClientConfig.getKeyName("nee.preview");

            if (previewKeyName != null) {
                hotkeys.put(previewKeyName + " + Click", I18n.format("neenergistics.gui.tooltip.crafting.preview"));
            }

            final String noPreviewKeyName = NEIClientConfig.getKeyName("nee.nopreview");

            if (noPreviewKeyName != null) {
                hotkeys.put(noPreviewKeyName + " + Click", I18n.format("neenergistics.gui.tooltip.crafting.nopreview"));
            }
        }

        return hotkeys;
    }

    private static boolean showCraftingHotkeys(NEEItemOverlayState button) {
        return button.isCraftingTerm() && button.getIngredient().isCraftable()
                && button.getIngredient().requiresToCraft();
    }

}
