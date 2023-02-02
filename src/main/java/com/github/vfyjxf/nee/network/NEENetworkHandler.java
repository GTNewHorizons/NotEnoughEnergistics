package com.github.vfyjxf.nee.network;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.network.packet.*;

import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NEENetworkHandler {

    private static final SimpleNetworkWrapper INSTANCE = new SimpleNetworkWrapper(NotEnoughEnergistics.MODID);
    private static int packId = 0;

    private static int nextId() {
        return packId++;
    }

    public static SimpleNetworkWrapper getInstance() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE.registerMessage(
                PacketNEIPatternRecipe.Handler.class,
                PacketNEIPatternRecipe.class,
                nextId(),
                Side.SERVER);
        INSTANCE.registerMessage(PacketArcaneRecipe.Handler.class, PacketArcaneRecipe.class, nextId(), Side.SERVER);
        INSTANCE.registerMessage(PacketExtremeRecipe.Handler.class, PacketExtremeRecipe.class, nextId(), Side.SERVER);
        INSTANCE.registerMessage(
                PacketStackCountChange.Handler.class,
                PacketStackCountChange.class,
                nextId(),
                Side.SERVER);
        INSTANCE.registerMessage(
                PacketSlotStackChange.Handler.class,
                PacketSlotStackChange.class,
                nextId(),
                Side.SERVER);
        INSTANCE.registerMessage(
                PacketCraftingRequest.Handler.class,
                PacketCraftingRequest.class,
                nextId(),
                Side.SERVER);
        INSTANCE.registerMessage(
                PacketOpenCraftAmount.Handler.class,
                PacketOpenCraftAmount.class,
                nextId(),
                Side.SERVER);
        INSTANCE.registerMessage(PacketOpenGui.Handler.class, PacketOpenGui.class, nextId(), Side.SERVER);
        INSTANCE.registerMessage(PacketSetRecipe.Handler.class, PacketSetRecipe.class, nextId(), Side.SERVER);
        INSTANCE.registerMessage(
                PacketValueConfigServer.Handler.class,
                PacketValueConfigServer.class,
                nextId(),
                Side.SERVER);
        INSTANCE.registerMessage(
                PacketValueConfigClient.Handler.class,
                PacketValueConfigClient.class,
                nextId(),
                Side.CLIENT);
    }
}
