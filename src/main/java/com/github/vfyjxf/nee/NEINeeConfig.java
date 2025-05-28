package com.github.vfyjxf.nee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;

import org.lwjgl.input.Keyboard;

import com.github.vfyjxf.nee.client.GuiEventHandler;
import com.github.vfyjxf.nee.nei.NEECraftingTerminalHandler;
import com.github.vfyjxf.nee.nei.NEEExtremeAutoCrafterHandler;
import com.github.vfyjxf.nee.nei.NEEKnowledgeInscriberHandler;
import com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler;
import com.github.vfyjxf.nee.nei.NEETerminalBookmarkContainerHandler;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.client.gui.GuiFluidCraftingWireless;
import com.glodblock.github.client.gui.GuiFluidPatternExWireless;
import com.glodblock.github.client.gui.GuiFluidPatternTerminal;
import com.glodblock.github.client.gui.GuiFluidPatternTerminalEx;
import com.glodblock.github.client.gui.GuiFluidPatternWireless;

import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiInterface;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.implementations.GuiMEPortableCell;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import codechicken.nei.NEIController;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.recipe.DefaultOverlayHandler;
import cpw.mods.fml.common.Loader;
import fox.spiteful.avaritia.gui.GUIExtremeCrafting;
import thaumicenergistics.client.gui.GuiArcaneCraftingTerminal;
import thaumicenergistics.client.gui.GuiKnowledgeInscriber;
import wanion.avaritiaddons.block.extremeautocrafter.GuiExtremeAutoCrafter;

public class NEINeeConfig implements IConfigureNEI {

    private static final List<Class<?>> transferBlacklist = new ArrayList<>(
            Arrays.asList(GuiInterface.class, GuiPatternTerm.class));

    @Override
    public void loadConfig() {

        RecipeProcessor.init();

        registerKeyBindings();

        registerGuiHandler();

        Set<String> defaultIdentifiers = new HashSet<>(
                Arrays.asList("crafting", "crafting2x2", "brewing", "smelting", "fuel", null));
        Set<String> identifiers = new HashSet<>(defaultIdentifiers);

        RecipeProcessor.recipeProcessors.stream().map(IRecipeProcessor::getAllOverlayIdentifier)
                .forEach(identifiers::addAll);

        for (String ident : identifiers) {
            API.registerGuiOverlay(GuiPatternTerm.class, ident);
            API.registerGuiOverlayHandler(GuiPatternTerm.class, NEEPatternTerminalHandler.instance, ident);
        }

        installCraftingTermSupport();

        installWirelessCraftingTermSupport();

        installFluidPatternTerminalSupport(new HashSet<>(identifiers));

        installThaumicEnergisticsSupport();

        installAvaritiaSupport();

        installAvaritiaddonsSupport();

        installPatternTerminalExSupport(new HashSet<>(identifiers));

        installFluidPatternTerminalExSupport(new HashSet<>(identifiers));

        installlBookmarkContainerHandler();
    }

    private void installFluidPatternTerminalExSupport(Set<String> identifiers) {
        if (Loader.isModLoaded(ModIDs.FC)) {
            identifiers.remove("crafting");
            identifiers.remove("crafting2x2");
            for (String ident : identifiers) {
                API.registerGuiOverlay(GuiFluidPatternTerminalEx.class, ident);
                API.registerGuiOverlay(GuiFluidPatternExWireless.class, ident);

                API.registerGuiOverlayHandler(
                        GuiFluidPatternTerminalEx.class,
                        NEEPatternTerminalHandler.instance,
                        ident);
                API.registerGuiOverlayHandler(
                        GuiFluidPatternExWireless.class,
                        NEEPatternTerminalHandler.instance,
                        ident);
            }
        }
    }

