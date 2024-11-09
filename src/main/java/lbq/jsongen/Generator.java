package lbq.jsongen;

import static lbq.jsongen.JSONConstants.*;
import static lbq.jsongen.JSONUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	private boolean multimc;
	private boolean micromixin;
	private Instant startTime;
	private String postfix;
	private String onlyver;

	private static final JSONObject preset_launchwrapper = getPresetJSON("launchwrapper");
	private static final JSONObject preset_launchwrapper_micromixin = getPresetJSON("launchwrapper_micromixin");
	private static final JSONObject preset_lwjgl3compat = getPresetJSON("lwjgl3compat");
	private static final JSONObject preset_lwjgl3 = getPresetJSON("lwjgl3");
	private static final JSONObject preset_lwjgl2 = getPresetJSON("lwjgl2");
	private static final JSONObject preset_paulscode = getPresetJSON("paulscode");
	private static JSONObject manifestBetacraft;
	private static JSONObject manifest;
	private static JSONObject authLib156;
	private static JSONObject legacyIndex = legacyIndex();
	// private Map<String, String> versionServers = new HashMap<>();

	public Generator(Path dir, boolean updateJsons, boolean packToFolder, boolean genManifest, String postfixString,
			String onlyVersion, boolean lwjgl3, boolean b1, boolean b2) {
		basePath = dir;
		generateManifest = genManifest;
		update = updateJsons;
		packToFolders = packToFolder;
		startTime = Instant.now();
		postfix = postfixString;
		onlyver = onlyVersion;
		lwjglCompat = lwjgl3;
		multimc = b1;
		micromixin = b2;
	}

	public void addMultiMCComponent(JSONArray components, JSONObject preset) {
		JSONObject component = new JSONObject();
		component.put("uid", preset.getString("uid"));
		components.put(component);
	}

	public void generate() throws IOException {
		authLib156 = generateLibraryEntry("https://libraries.minecraft.net/", "com.mojang:authlib:1.5.6");
		if(!multimc) {
			for(JSONObject j : new JSONObject[] {preset_launchwrapper, preset_launchwrapper_micromixin, preset_lwjgl2, preset_lwjgl3, preset_lwjgl3compat, preset_paulscode}) {
				j.remove("version");
				j.remove("formatVersion");
				j.remove("requires");
				j.remove("name");
				j.remove("uid");
				j.remove("+traits");
			}
		} else {
			for(JSONObject j : new JSONObject[] {preset_launchwrapper, preset_launchwrapper_micromixin, preset_lwjgl2, preset_lwjgl3, preset_lwjgl3compat, preset_paulscode}) {
				JSONArray libs = j.optJSONArray("libraries");
				if(libs != null) {
					for(int i = 0; i < libs.length(); i++) {
						JSONObject obj = libs.getJSONObject(i);
						if(!obj.has("downloads") && !obj.has("url")) {
							obj.put("downloads", new JSONObject());
						}
					}
				}
			}
		}

		if(multimc) {
			Files.createDirectories(basePath.resolve("patches"));
			Path lwjglJson = basePath.resolve("patches/org.lwjgl.json");
			Path lwJson = basePath.resolve("patches/org.mcphackers.launchwrapper.json");
			Path lwmmJson = basePath.resolve("patches/org.mcphackers.launchwrapper-micromixin.json");
			Path paulscode = basePath.resolve("patches/com.paulscode.json");
			
			JSONObject mmcPack = parseJSON(basePath.resolve("mmc-pack.json"));
			JSONArray components = mmcPack.getJSONArray("components");

			if(lwjglCompat) {
				addMultiMCComponent(components, preset_lwjgl3compat);
				writeJSON(preset_lwjgl3compat, lwjglJson);
			} else {
				addMultiMCComponent(components, preset_lwjgl2);
				writeJSON(preset_lwjgl2, lwjglJson);
			}
			addMultiMCComponent(components, preset_launchwrapper);
			writeJSON(preset_launchwrapper, lwJson);
			if(micromixin) {
				addMultiMCComponent(components, preset_launchwrapper_micromixin);
				writeJSON(preset_launchwrapper_micromixin, lwmmJson);
			}
			addMultiMCComponent(components, preset_paulscode);
			writeJSON(preset_paulscode, paulscode);
			writeJSON(mmcPack, basePath.resolve("mmc-pack.json"));
			return;
		}
		if (update) {
			Files.createDirectories(basePath);
			for (Path p : collectJSONs(basePath)) {
				JSONObject jobj = parseJSON(p);
				String id = jobj.getString("id");
				if(onlyver != null && !onlyver.equals(id)) {
					continue;
				}
				boolean updated = update(jobj);
				if(updated)
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
			manifestBetacraft = parseJSON(new URL("https://files.betacraft.uk/launcher/v2/assets/version_list.json").openStream());
			manifest = getManifest();
			Files.createDirectories(basePath);
			generateJSONs();
		}
		if (generateManifest) {
			generateManifest(false);
			generateManifest(true);
		}
	}

	public boolean update(JSONObject json) throws IOException {
		boolean updated = false;
		Instant time = getTime(json.getString("releaseTime"));
		String id = json.getString("id");
		String idNew = id;
		if(postfix != null && !id.endsWith(postfix)) {
			idNew = id + postfix;
			updated = true;
		}
		json.put("id", idNew);
		JSONArray verLibs = json.getJSONArray("libraries");
		if (time.compareTo(PAULSCODE_TIME) > 0) {
			updated |= mergePreset(preset_paulscode, json);
		}
		if (time.compareTo(LWJGL2_TIME) > 0) {
			updated |= mergePreset(preset_lwjgl3, json);
		} else {
			if (lwjglCompat) {
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl_util");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl-platform");
				updated |= mergePreset(preset_lwjgl3, json);
				updated |= mergePreset(preset_lwjgl3compat, json);
			} else {
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
			if (lib[0].equals("org.mcphackers") && lib[1].equals("launchwrapper")) {
				hasWrapper = true;
				break;
			}
		}
		Instant releaseTimeInstant = getTime(json.getString("releaseTime"));
		if (hasWrapper) {
			updated |= removeLibrary(verLibs, "org.ow2.asm", "asm-all");
			updated |= removeLibrary(verLibs, "net.sf.jopt-simple", "jopt-simple");
			updated |= removeLibrary(verLibs, "net.minecraft", "launchwrapper");
			updated |= mergePreset(preset_launchwrapper, json);
			if(micromixin) {
				updated |= mergePreset(preset_launchwrapper_micromixin, json);
			}
			if(json.has("arguments")) {
				json.remove("arguments");
				updated = true;
			}
			String minecraftArguments = json.optString("minecraftArguments");
			List<String> argsList = Arrays.asList(minecraftArguments.split(" "));
			if(!argsList.contains("--skinProxy")) {
				String skin = getSkin(releaseTimeInstant, id);
				if (skin != null) {
					argsList.add("--skinProxy");
					argsList.add(skin);
					updated = true;
					json.put("minecraftArguments", String.join(" ", argsList));
				}
			}
		}
		if (id.equals("1.7.6-pre1") || id.equals("1.7.6-pre2") || id.equals("1.7.7")) {
			updated |= replaceLibrary(verLibs, authLib156);
		}

		// JSONObject dls = json.getJSONObject("downloads");
		// if (!dls.has("server") && versionServers.containsKey(id)) {
		// 	updated = true;
		// 	String serverUrl = versionServers.get(id);
		// 	dls.put("server", getLibraryArtifact(new URL(serverUrl).openStream(), serverUrl));
		// }
		if (updated) {
			json.put("time", getTimeString(startTime));
		}
		return updated;
	}

	public void generateJSONs() throws IOException {
		Set<String> savedJSONs = new HashSet<>();
		JSONArray versions1 = manifest.getJSONArray("versions");
		JSONArray versions2 = manifestBetacraft.getJSONArray("versions");
		Map<String, JSONObject> versions = new HashMap<>();
		for(int i = 0; i < versions1.length(); i++) {
			JSONObject j = versions1.getJSONObject(i);
			versions.put(j.getString("id"), j);
		}
		for(int i = 0; i < versions2.length(); i++) {
			JSONObject j = versions2.getJSONObject(i);
			versions.put(j.getString("id"), j);
		}
		for (JSONObject ver : versions.values()) {
			try {
				URL versionUrl = new URL(ver.getString("url"));
				if (onlyver != null && !ver.getString("id").equals(onlyver)) {
					continue;
				}
				String id = ver.getString("id") + (postfix == null ? "" : postfix);
				if (savedJSONs.contains(id)) {
					continue;
				}
				JSONObject version = fetchVersion(versionUrl);
				System.out.print(id + "..");
				if (version == null || !version.has("minecraftArguments")) {
					System.out.println("skipped");
					continue;
				}
				update(version);
				System.out.println("done");
				Path jsonOut = basePath.resolve(id + ".json");
				writeJSON(version, jsonOut);
				if (packToFolders) {
					Path outPath = Files.createDirectory(basePath.resolve(id)).resolve(id + ".json");
					Files.deleteIfExists(outPath);
					Files.copy(jsonOut, outPath);
				}
				if (ver.getString("id").equals(onlyver)) {
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// private JSONArray removePaulscode(JSONArray libraries) {
	// 	JSONArray librariesNoSoundLib = new JSONArray(libraries);
	// 	for (int i = librariesNoSoundLib.length() - 1; i >= 0; i--) {
	// 		JSONObject obj = librariesNoSoundLib.getJSONObject(i);
	// 		String[] libraryName = obj.getString("name").split(":");
	// 		if (libraryName[0].equals("com.paulscode")) {
	// 			librariesNoSoundLib.remove(i);
	// 		}
	// 	}
	// 	return librariesNoSoundLib;
	// }

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
				version.put("url", "https://mcphackers.github.io/BetterJSONs/jsons/" + id + ".json");
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

	private static JSONObject legacyIndex() {
		try {
			String assetIndex = "https://launchermeta.mojang.com/v1/packages/770572e819335b6c0a053f8378ad88eda189fc14/legacy.json";
			byte[] jar = Util.readAllBytes(new URL(assetIndex).openStream());
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			String assetsId = assetIndex.substring(assetIndex.lastIndexOf('/') + 1).replace(".json", "");
			JSONObject assetIndexObj = new JSONObject();
			assetIndexObj.put("id", assetsId);
			assetIndexObj.put("size", jar.length);
			assetIndexObj.put("sha1", sha1);
			assetIndexObj.put("url", assetIndex);
			return assetIndexObj;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static List<Path> collectJSONs(Path dir) throws IOException {
		return Files.list(dir).filter(p -> (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json")))
				.collect(Collectors.toList());
	}

	// @Deprecated
	// public JSONObject createVersionJSON(String id, String clientURL, String serverURL, String releaseTime)
	// 		throws IOException {
	// 	JSONObject version = new JSONObject();
	// 	JSONArray libraries = preset_launchwrapper.getJSONArray("libraries");
	// 	JSONArray librariesNoSoundLib = removePaulscode(libraries);
	// 	JSONObject downloads = new JSONObject();
	// 	version.put("downloads", downloads);
	// 	if (clientURL != null) {
	// 		JSONObject client = new JSONObject();
	// 		byte[] jar = Util.readAllBytes(new URL(clientURL).openStream());
	// 		String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
	// 		client.put("url", clientURL);
	// 		client.put("size", jar.length);
	// 		client.put("sha1", sha1);

	// 		downloads.put("client", client);
	// 	}
	// 	if (serverURL != null) {
	// 		JSONObject server = new JSONObject();
	// 		byte[] jar = Util.readAllBytes(new URL(serverURL).openStream());
	// 		String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
	// 		server.put("url", serverURL);
	// 		server.put("size", jar.length);
	// 		server.put("sha1", sha1);

	// 		downloads.put("server", server);
	// 	}
	// 	version.put("id", id);
	// 	if (id.startsWith("a") || id.startsWith("c") || id.startsWith("in")) {
	// 		version.put("type", "old_alpha");
	// 	} else if (id.startsWith("b")) {
	// 		version.put("type", "old_beta");
	// 	} else if (id.charAt(2) == 'w' || id.equals("1.2")) {
	// 		version.put("type", "snapshot");
	// 	} else {
	// 		version.put("type", "release");
	// 	}
	// 	version.put("releaseTime", releaseTime);
	// 	version.put("time", getTimeString(startTime));
	// 	Instant releaseTimeInstant = getTime(releaseTime);
	// 	if (releaseTimeInstant.compareTo(PAULSCODE_TIME) > 0) {
	// 		version.put("libraries", libraries);
	// 	} else {
	// 		version.put("libraries", librariesNoSoundLib);
	// 	}
	// 	for (String key : preset_launchwrapper.keySet()) {
	// 		if (key.equals("libraries")) {
	// 			continue;
	// 		}
	// 		version.put(key, preset_launchwrapper.get(key));
	// 	}
	// 	String args = "--username ${auth_player_name} --sessionid ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}";
	// 	// int port = getPort(releaseTimeInstant, id);
	// 	// if (port != -1) {
	// 	// 	args += " --resourcesProxyPort " + port;
	// 	// }
	// 	String skin = getSkin(releaseTimeInstant, id);
	// 	if (skin != null) {
	// 		args += " --skinProxy " + skin;
	// 	}
	// 	version.put("minecraftArguments", args);
	// 	return version;
	// }
}
