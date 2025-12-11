package net.milkucha.notp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.text.Text;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Notp implements ModInitializer {
	public static final String MODID = "notp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	// This will be initialized after config is loaded
	public static SimpleCommandExceptionType OUT_OF_BOUNDS;

	public static class Region {
		public int xMin, xMax, zMin, zMax;

		public Region() {} // for Gson

		public Region(int xMin, int xMax, int zMin, int zMax) {
			this.xMin = xMin;
			this.xMax = xMax;
			this.zMin = zMin;
			this.zMax = zMax;
		}

		public boolean contains(double x, double z) {
			return x >= xMin && x <= xMax && z >= zMin && z <= zMax;
		}

		@Override
		public String toString() {
			return "Region{xMin=" + xMin + ", xMax=" + xMax + ", zMin=" + zMin + ", zMax=" + zMax + "}";
		}
	}

	private static List<Region> REGIONS = new ArrayList<>();

	@Override
	public void onInitialize() {
		loadConfig();
		LOGGER.info("Notp mod initialized with {} regions", REGIONS.size());
		for (int i = 0; i < REGIONS.size(); i++) {
			LOGGER.info("  region[{}] = {}", i, REGIONS.get(i));
		}
	}

	private void loadConfig() {
		try {
			Path configDir = FabricLoader.getInstance().getConfigDir();
			Path configFilePath = configDir.resolve(MODID + ".json");

			Files.createDirectories(configDir); // ensure config dir exists

			Gson gson = new Gson();
			Config cfg;

			if (Files.exists(configFilePath)) {
				try (Reader reader = Files.newBufferedReader(configFilePath)) {
					Type type = new TypeToken<Config>(){}.getType();
					cfg = gson.fromJson(reader, type);
				} catch (Exception e) {
					LOGGER.error("Failed to parse config file {}, falling back to defaults", configFilePath.toAbsolutePath(), e);
					cfg = defaultConfig();
				}
			} else {
				cfg = defaultConfig();
				try (Writer writer = Files.newBufferedWriter(configFilePath)) {
					gson.toJson(cfg, writer);
					LOGGER.info("Wrote default config to {}", configFilePath.toAbsolutePath());
				} catch (Exception e) {
					LOGGER.error("Failed to write default config to {}", configFilePath.toAbsolutePath(), e);
				}
			}

			// Apply config
			if (cfg.regions == null || cfg.regions.isEmpty()) {
				REGIONS = defaultRegions();
			} else {
				List<Region> cleaned = new ArrayList<>();
				for (Region r : cfg.regions) {
					if (r != null) cleaned.add(r);
					else LOGGER.warn("Found null region entry in config, skipping");
				}
				REGIONS = cleaned.isEmpty() ? defaultRegions() : cleaned;
			}

			// Initialize the customizable message
			String message = (cfg.outOfBoundsMessage == null || cfg.outOfBoundsMessage.isBlank())
					? "You're outside the allowed teleporting region! You must travel through space"
					: cfg.outOfBoundsMessage;
			OUT_OF_BOUNDS = new SimpleCommandExceptionType(Text.literal(message));

		} catch (Exception e) {
			LOGGER.error("Failed to load config", e);
			REGIONS = defaultRegions();
			OUT_OF_BOUNDS = new SimpleCommandExceptionType(Text.literal(
					"You're outside the allowed teleporting region! You must travel through space"));
		}
	}

	private static List<Region> defaultRegions() {
		List<Region> defaults = new ArrayList<>();
		defaults.add(new Region(-50, 50, -50, 50));
		return defaults;
	}

	private static Config defaultConfig() {
		Config cfg = new Config();
		cfg.regions.add(new Region(-50, 50, -50, 50));
		cfg.outOfBoundsMessage = "You're outside the allowed teleporting region! You must travel through space";
		return cfg;
	}

	public static Region getRegion(double x, double z) {
		if (REGIONS == null || REGIONS.isEmpty()) return null;
		for (Region r : REGIONS) {
			if (r != null && r.contains(x, z)) return r;
		}
		return null;
	}

	public static boolean canTeleport(double srcX, double srcZ, double dstX, double dstZ) {
		Region srcRegion = getRegion(srcX, srcZ);
		Region dstRegion = getRegion(dstX, dstZ);
		return srcRegion != null && srcRegion == dstRegion;
	}

	public static String debugRegionInfo(double srcX, double srcZ, double dstX, double dstZ) {
		Region src = getRegion(srcX, srcZ);
		Region dst = getRegion(dstX, dstZ);
		return "src=(" + srcX + "," + srcZ + ") -> " + (src == null ? "NONE" : src.toString())
				+ " ; dst=(" + dstX + "," + dstZ + ") -> " + (dst == null ? "NONE" : dst.toString());
	}

	private static class Config {
		List<Region> regions = new ArrayList<>();
		String outOfBoundsMessage;
	}
}