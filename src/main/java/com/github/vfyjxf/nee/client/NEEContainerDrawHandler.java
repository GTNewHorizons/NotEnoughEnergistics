package com.github.vfyjxf.nee.client;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Keyboard;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.nei.NEECraftingHandler;
import com.github.vfyjxf.nee.nei.NEECraftingHelper;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Please note the difference between the GTNH NEI and the official NEI
 */
public class NEEContainerDrawHandler {

    public static NEEContainerDrawHandler instance = new NEEContainerDrawHandler();

    private static final Color craftableColor = new Color(0.0f, 0.0f, 1.0f, 0.4f);
    private static final Color missingColor = new Color(1.0f, 0.0f, 0.0f, 0.4f);

    public boolean isGtnhNei;
    private final List<GuiButton> overlayButtons = new ArrayList<>();
    private final Map<Integer, IngredientTracker> trackerMap = new HashMap<>();
    private boolean isCraftingTerm;
    private boolean isPatternTerm;
    private int oldPage;
    private int oldType;

    public Field overlayButtonsField;
    public Method recipesPerPageMethod;
    private boolean drawRequestTooltip;
    private boolean drawMissingTooltip;
    private boolean drawCraftableTooltip;

    public NEEContainerDrawHandler() {
        // gtnh nei support
        isGtnhNei = true;
        try {
            this.overlayButtonsField = GuiRecipe.class.getDeclaredField("overlayButtons");
            this.recipesPerPageMethod = GuiRecipe.class.getDeclaredMethod("getRecipesPerPage");
            this.overlayButtonsField.setAccessible(true);
            this.recipesPerPageMethod.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            isGtnhNei = false;
        }
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (NEEConfig.drawHighlight && event.gui instanceof GuiRecipe) {
            GuiRecipe guiRecipe = (GuiRecipe) event.gui;
            this.isCraftingTerm = GuiUtils.isGuiCraftingTerm(guiRecipe.firstGui);
            this.isPatternTerm = GuiUtils.isPatternTerm(guiRecipe.firstGui);

            if (this.isCraftingTerm || this.isPatternTerm) {
                this.oldPage = guiRecipe.page;
                this.oldType = guiRecipe.recipetype;
                setOverlayButton(guiRecipe);
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (NEEConfig.drawHighlight && event.gui instanceof GuiRecipe) {
            if (this.isCraftingTerm || this.isPatternTerm) {
                GuiRecipe gui = (GuiRecipe) event.gui;

                IRecipeHandler currentHandler = (IRecipeHandler) gui.currenthandlers.get(gui.recipetype);

                if (NEECraftingHandler.isCraftingTableRecipe(currentHandler)) {

                    if (this.trackerMap.isEmpty()) {
                        initIngredientTracker(gui);
                    }

                    if (this.oldPage != gui.page) {
                        this.oldPage = gui.page;
                        setOverlayButton(gui);
                        initIngredientTracker(gui);
                    }

                    if (this.oldType != gui.recipetype) {
                        this.oldType = gui.recipetype;
                        setOverlayButton(gui);
                        initIngredientTracker(gui);
                    }

                    for (GuiButton button : overlayButtons) {
                        if (isMouseOverButton(button, event.mouseX, event.mouseY)) {
                            if (!trackerMap.isEmpty()) {
                                IngredientTracker tracker = trackerMap.get(button.id);
                                if (tracker != null) {
                                    Point offset = gui.getRecipePosition(tracker.getRecipeIndex());
                                    for (int i = 0; i < tracker.getIngredients().size(); i++) {
                                        Ingredient ingredient = tracker.getIngredients().get(i);
                                        Slot slot = gui.slotcontainer
                                                .getSlotWithStack(ingredient.getIngredient(), offset.x, offset.y);
                                        if (slot != null) {
                                            if (this.isCraftingTerm) {

                                                if (ingredient.isCraftable() && ingredient.requiresToCraft()) {
                                                    GuiDraw.drawRect(
                                                            slot.xDisplayPosition + gui.guiLeft,
                                                            slot.yDisplayPosition + gui.guiTop,
                                                            16,
                                                            16,
                                                            craftableColor.getRGB());
                                                    this.drawRequestTooltip = true;
                                                }

                                                if (ingredient.requiresToCraft() && !ingredient.isCraftable()) {
                                                    GuiDraw.drawRect(
                                                            slot.xDisplayPosition + gui.guiLeft,
                                                            slot.yDisplayPosition + gui.guiTop,
                                                            16,
                                                            16,
                                                            missingColor.getRGB());
                                                    this.drawMissingTooltip = true;
                                                }

                                            } else if (this.isPatternTerm) {
                                                if (ingredient.isCraftable()) {
                                                    GuiDraw.drawRect(
                                                            slot.xDisplayPosition + gui.guiLeft,
                                                            slot.yDisplayPosition + gui.guiTop,
                                                            16,
                                                            16,
                                                            craftableColor.getRGB());
                                                    this.drawCraftableTooltip = true;
                                                }
                                            }
                                        }
                                    }

                                    drawCraftingHelperTooltip((GuiRecipe) event.gui, event.mouseX, event.mouseY);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setOverlayButton(GuiRecipe guiRecipe) {
        // get all overlay buttons.
        overlayButtons.clear();
        trackerMap.clear();
        List<GuiButton> overlayButtonList = null;

        if (isGtnhNei) {
            try {
                overlayButtonList = new ArrayList<>(
                        Arrays.asList(
                                (GuiButton[]) NEEContainerDrawHandler.instance.overlayButtonsField.get(guiRecipe)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            overlayButtonList = new ArrayList<>();
            overlayButtonList.add(ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay1"));
            overlayButtonList.add(ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay2"));
        }

        if (overlayButtonList != null) {
            overlayButtons.addAll(overlayButtonList);
        }
    }

    private void initIngredientTracker(GuiRecipe gui) {
        for (int i = 0; i < overlayButtons.size(); i++) {
            GuiButton overlayButton = overlayButtons.get(i);
            if (overlayButton.visible) {
                IRecipeHandler handler = (IRecipeHandler) gui.currenthandlers.get(gui.recipetype);
                int recipeIndex = -1;
                if (isGtnhNei) {
                    try {
                        int OVERLAY_BUTTON_ID_START = 4;
                        recipeIndex = gui.page * (int) recipesPerPageMethod.invoke(gui) + overlayButton.id
                                - OVERLAY_BUTTON_ID_START;
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } else {
                    recipeIndex = gui.page * handler.recipiesPerPage() + i;
                }

                if (recipeIndex >= 0 && handler != null && recipeIndex < handler.numRecipes()) {
                    IngredientTracker tracker = this.isCraftingTerm
                            ? new IngredientTracker(gui.firstGui, handler, recipeIndex)
                            : new IngredientTracker(gui, handler, recipeIndex);
                    trackerMap.put(overlayButton.id, tracker);
                }
            }
        }
    }

    private void drawCraftingHelperTooltip(GuiRecipe guiRecipe, int mouseX, int mouseY) {
        List<String> tooltips = new ArrayList<>();
        if (this.isCraftingTerm) {
            if (this.drawRequestTooltip) {
                if (NEECraftingHelper.isIsPatternInterfaceExists()) {
                    tooltips.add(
                            String.format(
                                    "%s" + EnumChatFormatting.GRAY
                                            + " + "
                                            + EnumChatFormatting.BLUE
                                            + I18n.format("neenergistics.gui.tooltip.helper.crafting.text2"),
                                    EnumChatFormatting.YELLOW
                                            + Keyboard.getKeyName(NEIClientConfig.getKeyBinding("nee.preview"))));
                } else {
                    tooltips.add(
                            String.format(
                                    "%s" + EnumChatFormatting.GRAY
                                            + " + "
                                            + EnumChatFormatting.BLUE
                                            + I18n.format("neenergistics.gui.tooltip.helper.crafting.text1"),
                                    EnumChatFormatting.YELLOW
                                            + Keyboard.getKeyName(NEIClientConfig.getKeyBinding("nee.preview"))));
                }
            }
            if (this.drawMissingTooltip) {
                tooltips.add(EnumChatFormatting.RED + I18n.format("neenergistics.gui.tooltip.missing"));
            }
        }
        if (this.drawCraftableTooltip && this.isPatternTerm) {
            tooltips.add(EnumChatFormatting.BLUE + I18n.format("neenergistics.gui.tooltip.helper.pattern"));
        }
        // drawHoveringText
        guiRecipe.func_146283_a(tooltips, mouseX, mouseY);
        this.drawCraftableTooltip = false;
        this.drawRequestTooltip = false;
        this.drawMissingTooltip = false;
    }

    private boolean isMouseOverButton(GuiButton button, int mouseX, int mouseY) {
        return mouseX >= button.xPosition && mouseY >= button.yPosition
                && mouseX < button.xPosition + button.width
                && mouseY < button.yPosition + button.height;
    }
}
