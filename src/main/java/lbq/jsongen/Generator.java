package lbq.jsongen;

import static lbq.jsongen.JSONConstants.*;
import static lbq.jsongen.JSONUtil.*;
import static lbq.jsongen.Util.*;

import java.io.IOException;
import java.net.URL;
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
	private Instant startTime;
	private String postfix;
	private String version;

	private static JSONObject manifest;

	public Generator(Path dir, boolean updateJsons, boolean packToFolder, boolean genManifest, String postfixString, String onlyVersion, boolean lwjgl3, boolean mm) {
		basePath = dir;
		generateManifest = genManifest;
		update = updateJsons;
		packToFolders = packToFolder;
		startTime = Instant.now();
		postfix = postfixString;
		version = onlyVersion;
		lwjglCompat = lwjgl3;
		micromixin = mm;
	}

	public void generate() throws IOException {
		if (update) {
			Files.createDirectories(basePath);
			for (Path p : collectJSONs(basePath)) {
				JSONObject jobj = parseJSON(p);
				String id = jobj.getString("id");
				if(version != null && !version.equals(id)) {
					continue;
				}
				boolean updated = update(jobj);
				if(updated)
					System.out.println("Modified version: " + id);
				else {
					System.out.println("Unmodified version: " + id);
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
			JSONObject preset_paulscode = getPreset("paulscode");
			updated |= mergePreset(preset_paulscode, json);
		}
		if (time.compareTo(LWJGL2_TIME) > 0) {
			JSONObject preset_lwjgl3 = getPreset("lwjgl3");
			updated |= mergePreset(preset_lwjgl3, json);
		} else {
			if (lwjglCompat) {
				JSONObject preset_lwjgl3 = getPreset("lwjgl3");
				JSONObject preset_lwjgl3compat = getPreset("lwjgl3compat");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl_util");
				updated |= removeLibrary(verLibs, "org.lwjgl.lwjgl", "lwjgl-platform");
				updated |= mergePreset(preset_lwjgl3, json);
				updated |= mergePreset(preset_lwjgl3compat, json);
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
		// TODO update LW base json to use arguments array instead (NOTE: MMC doesn't support it)
		if(json.has("arguments")) {
			json.remove("arguments");
			updated = true;
		}
		JSONObject preset_launchwrapper = getPreset("launchwrapper");
		updated |= mergePreset(preset_launchwrapper, json);
		if(micromixin) {
			JSONObject preset_launchwrapper_micromixin = getPreset("launchwrapper_micromixin");
			updated |= mergePreset(preset_launchwrapper_micromixin, json);
		}
		String minecraftArguments = json.getString("minecraftArguments");
		List<String> argsList = new ArrayList<>();
		argsList.addAll(Arrays.asList(minecraftArguments.split(" ")));
		if(!argsList.contains("--skinProxy")) {
			String skin = getSkin(releaseTimeInstant, id);
			if (skin != null) {
				argsList.add("--skinProxy");
				argsList.add(skin);
				updated = true;
				json.put("minecraftArguments", String.join(" ", argsList));
			}
		}
		// if (id.equals("1.7.6-pre1") || id.equals("1.7.6-pre2") || id.equals("1.7.7")) {
		// 	JSONObject authLib156 = generateLibraryEntry("https://libraries.minecraft.net/", "com.mojang:authlib:1.5.6");
		// 	updated |= replaceLibrary(verLibs, authLib156);
		// }

		if (updated) {
			json.put("time", getTimeString(startTime));
		}
		return updated;
	}

	public void generateJSONs() throws IOException {
		Set<String> savedJSONs = new HashSet<>();
		JSONArray versionsArr = manifest.getJSONArray("versions");
		for(int i = 0; i < versionsArr.length(); i++) {
			JSONObject ver = versionsArr.getJSONObject(i);
			try {
				String versionUrl = ver.getString("url");
				if (version != null && !ver.getString("id").equals(version)) {
					continue;
				}
				String id = ver.getString("id") + (postfix == null ? "" : postfix);
				if (savedJSONs.contains(id)) {
					continue;
				}
				JSONObject versionJ = fetchVersion(versionUrl);
				System.out.print(id + "..");
				if (versionJ == null /*|| !version.has("minecraftArguments")*/) {
					System.out.println("skipped");
					continue;
				}
				update(versionJ);
				System.out.println("done");
				Path jsonOut = basePath.resolve(id + ".json");
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

	public static JSONObject getPreset(String name) {
		JSONObject preset = Presets.getPresetJSON(name);
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
