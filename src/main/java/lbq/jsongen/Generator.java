package lbq.jsongen;

import static lbq.jsongen.JSONConstants.*;
import static lbq.jsongen.JSONUtil.*;
import static lbq.jsongen.Util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {

	private final Path basePath;
	private boolean generateManifest;
	private boolean update;
	private boolean packToFolders;
	private boolean lwjglCompat;
	private boolean micromixin;
	private boolean clean;
	private Instant startTime;
	private String postfix;
	private String version;

	private static JSONObject manifest;

	public Generator(Path dir, boolean update, boolean packToFolders, boolean genManifest, String postfix,
			String version, boolean lwjglCompat, boolean micromixin, boolean clean) {
		this.basePath = dir;
		this.generateManifest = genManifest;
		this.update = update;
		this.packToFolders = packToFolders;
		this.startTime = Instant.now();
		this.postfix = postfix;
		this.version = version;
		this.lwjglCompat = lwjglCompat;
		this.micromixin = micromixin;
		this.clean = clean;
	}

	public void generate() throws IOException {
		Set<String> assets = new HashSet<>();
		Files.createDirectories(basePath.resolve("assets"));
		if (update) {
			Files.createDirectories(basePath);
			for (Path p : collectJSONs(basePath)) {
				JSONObject jobj = parseJSON(p);
				String id = jobj.getString("id");
				if (version != null && !version.equals(id)) {
					continue;
				}
				String assetHash = null;
				int assetSize = 0;

				JSONObject assetIndex = jobj.optJSONObject("assetIndex");
				if(assetIndex != null) {
					String assetsId = assetIndex.getString("id");
					if(!assets.contains(assetsId)) {
						assets.add(assetsId);
						String url = assetIndex.getString("url");
						if(!url.contains("mojang.com")) {
							JSONObject assetsJson = JSONUtil.parseJSON(openStream(url));
							JSONObject objects = assetsJson.getJSONObject("objects");
							JSONObject custom = new JSONObject();
							Set<String> removeAssets = new HashSet<>();
							for(String s : objects.keySet()) {
								if(s.equals("index.xml")) {
									continue;
								}
								JSONObject obj = objects.getJSONObject(s);
								if(obj.has("url")) {
									removeAssets.add(s);
									if(custom.isEmpty()) {
										assetsJson.put("custom", custom);
									}
									custom.put(s, obj);
								}
							}
							objects.remove("index.xml");
							for(String s : removeAssets) {
								objects.remove(s);
							}
							writeJSON(assetsJson, basePath.resolve("assets/" + assetsId + ".json"));
						}
					}
					try {
						assetHash = Util.getSHA1(Files.newInputStream(basePath.resolve("assets/" + assetsId + ".json")));
						assetSize = Util.readAllBytes(Files.newInputStream(basePath.resolve("assets/" + assetsId + ".json"))).length;
					}
					catch (IOException e) {

					}
				}
				boolean updated = update(jobj, postfix, lwjglCompat, micromixin, assetHash, assetSize, startTime);
				if (updated)
					System.out.println("Modified version: " + id);
				else {
					// System.out.println("Unmodified version: " + id);
				}
				id = jobj.getString("id");

				if (updated) {
					writeJSON(jobj, basePath.resolve(id + ".json"));
				}
				if (packToFolders) {
					Path outPath = Files.createDirectory(basePath.resolve(id)).resolve(id + ".json");
					Files.deleteIfExists(outPath);
					Files.copy(basePath.resolve(id + ".json"), outPath);
				}
			}
		} else {
			manifest = parseJSON(openStream("https://meta.omniarchive.uk/v1/manifest.json"));
			Files.createDirectories(basePath);
			generateJSONs();
		}
		if (generateManifest) {
			generateManifest(false);
			generateManifest(true);
		}
	}

	public static boolean update(JSONObject json, String postfix, boolean lwjglCompat, boolean micromixin, String assetsHash, int size,
			Instant startTime) throws IOException {
		boolean updated = false;
		Instant time = getTime(json.getString("releaseTime"));
		String id = json.getString("id");
		String idNew = id;
		if (postfix != null && !id.endsWith(postfix)) {
			idNew = id + postfix;
			updated = true;
		}
		JSONObject assetIndex = json.getJSONObject("assetIndex");
		String assetIndexUrl = assetIndex.getString("url");
		String assetIndexHash = assetIndex.getString("sha1");
		if(assetsHash != null) {
			if(!assetsHash.equals(assetIndexHash) || !assetIndexUrl.equals( "https://mcphackers.org/BetterJSONs/jsons/assets/" + assetIndex.getString("id") + ".json")) {
				assetIndex.put("sha1", assetsHash);
				assetIndex.put("size", size);
				assetIndex.put("url", "https://mcphackers.org/BetterJSONs/jsons/assets/" + assetIndex.getString("id") + ".json");
				updated = true;
			}
		}
		json.put("id", idNew);
		lwjglCompat = id.endsWith("-lwjgl3");
		JSONArray verLibs = json.getJSONArray("libraries");
		if (time.compareTo(PAULSCODE_TIME) > 0) {
			JSONObject preset_paulscode = getPreset("paulscode");
			updated |= mergePreset(preset_paulscode, json);
		}
		if (time.compareTo(LWJGL2_TIME) > 0) {
			JSONObject preset_lwjgl3 = getPreset("lwjgl3");
			updated |= mergePreset(preset_lwjgl3, json);
			if(lwjglCompat) {
				return false;
			}
		} else {
			if (lwjglCompat) {
				JSONObject preset_lwjgl3compat = getPreset("lwjgl3compat");
				JSONObject preset_lwjgl3 = getPreset("lwjgl3");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl_util");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl-platform");
				updated |= mergePreset(preset_lwjgl3compat, json);
				updated |= mergePreset(preset_lwjgl3, json);
			} else {
				JSONObject preset_lwjgl2 = getPreset("lwjgl2");
				updated |= mergePreset(preset_lwjgl2, json);
			}
		}
		boolean hasWrapper = false;
		for (int i2 = 0; i2 < verLibs.length(); i2++) {
			String[] lib = verLibs.getJSONObject(i2).getString("name").split(":");
			if (lib[0].equals("net.minecraft") && lib[1].equals("launchwrapper")) {
				hasWrapper = true;
				break;
			}
		}
		Instant releaseTimeInstant = getTime(json.getString("releaseTime"));
		if (hasWrapper) {
			// Remove LegacyLauncher dependencies
			updated |= removeLibrary(verLibs, "org.ow2.asm", "asm-all");
			updated |= removeLibrary(verLibs, "net.sf.jopt-simple", "jopt-simple");
			updated |= removeLibrary(verLibs, "net.minecraft", "launchwrapper");
		}
		// TODO update LW base json to use arguments array instead (NOTE: MMC doesn't
		// support it)
		// if (json.has("arguments")) {
		// 	json.remove("arguments");
		// 	updated = true;
		// }
		JSONObject arguments = new JSONObject();
		JSONArray argumentsGame = new JSONArray();
		JSONArray argumentsJvm = new JSONArray();
		arguments.put("game", argumentsGame);
		arguments.put("jvm", argumentsJvm);

		JSONObject preset_launchwrapper = getPreset("launchwrapper");
		String minecraftArguments = preset_launchwrapper.getString("minecraftArguments");
		List<String> argsList = new ArrayList<>();
		argsList.addAll(Arrays.asList(minecraftArguments.split(" ")));
		if (!argsList.contains("--skinProxy")) {
			String skin = getSkin(releaseTimeInstant, id);
			if (skin != null) {
				argsList.add("--skinProxy");
				argsList.add(skin);
				// preset_launchwrapper.put("minecraftArguments", String.join(" ", argsList));
			}
		}
		for(String s :argsList) {
			argumentsGame.put(s);
		}
		JSONObject a = new JSONObject();
		JSONObject features = new JSONObject();
		JSONArray rules = new JSONArray();
		JSONObject rule = new JSONObject();
		a.put("rules", rules);
		a.put("value", "--demo");
		rule.put("action", "allow");
		features.put("is_demo_user", true);
		rule.put("features", features);
		rules.put(rule);
		argumentsGame.put(a);

		a = new JSONObject();
		features = new JSONObject();
		rules = new JSONArray();
		rule = new JSONObject();
		a.put("rules", rules);
		a.put("value", getStringsAsArray("--width", "${resolution_width}", "--height", "${resolution_height}"));
		rule.put("action", "allow");
		features.put("has_custom_resolution", true);
		rule.put("features", features);
		rules.put(rule);
		argumentsGame.put(a);

		a = new JSONObject();
		features = new JSONObject();
		rules = new JSONArray();
		rule = new JSONObject();
		a.put("rules", rules);
		a.put("value", "--fullscreen");
		rule.put("action", "allow");
		features.put("has_fullscreen", true);
		rule.put("features", features);
		rules.put(rule);
		argumentsGame.put(a);

		if(containsLibrary(verLibs, "org.lwjgl", "lwjgl")) {
			a = new JSONObject();
			JSONObject os = new JSONObject();
			rules = new JSONArray();
			rule = new JSONObject();
			a.put("rules", rules);
			a.put("value", getStringsAsArray("-XstartOnFirstThread", "-Djava.awt.headless=true"));
			rule.put("action", "allow");
			os.put("name", "osx");
			rule.put("os", os);
			rules.put(rule);
			argumentsJvm.put(a);
		}
		argumentsJvm.put("-Djava.library.path=${natives_directory}");
        argumentsJvm.put("-Dminecraft.launcher.brand=${launcher_name}");
        argumentsJvm.put("-Dminecraft.launcher.version=${launcher_version}");
        argumentsJvm.put("-cp");
        argumentsJvm.put("${classpath}");

		preset_launchwrapper.put("arguments", arguments);
		preset_launchwrapper.remove("minecraftArguments");
		updated |= mergePreset(preset_launchwrapper, json);
		if (micromixin) {
			JSONObject preset_launchwrapper_micromixin = getPreset("launchwrapper_micromixin");
			updated |= mergePreset(preset_launchwrapper_micromixin, json);
		}
		// if (id.equals("1.7.6-pre1") || id.equals("1.7.6-pre2") || id.equals("1.7.7"))
		// {
		// JSONObject authLib156 =
		// generateLibraryEntry("https://libraries.minecraft.net/",
		// "com.mojang:authlib:1.5.6");
		// updated |= replaceLibrary(verLibs, authLib156);
		// }

		if (updated) {
			json.put("time", getTimeString(startTime));
		}
		return updated;
	}

	public void generateJSONs() throws IOException {
		Set<String> savedJSONs = new HashSet<>();
		JSONArray versionsArr = manifest.getJSONArray("versions");
		for (int i = 0; i < versionsArr.length(); i++) {
			JSONObject ver = versionsArr.getJSONObject(i);
			try {
				String versionUrl = ver.getString("url");
				if (version != null && !ver.getString("id").equals(version)) {
					continue;
				}
				String id = ver.getString("id") + (postfix == null ? "" : postfix);
				Path jsonOut = basePath.resolve(id + ".json");
				if (savedJSONs.contains(id)) {
					continue;
				}
				if (!clean && Files.exists(jsonOut)) {
					continue;
				}
				Instant time = getTime(ver.getString("releaseTime"));
				if(lwjglCompat && time.compareTo(LWJGL2_TIME) > 0) {
					continue;
				}
				System.out.print(id + "..");
				JSONObject versionJ = fetchVersion(versionUrl);
				if (versionJ == null /* || !version.has("minecraftArguments") */) {
					System.out.println("skipped");
					continue;
				}
				update(versionJ, postfix, lwjglCompat, micromixin, null, 0, startTime);
				System.out.println("done");
				writeJSON(versionJ, jsonOut);
				if (packToFolders) {
					Path outPath = Files.createDirectory(basePath.resolve(id)).resolve(id + ".json");
					Files.deleteIfExists(outPath);
					Files.copy(jsonOut, outPath);
				}
				if (ver.getString("id").equals(version)) {
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void generateManifest(boolean v2) throws IOException {
		JSONObject manifest = new JSONObject();
		JSONObject latest = new JSONObject();
		JSONArray versions = new JSONArray();
		manifest.put("latest", latest);
		manifest.put("versions", versions);

		Instant latestSnapshotTime = null;
		Instant latestReleaseTime = null;
		String latestSnapshot = null;
		String latestRelease = null;
		List<JSONObject> versionsList = new ArrayList<>();
		for (Path p : collectJSONs(basePath)) {
			try {
				JSONObject version = new JSONObject();
				JSONObject json = parseJSON(p);
				String id = json.getString("id");
				String type = json.getString("type");
				String releaseTime = json.getString("releaseTime");
				version.put("id", id);
				version.put("releaseTime", releaseTime);
				version.put("time", json.getString("time"));
				version.put("type", type);
				version.put("url", "https://mcphackers.org/BetterJSONs/jsons/" + id + ".json");
				if (v2) {
					version.put("sha1", Util.getSHA1(Files.newInputStream(p)));
					version.put("complianceLevel", 0);
				}
				versionsList.add(version);
				if (type.equals("release")) {
					Instant time = getTime(releaseTime);
					if (latestReleaseTime == null || time.compareTo(latestReleaseTime) > 0) {
						latestReleaseTime = time;
						latestRelease = id;
					}
				} else if (type.equals("snapshot")) {
					Instant time = getTime(releaseTime);
					if (latestSnapshotTime == null || time.compareTo(latestSnapshotTime) > 0) {
						latestSnapshotTime = time;
						latestSnapshot = id;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		versionsList.sort((obj, obj2) -> {
			return getTime(obj2.getString("releaseTime")).compareTo(getTime(obj.getString("releaseTime")));
		});
		versions.putAll(versionsList);
		latest.put("release", latestRelease);
		latest.put("snapshot", latestSnapshot);
		writeJSON(manifest, basePath.getParent().resolve(v2 ? "version_manifest_v2.json" : "version_manifest.json"));
	}

	public static JSONObject getPreset(String presetName) {
		JSONObject preset;
		try {
			preset = JSONUtil.parseJSON(ClassLoader.getSystemResourceAsStream("preset_" + presetName + ".json"));
		} catch (IOException e) {
			return null;
		}
		preset.remove("version");
		preset.remove("formatVersion");
		preset.remove("requires");
		preset.remove("name");
		preset.remove("uid");
		preset.remove("+traits");
		return preset;
	}

	public static List<Path> collectJSONs(Path dir) throws IOException {
		return Files.list(dir).filter(p -> (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json")))
				.collect(Collectors.toList());
	}
}
