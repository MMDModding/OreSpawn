package com.mcmoddev.orespawn.api.os3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class OreSpawnBlockMatcher implements Predicate<IBlockState> {
	private final List<IBlockState> possibles;
	
	public OreSpawnBlockMatcher(final IBlockState...matches) {
		this.possibles = Arrays.asList(matches);
	}
	
	public OreSpawnBlockMatcher(final List<IBlockState> matches) {
		this.possibles = new ArrayList<>();
		this.possibles.addAll(matches);
	}
	
	public boolean test(final IBlockState other) {
		return other != null && other.getBlock() != Blocks.AIR && possibles.contains(other);
	}
}