    private void installFluidPatternTerminalSupport(Set<String> identifiers) {
        if (Loader.isModLoaded(ModIDs.FC)) {
            for (String ident : identifiers) {
                API.registerGuiOverlay(GuiFluidPatternTerminal.class, ident);
                API.registerGuiOverlay(GuiFluidPatternWireless.class, ident);

                API.registerGuiOverlayHandler(GuiFluidPatternTerminal.class, NEEPatternTerminalHandler.instance, ident);
                API.registerGuiOverlayHandler(GuiFluidPatternWireless.class, NEEPatternTerminalHandler.instance, ident);
            }
        }
    }

    private void installlBookmarkContainerHandler() {
        API.registerBookmarkContainerHandler(GuiPatternTerm.class, NEETerminalBookmarkContainerHandler.instance);
        API.registerBookmarkContainerHandler(GuiPatternTermEx.class, NEETerminalBookmarkContainerHandler.instance);
        API.registerBookmarkContainerHandler(GuiCraftingTerm.class, NEETerminalBookmarkContainerHandler.instance);
        API.registerBookmarkContainerHandler(GuiMEMonitorable.class, NEETerminalBookmarkContainerHandler.instance);
        API.registerBookmarkContainerHandler(GuiMEPortableCell.class, NEETerminalBookmarkContainerHandler.instance);

        if (Loader.isModLoaded(ModIDs.FC)) {
            API.registerBookmarkContainerHandler(
                    GuiFluidPatternWireless.class,
                    NEETerminalBookmarkContainerHandler.instance);
            API.registerBookmarkContainerHandler(
                    GuiFluidPatternTerminal.class,
                    NEETerminalBookmarkContainerHandler.instance);
            API.registerBookmarkContainerHandler(
                    GuiFluidPatternTerminalEx.class,
                    NEETerminalBookmarkContainerHandler.instance);
            API.registerBookmarkContainerHandler(
                    GuiFluidPatternExWireless.class,
                    NEETerminalBookmarkContainerHandler.instance);

            API.registerBookmarkContainerHandler(
                    GuiFluidCraftingWireless.class,
                    NEETerminalBookmarkContainerHandler.instance);
        }

        if (Loader.isModLoaded(ModIDs.WCT)) {
            API.registerBookmarkContainerHandler(
                    GuiWirelessCraftingTerminal.class,
                    NEETerminalBookmarkContainerHandler.instance);
        }

        if (Loader.isModLoaded(ModIDs.ThE)) {
            API.registerBookmarkContainerHandler(
                    GuiArcaneCraftingTerminal.class,
                    NEETerminalBookmarkContainerHandler.instance);
        }

    }

    @Override
    public String getName() {
        return NotEnoughEnergistics.NAME;
    }

    @Override
    public String getVersion() {
        return NotEnoughEnergistics.VERSION;
    }

    private void registerKeyBindings() {
        API.addKeyBind("nee.count", Keyboard.KEY_LCONTROL);
        API.addKeyBind("nee.ingredient", Keyboard.KEY_LSHIFT);
        API.addKeyBind("nee.preview", Keyboard.KEY_LCONTROL);
        API.addKeyBind("nee.nopreview", Keyboard.KEY_LMENU);
    }

    private void registerGuiHandler() {
        API.registerNEIGuiHandler(GuiEventHandler.instance);
        GuiContainerManager.addTooltipHandler(GuiEventHandler.instance);
        // disable MouseScrollTransfer in some gui
        replaceNEIController();
    }

