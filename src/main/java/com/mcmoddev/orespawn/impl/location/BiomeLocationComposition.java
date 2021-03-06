package com.mcmoddev.orespawn.impl.location;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcmoddev.orespawn.api.BiomeLocation;
import com.mcmoddev.orespawn.data.Constants;

import net.minecraft.world.biome.Biome;

public final class BiomeLocationComposition implements BiomeLocation {

	private final BiomeLocation inclusions;

	private final BiomeLocation exclusions;

	private final int hash;

	public BiomeLocationComposition(final BiomeLocation inclusions,
			final BiomeLocation exclusions) {
		this.inclusions = inclusions;
		this.exclusions = exclusions;
		this.hash = Objects.hash(inclusions, exclusions);
	}

	@Override
	public boolean matches(final Biome biome) {
		final boolean inWhite = this.inclusions.matches(biome);
		final boolean inBlack = this.exclusions.matches(biome);

		return !inBlack && inWhite;
	}

	@Override
	public int hashCode() {
		return this.hash;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof BiomeLocationComposition) {
			final BiomeLocationComposition other = (BiomeLocationComposition) obj;
			return this.inclusions.equals(other.inclusions)
					&& this.exclusions.equals(other.exclusions);
		}

		return false;
	}

	@Override
	public ImmutableList<Biome> getBiomes() {
		final List<Biome> temp = new LinkedList<>();
		temp.addAll(this.inclusions.getBiomes());
		temp.addAll(this.exclusions.getBiomes());
		return ImmutableList.copyOf(temp);
	}

	public BiomeLocation getInclusions() {
		return this.inclusions;
	}

	public BiomeLocation getExclusions() {
		return this.exclusions;
	}

	@Override
	public JsonElement serialize() {
		final JsonObject rv = new JsonObject();

		rv.add(Constants.ConfigNames.BLACKLIST, this.exclusions.serialize());
		if (!(this.inclusions instanceof BiomeLocationEmpty)) {
			rv.add(Constants.ConfigNames.WHITELIST, this.inclusions.serialize());
		}

		return rv;
	}

}
