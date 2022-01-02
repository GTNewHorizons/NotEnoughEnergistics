package com.github.vfyjxf.nee.nei;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.core.AELog;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketNEIRecipe;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import com.github.vfyjxf.nee.network.packet.PacketOpenCraftAmount;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class NEECraftingHelper implements IOverlayHandler {
    public static final NEECraftingHelper INSTANCE = new NEECraftingHelper();
    public static IngredientTracker tracker = null;
    public static int stackIndex = 1;
    public static boolean noPreview = false;

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        boolean isCraftingGuiTerm = firstGui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(firstGui);
        if (isCraftingGuiTerm && NEECraftingHandler.isCraftingTableRecipe(recipe)) {
            tracker = new IngredientTracker(firstGui, recipe, recipeIndex);
            boolean doCraftingHelp = Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.preview")) || Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.nopreview"));
            noPreview = Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.nopreview"));
            if (doCraftingHelp) {
                if (noPreview) {
                    if (!tracker.getRequireStacks().isEmpty()) {
                        IAEItemStack stack = AEItemStack.create(tracker.getRequiredStack(0));
                        NEENetworkHandler.getInstance().sendToServer(new PacketCraftingRequest(stack, noPreview));
                    } else {
                        moveItem(firstGui, recipe, recipeIndex);
                    }
                } else if (NEEConfig.enableCraftAmountSettingGui) {
                    NEENetworkHandler.getInstance().sendToServer(new PacketOpenCraftAmount(recipe.getResultStack(recipeIndex).item));
                }
                stackIndex = 1;
            } else {
                moveItem(firstGui, recipe, recipeIndex);
            }
        }
    }

    /**
     * Copied from GTNewHorizons/Applied-Energistics-2-Unofficial
     */
    private void moveItem(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        try {
            final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
            if (firstGui instanceof GuiCraftingTerm) {
                PacketNEIRecipe packet = new PacketNEIRecipe(packIngredients(firstGui, ingredients, false));
                //don't use gtnh ae2's method;
                int packetSize = getPacketSize(packet);
                if (packetSize >= 32 * 1024) {
                    AELog.warn("Recipe for " + recipe.getRecipeName() + " has too many variants, reduced version will be used");
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
        } catch (final Exception | Error ignored) {
        }
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

    /**
     * Copied from GTNewHorizons/Applied-Energistics-2-Unofficial
     */
    @SuppressWarnings("unchecked")
    private NBTTagCompound packIngredients(GuiContainer gui, List<PositionedStack> ingredients, boolean limited) throws IOException {
        final NBTTagCompound recipe = new NBTTagCompound();
        for (final PositionedStack positionedStack : ingredients) {
            final int col = (positionedStack.relx - 25) / 18;
            final int row = (positionedStack.rely - 6) / 18;
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                for (final Slot slot : (List<Slot>) gui.inventorySlots.inventorySlots) {
                    if (isCraftingMatrixSlot(gui, slot)) {
                        if (slot.getSlotIndex() == col + row * 3) {
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
        }
        return recipe;
    }

    private boolean isCraftingMatrixSlot(GuiContainer gui, Slot slot) {
        if (gui instanceof GuiCraftingTerm) {
            return slot instanceof SlotCraftingMatrix || slot instanceof SlotFakeCraftingMatrix;
        } else if (GuiUtils.isGuiWirelessCrafting(gui)) {
            return slot instanceof net.p455w0rd.wirelesscraftingterminal.common.container.slot.SlotCraftingMatrix || slot instanceof net.p455w0rd.wirelesscraftingterminal.common.container.slot.SlotFakeCraftingMatrix;
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

    @Optional.Method(modid = "ae2wct")
    private void moveItemsForWirelessCrafting(GuiContainer firstGui, List<PositionedStack> ingredients) {
        try {
            net.p455w0rd.wirelesscraftingterminal.core.sync.network.NetworkHandler.instance.sendToServer(new net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketNEIRecipe(packIngredients(firstGui, ingredients, false)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onActionPerformedEventPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (NEEConfig.noShift) {
            //make nei's transfer system doesn't require presses shift
            if (event.gui instanceof GuiRecipe) {
                GuiRecipe guiRecipe = (GuiRecipe) event.gui;
                List<GuiButton> overlayButtons = null;
                final int OVERLAY_BUTTON_ID_START = 4;
                boolean isGtnhNei = true;
                try {
                    Field overlayButtonsField = GuiRecipe.class.getDeclaredField("overlayButtons");
                    overlayButtonsField.setAccessible(true);
                    overlayButtons = new ArrayList<>(Arrays.asList((GuiButton[]) overlayButtonsField.get(guiRecipe)));
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    isGtnhNei = false;
                }
                if (!isGtnhNei) {
                    overlayButtons = new ArrayList<>();
                    GuiButton overlay1 = ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay1");
                    GuiButton overlay2 = ReflectionHelper.getPrivateValue(GuiRecipe.class, guiRecipe, "overlay2");
                    overlayButtons.add(overlay1);
                    overlayButtons.add(overlay2);
                }
                if (event.button.id >= OVERLAY_BUTTON_ID_START && event.button.id < OVERLAY_BUTTON_ID_START + overlayButtons.size()) {
                    boolean isPatternTerm = guiRecipe.firstGui instanceof GuiPatternTerm || GuiUtils.isPatternTermExGui(guiRecipe.firstGui);
                    boolean isCraftingTerm = guiRecipe.firstGui instanceof GuiCraftingTerm || GuiUtils.isGuiWirelessCrafting(guiRecipe.firstGui);
                    if (isCraftingTerm || isPatternTerm) {
                        int recipesPerPage = 2;
                        IRecipeHandler handler = guiRecipe.currenthandlers.get(guiRecipe.recipetype);
                        if (isGtnhNei) {
                            try {
                                recipesPerPage = (int) ReflectionHelper.findMethod(GuiRecipe.class, guiRecipe, new String[]{"getRecipesPerPage"}).invoke(guiRecipe);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                        if (recipesPerPage >= 0 && handler != null) {
                            int recipe = guiRecipe.page * recipesPerPage + event.button.id - OVERLAY_BUTTON_ID_START;
                            final IOverlayHandler overlayHandler = handler.getOverlayHandler(guiRecipe.firstGui, recipe);
                            Minecraft.getMinecraft().displayGuiScreen(guiRecipe.firstGui);
                            overlayHandler.overlayRecipe(guiRecipe.firstGui, guiRecipe.currenthandlers.get(guiRecipe.recipetype), recipe, NEIClientUtils.shiftKey());
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

}
