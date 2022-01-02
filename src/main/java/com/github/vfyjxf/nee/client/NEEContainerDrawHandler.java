package com.github.vfyjxf.nee.client;

import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiPatternTerm;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Please note the difference between the GTNH NEI and the official NEI
 */
public class NEEContainerDrawHandler implements IContainerDrawHandler {

    public static NEEContainerDrawHandler instance = new NEEContainerDrawHandler();

    private Field overlayButtonsField;
    private Method recipesPerPageMethod;
    private boolean drawRequestTooltip;
    private boolean drawMissingTooltip;
    private boolean drawCraftableTooltip;
    private boolean isGtnhNei;

    public NEEContainerDrawHandler() {

    }

    @Override
    public void onPreDraw(GuiContainer gui) {

    }

    @Override
    public void renderObjects(GuiContainer gui, int mouseX, int mouseY) {

    }

    @Override
    public void postRenderObjects(GuiContainer gui, int mouseX, int mouseY) {

    }

    @Override
    public void renderSlotUnderlay(GuiContainer gui, Slot slot) {

    }

    @SuppressWarnings("ALL")
    @Override
    public void renderSlotOverlay(GuiContainer gui, Slot slot) {

        if (NEEConfig.drawHighlight && gui instanceof GuiRecipe) {
            //gtnh nei support
            this.isGtnhNei = true;
            if (this.overlayButtonsField == null || this.recipesPerPageMethod == null) {
                try {
                    this.overlayButtonsField = GuiRecipe.class.getDeclaredField("overlayButtons");
                    this.recipesPerPageMethod = GuiRecipe.class.getDeclaredMethod("getRecipesPerPage");
                    this.overlayButtonsField.setAccessible(true);
                    this.recipesPerPageMethod.setAccessible(true);
                } catch (NoSuchMethodException | NoSuchFieldException e) {
                    this.isGtnhNei = false;
                }
            }

            Minecraft mc = Minecraft.getMinecraft();
            GuiRecipe guiRecipe = (GuiRecipe) gui;
            GuiContainer firstGui = guiRecipe.firstGui;
            boolean isGuiPatternTerm = firstGui instanceof GuiPatternTerm || GuiUtils.isPatternTermExGui(firstGui);
            boolean isCraftingTerm = firstGui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(firstGui);
            if (isGuiPatternTerm || isCraftingTerm) {
                final ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                int i = scaledresolution.getScaledWidth();
                int j = scaledresolution.getScaledHeight();
                final int mouseX = Mouse.getX() * i / mc.displayWidth;
                final int mouseY = j - Mouse.getY() * j / mc.displayHeight - 1;


                List<GuiButton> overlayButtons = null;
                if (isGtnhNei) {
                    try {
                        overlayButtons = new ArrayList<>(Arrays.asList((GuiButton[]) this.overlayButtonsField.get(guiRecipe)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    overlayButtons = new ArrayList<>();
                    GuiButton overlay1 = ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay1");
                    GuiButton overlay2 = ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay2");
                    overlayButtons.add(overlay1);
                    overlayButtons.add(overlay2);
                }

                if (overlayButtons != null) {
                    for (GuiButton button : overlayButtons) {
                        if (button.visible && button.enabled && GuiUtils.isMouseOverButton(button, mouseX, mouseY)) {
                            final int OVERLAY_BUTTON_ID_START = 4;
                            IRecipeHandler handler = guiRecipe.currenthandlers.get(guiRecipe.recipetype);
                            int recipesPerPage = 2;
                            if (isGtnhNei) {
                                try {
                                    recipesPerPage = (int) this.recipesPerPageMethod.invoke(guiRecipe);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (recipesPerPage >= 0 && handler != null) {
                                int recipeIndex = guiRecipe.page * recipesPerPage + button.id - OVERLAY_BUTTON_ID_START;
                                List<PositionedStack> ingredients = handler.getIngredientStacks(recipeIndex);
                                IngredientTracker tracker = new IngredientTracker(guiRecipe.firstGui, handler, recipeIndex);

                                for (int var1 = 0; var1 < ingredients.size(); var1++) {
                                    PositionedStack stack = ingredients.get(var1);
                                    Ingredient ingredient = tracker.getIngredients().get(var1);
                                    Slot stackSlot = guiRecipe.slotcontainer.getSlotWithStack(stack, guiRecipe.getRecipePosition(recipeIndex).x, guiRecipe.getRecipePosition(recipeIndex).y);
                                    if (stackSlot.equals(slot)) {
                                        if (!NEEConfig.enableCraftAmountSettingGui) {
                                            boolean renderAutoAbleItems = (firstGui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(firstGui)) && ingredient.isCraftable() && ingredient.requiresToCraft();
                                            boolean renderCraftableItems = (firstGui instanceof GuiPatternTerm || GuiUtils.isPatternTermExGui(firstGui)) && ingredient.isCraftable();
                                            boolean renderMissingItems = (firstGui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(firstGui)) && !ingredient.isCraftable() && ingredient.requiresToCraft();
                                            if (renderAutoAbleItems || renderCraftableItems) {
                                                Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition,
                                                        slot.xDisplayPosition + 16, slot.yDisplayPosition + 16,
                                                        new Color(0.0f, 0.0f, 1.0f, 0.4f).getRGB());
                                                this.drawCraftableTooltip = renderCraftableItems;
                                                this.drawRequestTooltip = renderAutoAbleItems;
                                            }
                                            if (renderMissingItems) {
                                                Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition,
                                                        slot.xDisplayPosition + 16, slot.yDisplayPosition + 16,
                                                        new Color(1.0f, 0.0f, 0.0f, 0.4f).getRGB());
                                                this.drawMissingTooltip = true;
                                            }
                                        } else {

                                            if (ingredient.isCraftable()) {
                                                Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition,
                                                        slot.xDisplayPosition + 16, slot.yDisplayPosition + 16,
                                                        new Color(0.0f, 0.0f, 1.0f, 0.4f).getRGB());
                                                this.drawRequestTooltip = true;
                                            } else {
                                                Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition,
                                                        slot.xDisplayPosition + 16, slot.yDisplayPosition + 16,
                                                        new Color(1.0f, 0.0f, 0.0f, 0.4f).getRGB());
                                                this.drawMissingTooltip = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (this.drawCraftableTooltip || this.drawRequestTooltip || this.drawMissingTooltip) {
            if (event.gui instanceof GuiRecipe) {
                GuiRecipe guiRecipe = (GuiRecipe) event.gui;
                List<GuiButton> overlayButtons = null;
                if (this.isGtnhNei) {
                    try {
                        overlayButtons = new ArrayList<>(Arrays.asList((GuiButton[]) this.overlayButtonsField.get(guiRecipe)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    overlayButtons = new ArrayList<>();
                    GuiButton overlay1 = ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay1");
                    GuiButton overlay2 = ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay2");
                    overlayButtons.add(overlay1);
                    overlayButtons.add(overlay2);
                }
                if (overlayButtons != null) {
                    for (GuiButton button : overlayButtons) {
                        if (button.visible && button.enabled && GuiUtils.isMouseOverButton(button, event.mouseX, event.mouseY)) {
                            drawCraftingHelperTooltip(guiRecipe, event.mouseX, event.mouseY);
                            this.drawCraftableTooltip = false;
                            this.drawRequestTooltip = false;
                            this.drawMissingTooltip = false;
                        }
                    }
                }
            }
        }
    }

    private void drawCraftingHelperTooltip(GuiRecipe guiRecipe, int mouseX, int mouseY) {
        List<String> tooltips = new ArrayList<>();
        boolean isCraftingTerm = guiRecipe.firstGui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(guiRecipe.firstGui);
        if (isCraftingTerm) {
            if (this.drawRequestTooltip) {
                tooltips.add(String.format("%s" + EnumChatFormatting.GRAY + " + " +
                                EnumChatFormatting.BLUE + I18n.format("neenergistics.gui.tooltip.helper.crafting"),
                        EnumChatFormatting.YELLOW + Keyboard.getKeyName(NEIClientConfig.getKeyBinding("nee.preview"))));
            }
            if (this.drawMissingTooltip) {
                tooltips.add(EnumChatFormatting.RED + I18n.format("neenergistics.gui.tooltip.missing"));
            }
        }
        boolean isPatternTerm = guiRecipe.firstGui instanceof GuiPatternTerm || GuiUtils.isPatternTermExGui(guiRecipe.firstGui);
        if (this.drawCraftableTooltip && isPatternTerm) {
            tooltips.add(EnumChatFormatting.BLUE + I18n.format("neenergistics.gui.tooltip.helper.pattern"));
        }
        //drawHoveringText
        guiRecipe.func_146283_a(tooltips, mouseX, mouseY);
    }
}