    private void replaceNEIController() {
        int controllerIndex = -1;
        for (IContainerInputHandler inputHandler : GuiContainerManager.inputHandlers) {
            if (inputHandler instanceof NEIController) {
                controllerIndex = GuiContainerManager.inputHandlers.indexOf(inputHandler);
                break;
            }
        }
        if (controllerIndex > 0) {
            GuiContainerManager.inputHandlers.remove(controllerIndex);
            GuiContainerManager.inputHandlers.add(controllerIndex, new NEIController() {

                @Override
                public boolean mouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled) {
                    if (transferBlacklist.contains(gui.getClass())) {
                        return false;
                    }
                    return super.mouseScrolled(gui, mouseX, mouseY, scrolled);
                }
            });
            NotEnoughEnergistics.logger.info("NEIController replaced success");
        }
    }

    private void installCraftingTermSupport() {
        API.registerGuiOverlay(GuiCraftingTerm.class, "crafting");
        API.registerGuiOverlay(GuiCraftingTerm.class, "crafting2x2");
        API.registerGuiOverlayHandler(GuiCraftingTerm.class, NEECraftingTerminalHandler.instance, "crafting");
        API.registerGuiOverlayHandler(GuiCraftingTerm.class, NEECraftingTerminalHandler.instance, "crafting2x2");

        if (Loader.isModLoaded(ModIDs.FC)) {
            API.registerGuiOverlayHandler(
                    GuiFluidCraftingWireless.class,
                    NEECraftingTerminalHandler.instance,
                    "crafting");
            API.registerGuiOverlayHandler(
                    GuiFluidCraftingWireless.class,
                    NEECraftingTerminalHandler.instance,
                    "crafting2x2");
        }
    }

    private void installWirelessCraftingTermSupport() {
        if (Loader.isModLoaded(ModIDs.WCT)) {
            API.registerGuiOverlayHandler(
                    GuiWirelessCraftingTerminal.class,
                    NEECraftingTerminalHandler.instance,
                    "crafting");
            API.registerGuiOverlayHandler(
                    GuiWirelessCraftingTerminal.class,
                    NEECraftingTerminalHandler.instance,
                    "crafting2x2");
        }
    }

    private void installThaumicEnergisticsSupport() {

        if (Loader.isModLoaded(ModIDs.ThE)) {
            NotEnoughEnergistics.logger.info("Install ThaumicEnergistics support");

            try {
                Class.forName("thaumicenergistics.client.gui.GuiKnowledgeInscriber");

                API.registerGuiOverlay(GuiKnowledgeInscriber.class, "arcaneshapedrecipes");
                API.registerGuiOverlay(GuiKnowledgeInscriber.class, "arcaneshapelessSrecipes");
                API.registerGuiOverlayHandler(
                        GuiKnowledgeInscriber.class,
                        NEEKnowledgeInscriberHandler.instance,
                        "arcaneshapedrecipes");
                API.registerGuiOverlayHandler(
                        GuiKnowledgeInscriber.class,
                        NEEKnowledgeInscriberHandler.instance,
                        "arcaneshapelessrecipes");

            } catch (ClassNotFoundException e) {

            }

        }
    }

    private void installPatternTerminalExSupport(Set<String> identifiers) {
        identifiers.remove("crafting");
        identifiers.remove("crafting2x2");
        // PatternTermEx Support
        for (String ident : identifiers) {
            API.registerGuiOverlay(GuiPatternTermEx.class, ident);
            API.registerGuiOverlayHandler(GuiPatternTermEx.class, NEEPatternTerminalHandler.instance, ident);
        }
    }

    private void installAvaritiaddonsSupport() {
        try {
            Class.forName("wanion.avaritiaddons.block.extremeautocrafter.GuiExtremeAutoCrafter");
        } catch (ClassNotFoundException e) {
            return;
        }

        if (Loader.isModLoaded("avaritiaddons")) {
            NotEnoughEnergistics.logger.info("Install Avaritiaddons support");
            API.registerGuiOverlay(GuiExtremeAutoCrafter.class, "extreme", 181, 15);
            API.registerGuiOverlayHandler(
                    GuiExtremeAutoCrafter.class,
                    NEEExtremeAutoCrafterHandler.instance,
                    "extreme");
        }
    }

    private void installAvaritiaSupport() {
        try {
            Class.forName("fox.spiteful.avaritia.gui.GUIExtremeCrafting");
        } catch (ClassNotFoundException e) {
            return;
        }

        if (Loader.isModLoaded("Avaritia")) {
            API.registerGuiOverlay(GUIExtremeCrafting.class, "extreme", 9, 5);
            API.registerGuiOverlayHandler(GUIExtremeCrafting.class, new DefaultOverlayHandler(9, 5), "extreme");
        }
    }
}
