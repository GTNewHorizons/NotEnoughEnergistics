package com.github.vfyjxf.nee;

import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiInterface;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import codechicken.nei.NEIController;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import com.github.vfyjxf.nee.client.GuiEventHandler;
import com.github.vfyjxf.nee.nei.NEECraftingHandler;
import com.github.vfyjxf.nee.nei.NEECraftingHelper;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.ModIDs;
import cpw.mods.fml.common.Loader;
import java.util.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;
import org.lwjgl.input.Keyboard;
import thaumicenergistics.client.gui.GuiKnowledgeInscriber;
import wanion.avaritiaddons.block.extremeautocrafter.GuiExtremeAutoCrafter;

public class NEINeeConfig implements IConfigureNEI {

    private static final List<Class<?>> transferBlacklist =
            new ArrayList<>(Arrays.asList(GuiInterface.class, GuiPatternTerm.class));

    @Override
    public void loadConfig() {

        RecipeProcessor.init();

        registerKeyBindings();

        registerGuiHandler();

        Set<String> defaultIdentifiers =
                new HashSet<>(Arrays.asList("crafting", "crafting2x2", "brewing", "smelting", "fuel", null));
        Set<String> identifiers = new HashSet<>(defaultIdentifiers);

        RecipeProcessor.recipeProcessors.stream()
                .map(IRecipeProcessor::getAllOverlayIdentifier)
                .forEach(identifiers::addAll);

        for (String ident : identifiers) {
            API.registerGuiOverlay(GuiPatternTerm.class, ident);
            API.registerGuiOverlayHandler(GuiPatternTerm.class, NEECraftingHandler.INSTANCE, ident);
        }

        installCraftingTermSupport();

        installWirelessCraftingTermSupport();

        installPatternTerminalExSupport(identifiers);

        installThaumicEnergisticsSupport();

        installAvaritiaddonsSupport();
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
        API.registerGuiOverlayHandler(GuiCraftingTerm.class, NEECraftingHelper.INSTANCE, "crafting");
        API.registerGuiOverlayHandler(GuiCraftingTerm.class, NEECraftingHelper.INSTANCE, "crafting2x2");
    }

    private void installWirelessCraftingTermSupport() {
        if (Loader.isModLoaded(ModIDs.WCT)) {
            API.registerGuiOverlayHandler(GuiWirelessCraftingTerminal.class, NEECraftingHelper.INSTANCE, "crafting");
            API.registerGuiOverlayHandler(GuiWirelessCraftingTerminal.class, NEECraftingHelper.INSTANCE, "crafting2x2");
        }
    }

    private void installThaumicEnergisticsSupport() {
        try {
            Class.forName("thaumicenergistics.client.gui.GuiKnowledgeInscriber");
        } catch (ClassNotFoundException e) {
            return;
        }
        if (Loader.isModLoaded("thaumcraftneiplugin")) {
            NotEnoughEnergistics.logger.info("Install ThaumicEnergistics support");
            API.registerGuiOverlay(GuiKnowledgeInscriber.class, "arcaneshapedrecipes");
            API.registerGuiOverlay(GuiKnowledgeInscriber.class, "arcaneshapelessrecipes");
            API.registerGuiOverlayHandler(GuiKnowledgeInscriber.class, new NEECraftingHandler(), "arcaneshapedrecipes");
            API.registerGuiOverlayHandler(
                    GuiKnowledgeInscriber.class, new NEECraftingHandler(), "arcaneshapelessrecipes");
        }
    }

    private void installPatternTerminalExSupport(Set<String> identifiers) {
        identifiers.remove("crafting");
        identifiers.remove("crafting2x2");
        // PatternTermEx Support
        for (String ident : identifiers) {
            API.registerGuiOverlay(GuiPatternTermEx.class, ident);
            API.registerGuiOverlayHandler(GuiPatternTermEx.class, new NEECraftingHandler(), ident);
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
            API.registerGuiOverlay(GuiExtremeAutoCrafter.class, "extreme");
            API.registerGuiOverlayHandler(GuiExtremeAutoCrafter.class, new NEECraftingHandler(), "extreme");
        }
    }
}
