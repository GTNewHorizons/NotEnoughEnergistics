package com.github.vfyjxf.nee.network;

import com.github.vfyjxf.nee.NotEnoughEnergistics;
import com.github.vfyjxf.nee.network.packet.PacketArcaneRecipe;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import com.github.vfyjxf.nee.network.packet.PacketExtremeRecipe;
import com.github.vfyjxf.nee.network.packet.PacketNEIBookmark;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.github.vfyjxf.nee.network.packet.PacketOpenGui;
import com.github.vfyjxf.nee.network.packet.PacketSlotStackChange;
import com.github.vfyjxf.nee.network.packet.PacketStackCountChange;
import com.github.vfyjxf.nee.network.packet.PacketValueConfigClient;
import com.github.vfyjxf.nee.network.packet.PacketValueConfigServer;

import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NEENetworkHandler {

    private static final SimpleNetworkWrapper INSTANCE = new SimpleNetworkWrapper(NotEnoughEnergistics.MODID);
    private static int packId = 0;

    private static int nextId() {
        return packId++;
    }

    private NEENetworkHandler() {
        throw new IllegalStateException("Utility class");
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
        INSTANCE.registerMessage(PacketNEIBookmark.Handler.class, PacketNEIBookmark.class, nextId(), Side.SERVER);
        INSTANCE.registerMessage(PacketOpenGui.Handler.class, PacketOpenGui.class, nextId(), Side.SERVER);
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
