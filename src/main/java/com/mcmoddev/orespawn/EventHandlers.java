package com.mcmoddev.orespawn;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import com.mcmoddev.orespawn.api.os3.BuilderLogic;
import com.mcmoddev.orespawn.api.os3.SpawnBuilder;
import com.mcmoddev.orespawn.data.Config;
import com.mcmoddev.orespawn.data.Constants;
import com.mcmoddev.orespawn.worldgen.OreSpawnWorldGen;

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

public class EventHandlers {
	private List<ChunkPos> chunks;
	private List<ChunkPos> retroChunks;
	
    public EventHandlers() {
    	chunks = new LinkedList<>();
    	retroChunks = new LinkedList<>();
    }

    List<EventType> vanillaEvents = Arrays.asList(EventType.ANDESITE, EventType.COAL, EventType.DIAMOND, EventType.DIORITE, EventType.DIRT, 
    		EventType.EMERALD, EventType.GOLD, EventType.GRANITE, EventType.GRAVEL, EventType.IRON, EventType.LAPIS, EventType.REDSTONE, 
    		EventType.QUARTZ, EventType.SILVERFISH);

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onGenerateMinable(OreGenEvent.GenerateMinable event) {
    	if( Config.getBoolean(Constants.REPLACE_VANILLA_OREGEN) ) {
    		if( vanillaEvents.contains(event.getType())) {
    			event.setResult(Event.Result.DENY);
    		}
    	}
    }
    
	@SubscribeEvent
	public void onChunkSave(ChunkDataEvent.Save ev) {
		NBTTagCompound dataTag = ev.getData().getCompoundTag(Constants.CHUNK_TAG_NAME);
		NBTTagList ores = new NBTTagList();
		NBTTagList features = new NBTTagList();
		boolean retro = retroChunks.contains( new ChunkPos(ev.getChunk().x, ev.getChunk().z) );
		
		features.appendTag( new NBTTagString("orespawn:default"));
		
		for( Entry<String, BuilderLogic> ent : OreSpawn.API.getSpawns().entrySet() ) {
			BuilderLogic log = ent.getValue();
			if( log.getAllDimensions().containsKey(ev.getWorld().provider.getDimension()) ) {
				Collection<SpawnBuilder> vals = log.getDimension(ev.getWorld().provider.getDimension()).getAllSpawns();
				for( SpawnBuilder s : vals ) {
					ores.appendTag( new NBTTagString( s.getOres().get(0).getOre().getBlock().getRegistryName().toString() ) );
				}
			}
			if( log.getAllDimensions().containsKey(OreSpawn.API.dimensionWildcard()) ) {
				Collection<SpawnBuilder> vals = log.getDimension(OreSpawn.API.dimensionWildcard()).getAllSpawns();
				for( SpawnBuilder s : vals ) {
					ores.appendTag( new NBTTagString( s.getOres().get(0).getOre().getBlock().getRegistryName().toString() ) );
				}				
			}
		}
		
		dataTag.setTag(Constants.ORE_TAG, ores);
		dataTag.setTag(Constants.FEATURES_TAG, features);
		dataTag.setBoolean(Constants.RETRO_BEDROCK_TAG, retro);
		ev.getData().setTag(Constants.CHUNK_TAG_NAME, dataTag);
	}
	
	@SubscribeEvent
	public void onChunkLoad(ChunkDataEvent.Load ev) {
		World world = ev.getWorld();
		ChunkPos chunkCoords = new ChunkPos(ev.getChunk().x, ev.getChunk().z);
		int chunkX = ev.getChunk().x;
		int chunkZ = ev.getChunk().z;

		
		doBlockRetrogen(world, chunkCoords, ev.getData());
		
		if( chunks.contains(chunkCoords) ) {
			return;
		}
		
		if( Config.getBoolean(Constants.RETROGEN_KEY) ) {
			chunks.add(chunkCoords);
			
			NBTTagCompound chunkTag = ev.getData().getCompoundTag(Constants.CHUNK_TAG_NAME);
			int count = chunkTag==null?0:chunkTag.getTagList(Constants.ORE_TAG, 8).tagCount();
			if( count != countOres(ev.getWorld().provider.getDimension()) ||
					Config.getBoolean(Constants.FORCE_RETROGEN_KEY)) {
				OreSpawnWorldGen owg = OreSpawn.API.getGenerator();
				long worldSeed = world.getSeed();
				Random fmlRandom = new Random(worldSeed);
				long xSeed = fmlRandom.nextLong() >> 2 + 1L;
				long zSeed = fmlRandom.nextLong() >> 2 + 1L;
				long chunkSeed = (xSeed * chunkCoords.x + zSeed * chunkCoords.z) ^ worldSeed;

				fmlRandom.setSeed(chunkSeed);
				ChunkProviderServer chunkProvider = (ChunkProviderServer) world.getChunkProvider();
				IChunkGenerator chunkGenerator = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderServer.class, chunkProvider, "field_186029_c", "chunkGenerator");
				owg.generate(fmlRandom, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
			}
		}
	}


	private void doBlockRetrogen(World world, ChunkPos chunkCoords, NBTTagCompound eventData) {
		if( retroChunks.contains(chunkCoords) ) return;
		if( Config.getBoolean(Constants.RETRO_BEDROCK) ) {
			NBTTagCompound chunkTag = eventData.getCompoundTag(Constants.CHUNK_TAG_NAME);
			if( chunkTag != null && (!chunkTag.hasKey( Constants.RETRO_BEDROCK_TAG ) || !chunkTag.getBoolean(Constants.RETRO_BEDROCK_TAG))) {
				// make sure we record that we've hit this chunk already
				retroChunks.add(chunkCoords);
				OreSpawn.flatBedrock.retrogen(world, chunkCoords.x, chunkCoords.z);
			}
		}
	}

	private int countOres(int dim) {
		int acc = 0;
		for( Entry<String, BuilderLogic> sL : OreSpawn.API.getSpawns().entrySet() ) {
			if( sL.getValue().getAllDimensions().containsKey(dim) ) {
				acc += sL.getValue().getAllDimensions().get(dim).getAllSpawns().size();
			}
			if( sL.getValue().getAllDimensions().containsKey(OreSpawn.API.dimensionWildcard()) ) {
				acc += sL.getValue().getAllDimensions().get(OreSpawn.API.dimensionWildcard()).getAllSpawns().size();
			}
		}
		return acc;
	}
	
	
}
