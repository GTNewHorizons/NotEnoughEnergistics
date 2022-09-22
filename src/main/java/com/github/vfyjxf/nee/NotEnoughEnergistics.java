package com.github.vfyjxf.nee;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.network.NEEGuiHandler;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.proxy.CommonProxy;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import java.io.File;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = NotEnoughEnergistics.MODID,
        version = NotEnoughEnergistics.VERSION,
        name = NotEnoughEnergistics.NAME,
        dependencies = NotEnoughEnergistics.DEPENDENCIES,
        guiFactory = NotEnoughEnergistics.GUI_FACTORY,
        useMetadata = true)
public class NotEnoughEnergistics {
    public static final String MODID = "neenergistics";
    public static final String NAME = "NotEnoughEnergistics";
    public static final String VERSION = "GRADLETOKEN_VERSION";
    public static final String DEPENDENCIES =
            "required-after:NotEnoughItems;required-after:appliedenergistics2@[rv3.beta.22,),[rv3.beta-22,),[rv3-beta.22,),[rv3-beta-22,);after:ae2wct;after:ae2fc";
    public static final String GUI_FACTORY = "com.github.vfyjxf.nee.config.NEEConfigGuiFactory";
    public static final Logger logger = LogManager.getLogger("NotEnoughEnergistics");

    @SidedProxy(
            clientSide = "com.github.vfyjxf.nee.proxy.ClientProxy",
            serverSide = "com.github.vfyjxf.nee.proxy.ServerProxy")
    public static CommonProxy proxy;

    @Mod.Instance(MODID)
    public static NotEnoughEnergistics instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        NEENetworkHandler.init();
        NEEConfig.loadConfig(new File(Launch.minecraftHome, "config/NotEnoughEnergistics.cfg"));
        FMLCommonHandler.instance().bus().register(new NEEConfig());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new NEEGuiHandler());
    }
}
