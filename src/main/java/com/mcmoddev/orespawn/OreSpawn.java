package com.mcmoddev.orespawn;

import com.mcmoddev.orespawn.data.Constants;
import com.mcmoddev.orespawn.data.FeatureRegistry;
import com.mcmoddev.orespawn.impl.OreSpawnImpl;
import com.mcmoddev.orespawn.json.OS1Reader;
import com.mcmoddev.orespawn.json.OS2Reader;
import com.mcmoddev.orespawn.json.OS3Reader;
import com.mcmoddev.orespawn.json.OS3Writer;
import com.mcmoddev.orespawn.api.OreSpawnAPI;
import com.mcmoddev.orespawn.commands.AddOreCommand;
import com.mcmoddev.orespawn.commands.ClearChunkCommand;
import com.mcmoddev.orespawn.commands.DumpBiomesCommand;
import com.mcmoddev.orespawn.data.Config;
import com.mcmoddev.orespawn.api.SpawnLogic;

import java.nio.file.Paths;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Main entry point for the mod, everything runs through this
 *
 * @author DShadowWolf &lt;dshadowwolf@gmail.com&gt;
 */

@Mod( modid = Constants.MODID,
      name = Constants.NAME,
      version = Constants.VERSION,
      acceptedMinecraftVersions = "[1.11.2,)" )

public class OreSpawn {
    @Instance
    public static OreSpawn INSTANCE = null;
    public final static Logger LOGGER = LogManager.getFormatterLogger(Constants.MODID);
    public final static OreSpawnAPI API = new OreSpawnImpl();
    public static final OS3Writer writer = new OS3Writer();
    public static final EventHandlers eventHandlers = new EventHandlers();
    public static final FeatureRegistry FEATURES = new FeatureRegistry();
    public static String OS1ConfigPath;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent ev) {
    	Config.loadConfig();
    	
    	if( Config.getBoolean(Constants.RETROGEN_KEY) ) {
    		MinecraftForge.EVENT_BUS.register(eventHandlers);
    	}
    	
    	if( Config.getBoolean(Constants.REPLACE_VANILLA_OREGEN) ) {
    		MinecraftForge.ORE_GEN_BUS.register(eventHandlers);
    	}
    	
    	OS1ConfigPath = Paths.get(ev.getSuggestedConfigurationFile().toPath().getParent().toString(),"orespawn").toString();
    	FMLInterModComms.sendFunctionMessage("orespawn", "api", "com.mcmoddev.orespawn.data.VanillaOrespawn");
    }

    @EventHandler
    public void init(FMLInitializationEvent ev) {
    	OS1Reader.loadEntries(Paths.get(OS1ConfigPath));
    	OS2Reader.loadEntries();
    	OS3Reader.loadEntries();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent ev) {
    	writer.writeSpawnEntries();
    	Config.saveConfig();
    }
    
    @EventHandler
    public void onIMC(FMLInterModComms.IMCEvent event) {
        event.getMessages().stream().filter(message -> "api".equalsIgnoreCase(message.key)).forEach(message -> {
            Optional<Function<OreSpawnAPI, SpawnLogic>> value = message.getFunctionValue(OreSpawnAPI.class, SpawnLogic.class);
            if (OreSpawn.API.getSpawnLogic(message.getSender()) == null && value.isPresent()) {
                OreSpawn.API.registerSpawnLogic(message.getSender(), value.get().apply(OreSpawn.API));
            }
        });
    }
    
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent ev) {
    	ev.registerServerCommand(new ClearChunkCommand());
    	ev.registerServerCommand(new DumpBiomesCommand());
    	ev.registerServerCommand(new AddOreCommand());
    }
}
