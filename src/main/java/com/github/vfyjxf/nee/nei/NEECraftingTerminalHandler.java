package com.github.vfyjxf.nee.nei;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Keyboard;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.nei.NEEGuiOverlayButton.NEEItemOverlayState;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import com.github.vfyjxf.nee.utils.ModIDs;

import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.core.AELog;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketNEIRecipe;
import appeng.util.Platform;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;

public class NEECraftingTerminalHandler implements IOverlayHandler {

    public static final NEECraftingTerminalHandler instance = new NEECraftingTerminalHandler();
    public static final int RECIPE_LENGTH = 9;

    private IngredientTracker tracker = null;
    private boolean noPreview = false;

    private NEECraftingTerminalHandler() {}

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        this.noPreview = Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.nopreview"));
        boolean doCraftingHelp = this.noPreview || Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.preview"));
        this.tracker = null;

        if (!doCraftingHelp) {
            moveItems(firstGui, recipe, recipeIndex);
        } else {
            this.tracker = new IngredientTracker(firstGui, recipe, recipeIndex);

            if (!this.tracker.getRequireStacks().isEmpty()) {
                this.tracker.requestNextIngredient(this.noPreview);
            } else {
                moveItems(firstGui, recipe, recipeIndex);
            }
        }
    }

    /**
     * Copied from GTNewHorizons/Applied-Energistics-2-Unofficial
     */
    private void moveItems(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        try {
            final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
            if (firstGui instanceof GuiCraftingTerm) {
                PacketNEIRecipe packet = new PacketNEIRecipe(packIngredients(firstGui, ingredients, false));
                // don't use gtnh ae2's method;
                int packetSize = getPacketSize(packet);
                if (packetSize >= 32 * 1024) {
                    AELog.warn(
                            "Recipe for " + recipe.getRecipeName()
                                    + " has too many variants, reduced version will be used");
                    packet = new PacketNEIRecipe(packIngredients(firstGui, ingredients, true));
                }
                if (packetSize >= 0) {
                    NetworkHandler.instance.sendToServer(packet);
                } else {
                    NotEnoughEnergistics.logger.error("Can't get packet size!");
                }
            } else if (GuiUtils.isGuiWirelessCrafting(firstGui)) {
                moveItemsForWirelessCrafting(firstGui, ingredients);
            }
        } catch (final Exception | Error ignored) {}
    }

    /**
     * Copied from GTNewHorizons/Applied-Energistics-2-Unofficial
     */
    private boolean testSize(final NBTTagCompound recipe) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream outputStream = new DataOutputStream(bytes);
        CompressedStreamTools.writeCompressed(recipe, outputStream);
        return bytes.size() > 3 * 1024;
    }

    @Override
    public List<ItemOverlayState> presenceOverlay(GuiContainer firstGui, IRecipeHandler handler, int recipeIndex) {
        final IngredientTracker tracker = new IngredientTracker(firstGui, handler, recipeIndex);
        final List<ItemOverlayState> itemPresenceSlots = new ArrayList<>();

        for (Ingredient ingredient : tracker.getIngredients()) {
            itemPresenceSlots.add(new NEEItemOverlayState(ingredient, true));
        }

        return itemPresenceSlots;
    }

    /**
     * Copied from GTNewHorizons/Applied-Energistics-2-Unofficial
     */
    @SuppressWarnings("unchecked")
    private NBTTagCompound packIngredients(GuiContainer gui, List<PositionedStack> ingredients, boolean limited)
            throws IOException {
        final NBTTagCompound recipe = new NBTTagCompound();
        for (final PositionedStack positionedStack : ingredients) {
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                final int col = (positionedStack.relx - 25) / 18;
                final int row = (positionedStack.rely - 6) / 18;
                for (final Slot slot : (List<Slot>) gui.inventorySlots.inventorySlots) {
                    if (isCraftingMatrixSlot(gui, slot) && slot.getSlotIndex() == col + row * 3) {
                        final NBTTagList tags = new NBTTagList();
                        final List<ItemStack> list = new LinkedList<>();

                        // prefer pure crystals.
                        for (int x = 0; x < positionedStack.items.length; x++) {
                            if (Platform.isRecipePrioritized(positionedStack.items[x])) {
                                list.add(0, positionedStack.items[x]);
                            } else {
                                list.add(positionedStack.items[x]);
                            }
                        }

                        for (final ItemStack is : list) {
                            final NBTTagCompound tag = new NBTTagCompound();
                            is.writeToNBT(tag);
                            tags.appendTag(tag);
                            if (limited) {
                                final NBTTagCompound test = new NBTTagCompound();
                                test.setTag("#" + slot.getSlotIndex(), tags);
                                if (testSize(test)) {
                                    break;
                                }
                            }
                        }

                        recipe.setTag("#" + slot.getSlotIndex(), tags);
                        break;
                    }
                }
            }
        }
        return recipe;
    }

    private boolean isCraftingMatrixSlot(GuiContainer gui, Slot slot) {// todo change
        if (gui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(gui)) {
            return slot instanceof SlotCraftingMatrix || slot instanceof SlotFakeCraftingMatrix;
        }
        return false;
    }

    private int getPacketSize(AppEngPacket packet) {
        try {
            ByteBuf p = (ByteBuf) ReflectionHelper.findField(AppEngPacket.class, "p").get(packet);
            return p.array().length;
        } catch (IllegalAccessException e) {
            return -1;
        }
    }

    @Optional.Method(modid = ModIDs.WCT)
    private void moveItemsForWirelessCrafting(GuiContainer firstGui, List<PositionedStack> ingredients) {
        try {
            net.p455w0rd.wirelesscraftingterminal.core.sync.network.NetworkHandler.instance.sendToServer(
                    new net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketNEIRecipe(
                            packIngredients(firstGui, ingredients, false)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onActionPerformedEventPre(GuiOverlayButton.UpdateOverlayButtonsEvent.Post event) {
        if (event.gui instanceof GuiRecipe && isGuiCraftingTerm((GuiRecipe<?>) event.gui)) {
            for (int i = 0; i < event.buttonList.size(); i++) {
                event.buttonList.set(i, new NEEGuiOverlayButton(event.buttonList.get(i)));
            }
        }
    }

    @SubscribeEvent
    public void onGuiCraftConfirmOpen(GuiOpenEvent event) {
        final GuiScreen old = Minecraft.getMinecraft().currentScreen;

        if (old != null && this.tracker != null
                && old instanceof GuiCraftConfirm
                && isContainerCraftConfirm(((GuiContainer) old).inventorySlots)) {

            if (this.tracker.hasNext()) {
                this.tracker.requestNextIngredient(this.noPreview);
            } else {
                this.tracker = null;
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (this.tracker != null && event.gui instanceof GuiCraftConfirm
                && getCancelButton((GuiCraftConfirm) event.gui) == event.button) {
            this.tracker = null;
        }
    }

    private boolean isContainerCraftConfirm(Container container) {
        return container instanceof ContainerCraftConfirm;
    }

    private GuiButton getCancelButton(GuiCraftConfirm gui) {
        return ReflectionHelper.getPrivateValue(GuiCraftConfirm.class, gui, "cancel");
    }

    private boolean isGuiCraftingTerm(GuiRecipe<?> gui) {
        return this.getClass().isInstance(gui.getHandler().getOverlayHandler(gui.firstGui, 0));
    }
}
