package lbq.jsongen;

import static lbq.jsongen.JSONConstants.*;
import static lbq.jsongen.JSONUtil.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {

	private final Path basePath;
	private JSONObject wrapperArtifact;
	private JSONObject wrapperArtifactSource;
	private boolean generateManifest;
	private boolean skipManifest;
	private boolean update;
	private boolean packToFolders;
	private Instant startTime;

	public Generator(Path dir, Path wrapperJar, Path wrapperSource, boolean updateJsons, boolean skipMan, boolean packToFolder, boolean genManifest) {
		basePath = dir;
		skipManifest = skipMan;
		generateManifest = genManifest;
		update = updateJsons;
		packToFolders = packToFolder;
		startTime = Instant.now();
		try {
			if(wrapperJar != null) {
				wrapperArtifact = getLibraryArtifact(wrapperJar, "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar");
				wrapperArtifact.put("path", "org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar");
			}
			if(wrapperSource != null) {
				wrapperArtifactSource = getLibraryArtifact(wrapperSource, "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0-sources.jar");
				wrapperArtifactSource.put("path", "org/mcphackers/launchwrapper/1.0/launchwrapper-1.0-sources.jar");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void generate() throws IOException {
		if(update) {
			update();
		} else {
			generateJSONs();
		}
		if(generateManifest) {
			generateManifest(false);
			generateManifest(true);
		}
	}

	public void update() throws IOException {
		Map<String, String> versionServers = new HashMap<>();
		JSONArray versionsIndex = parseJSONArray(ClassLoader.getSystemResourceAsStream("versions.json"));
		for(int i = 0; i < versionsIndex.length(); i++) {
			JSONObject ver = versionsIndex.getJSONObject(i);
			if(ver.has("urlServer")) {
				versionServers.put(ver.getString("id"), ver.getString("urlServer"));
			}
		}
		JSONObject preset = getPresetJSON();
		JSONArray libraries = getPresetLibraries(preset);
		JSONArray librariesNoSoundLib = removePaulscode(libraries);
		for(Path p : collectJSONs(basePath)) {
			try {
				JSONObject json = parseJSON(p);
				boolean updated = false;
				Instant time = getTime(json.getString("releaseTime"));
				String id = json.getString("id");
				if(!hasAssetIndex(time, id)) {
					json.put("assetIndex", preset.getJSONObject("assetIndex"));
					json.put("assets", preset.getString("assets"));
				}
				JSONArray verLibs = json.getJSONArray("libraries");
				if(time.compareTo(PAULSCODE_TIME) > 0) {
					for(int i2 = 0; i2 < libraries.length(); i2++) {
						updated |= replaceLibrary(verLibs, libraries.getJSONObject(i2));
					}
				} else {
					for(int i2 = 0; i2 < librariesNoSoundLib.length(); i2++) {
						updated |= replaceLibrary(verLibs, librariesNoSoundLib.getJSONObject(i2));
					}
				}
				String args = json.getString("minecraftArguments");
				Instant releaseTimeInstant = getTime(json.getString("releaseTime"));
				int port = getPort(releaseTimeInstant, id);
				if(port != -1) {
					if(args.contains("--resourcesProxyPort")) {
						String old = args;
						args = args.replaceAll("--resourcesProxyPort \\S+", "--resourcesProxyPort " + port);
						updated |= !old.equals(args);
					} else {
						args += " --resourcesProxyPort " + port;
						updated = true;
					}
				}
				String skin = getSkin(releaseTimeInstant, id);
				if(skin != null) {
					if(args.contains("--skinProxy")) {
						String old = args;
						args = args.replaceAll("--skinProxy \\S+", "--skinProxy " + skin);
						updated |= !old.equals(args);
					} else {
						args += " --skinProxy " + skin;
						updated = true;
					}
				}
				JSONObject dls = json.getJSONObject("downloads");
				if(!dls.has("server") && versionServers.containsKey(id)) {
					updated = true;
					dls.put("server", getLibraryArtifact(versionServers.get(id)));
				}
				json.put("minecraftArguments", args);
				if(updated) {
					System.out.println("Modified version: " + id);
					json.put("time", getTimeString(startTime));
					try(BufferedWriter writer = Files.newBufferedWriter(p)) {
						json.write(writer);
					}
				} else {
					//System.out.println("Unmodified version: " + id);
				}
				if(packToFolders) {
					Path outPath = Files.createDirectory(basePath.resolve(id)).resolve(id + ".json");
					Files.deleteIfExists(outPath);
					Files.copy(p, outPath);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public JSONArray getPresetLibraries(JSONObject preset) throws IOException {
		JSONArray libraries = preset.getJSONArray("libraries");
		if(wrapperArtifact != null) {
			for(int i = libraries.length() - 1; i >= 0; i--) {
				JSONObject obj = libraries.getJSONObject(i);
				String[] libraryName = obj.getString("name").split(":");
				if(libraryName[0].equals("org.mcphackers") && libraryName[1].equals("launchwrapper")) {
					obj.getJSONObject("downloads").put("artifact", wrapperArtifact);
				}
			}
		}
		return libraries;
	}

	public void generateJSONs() throws IOException {
		JSONObject manifest = getManifest();
		JSONObject preset = getPresetJSON();
		JSONArray libraries = getPresetLibraries(preset);
		JSONArray librariesNoSoundLib = removePaulscode(libraries);
		JSONObject legacyAssetIndex = new JSONObject();
		legacyAssetIndex.put("id", "legacy");
		legacyAssetIndex.put("sha1", "770572e819335b6c0a053f8378ad88eda189fc14");
		legacyAssetIndex.put("size", 109634);
		legacyAssetIndex.put("totalSize", 153475165);
		legacyAssetIndex.put("url", "https://launchermeta.mojang.com/v1/packages/770572e819335b6c0a053f8378ad88eda189fc14/legacy.json");
		JSONObject authLib156 = new JSONObject();
		JSONObject authLib156Downloads = new JSONObject();
		JSONObject artifactAuthLib156 = getLibraryArtifact("https://libraries.minecraft.net/com/mojang/authlib/1.5.6/authlib-1.5.6.jar");
		artifactAuthLib156.put("path", "com/mojang/authlib/1.5.6/authlib-1.5.6.jar");
		authLib156Downloads.put("artifact", artifactAuthLib156);
		authLib156.put("downloads", authLib156Downloads);
		authLib156.put("name", "com.mojang:authlib:1.5.6");

		Files.createDirectories(basePath);
		List<String> savedJSONs = new ArrayList<>();
		if(!skipManifest) {
			try {
				JSONArray versions = manifest.getJSONArray("versions");
				for(int i = 0; i < versions.length(); i++) {
					JSONObject ver = versions.getJSONObject(i);
					URL versionUrl = new URL(ver.getString("url"));
					String id = ver.getString("id");
					if(savedJSONs.contains(id)) {
						continue;
					}
					System.out.println(id);
					JSONObject version = parseJSON(versionUrl.openStream());
					if(!version.has("minecraftArguments")) {
						continue;
					}
					JSONArray verLibs = version.getJSONArray("libraries");
					boolean hasWrapper = false;
					for(int i2 = 0; i2 < verLibs.length(); i2++) {
						String[] lib = verLibs.getJSONObject(i2).getString("name").split(":");
						if(lib[0].equals("net.minecraft") && lib[1].equals("launchwrapper")) {
							hasWrapper = true;
						}
					}
					if(hasWrapper) {
						removeLibrary(verLibs, "org.ow2.asm", "asm-all");
						removeLibrary(verLibs, "net.sf.jopt-simple", "jopt-simple");
						removeLibrary(verLibs, "net.minecraft", "launchwrapper");
					}
					Instant time = getTime(version.getString("releaseTime"));
					if(time.compareTo(PAULSCODE_TIME) > 0) {
						for(int i2 = 0; i2 < libraries.length(); i2++) {
							replaceLibrary(verLibs, libraries.getJSONObject(i2));
						}
					} else {
						for(int i2 = 0; i2 < librariesNoSoundLib.length(); i2++) {
							replaceLibrary(verLibs, librariesNoSoundLib.getJSONObject(i2));
						}
					}
					if(id.equals("1.7.6-pre1") || id.equals("1.7.6-pre2") || id.equals("1.7.7")) {
						replaceLibrary(verLibs, authLib156);
					}
					boolean keepAssetsIndex = hasAssetIndex(time, id);
					if(keepAssetsIndex) {
						if(version.getString("assets").equals("pre-1.6")) {
							version.put("assets", "legacy");
							version.put("assetIndex", legacyAssetIndex); 
						}
					}
					for(String key : preset.keySet()) {
						if(keepAssetsIndex) {
							if(key.equals("assets") || key.equals("assetIndex")) {
								continue;
							}
						}
						if(key.equals("libraries")) {
							continue;
						}
						version.put(key, preset.get(key));
					}
					String args = version.getString("minecraftArguments");
					args = args.replace("${auth_player_name} ${auth_session}", "--username ${auth_player_name} --session ${auth_session}");
					List<String> argsList = Arrays.asList(version.getString("minecraftArguments").split(" "));
					if(!argsList.contains("--gameDir")) {
						args += " --gameDir ${game_directory}";
					}
					if(!argsList.contains("--assetsDir")) {
						args += " --assetsDir ${game_assets}";
					}
					int port = getPort(time, id);
					if(port != -1) {
						args += " --resourcesProxyPort " + port;
					}
					String skin = getSkin(time, id);
					if(skin != null) {
						args += " --skinProxy " + skin;
					}
					version.put("time", getTimeString(startTime));
					version.put("minecraftArguments", args);
					Path jsonOut = basePath.resolve(id + ".json");
					try(BufferedWriter writer = Files.newBufferedWriter(jsonOut)) {
						version.write(writer);
					}
					if(packToFolders) {
						Path outPath = Files.createDirectory(basePath.resolve(id)).resolve(id + ".json");
						Files.deleteIfExists(outPath);
						Files.copy(jsonOut, outPath);
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private JSONArray removePaulscode(JSONArray libraries) {
		JSONArray librariesNoSoundLib = new JSONArray(libraries);
		for(int i = librariesNoSoundLib.length() - 1; i >= 0; i--) {
			JSONObject obj = librariesNoSoundLib.getJSONObject(i);
			String[] libraryName = obj.getString("name").split(":");
			if(libraryName[0].equals("com.paulscode")) {
				librariesNoSoundLib.remove(i);
			}
		}
		return librariesNoSoundLib;
	}
	
	public void generateManifest(boolean v2) throws IOException {
		//TODO v2 manifest
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
		for(Path p : collectJSONs(basePath)) {
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
				if(v2) {
					version.put("sha1", Util.getSHA1(Files.newInputStream(p)));
					version.put("complianceLevel", 0);
				}
				versionsList.add(version);
				if(type.equals("release")) {
					Instant time = getTime(releaseTime);
					if(latestReleaseTime == null || time.compareTo(latestReleaseTime) > 0) {
						latestReleaseTime = time;
						latestRelease = id;
					}
				} else if(type.equals("snapshot")) {
					Instant time = getTime(releaseTime);
					if(latestSnapshotTime == null || time.compareTo(latestSnapshotTime) > 0) {
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
		try(BufferedWriter writer = Files.newBufferedWriter(basePath.getParent().resolve(v2 ? "version_manifest_v2.json" : "version_manifest.json"))) {
			manifest.write(writer);
		}
	}
	
	public static List<Path> collectJSONs(Path dir) throws IOException {
		return Files.list(dir).filter(p -> (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))).collect(Collectors.toList());
	}

	public JSONObject createVersionJSON(String id, String clientURL, String serverURL, String releaseTime) throws IOException {
		JSONObject version = new JSONObject();
		JSONObject preset = getPresetJSON();
		JSONArray libraries = getPresetLibraries(preset);
		JSONArray librariesNoSoundLib = removePaulscode(libraries);
		JSONObject downloads = new JSONObject();
		version.put("downloads", downloads);
		if(clientURL != null) {
			JSONObject client = new JSONObject();
			byte[] jar = Util.readAllBytes(new URL(clientURL).openStream());
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			client.put("url", clientURL);
			client.put("size", jar.length);
			client.put("sha1", sha1);
			
			downloads.put("client", client);
		}
		if(serverURL != null) {
			JSONObject server = new JSONObject();
			byte[] jar = Util.readAllBytes(new URL(serverURL).openStream());
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			server.put("url", serverURL);
			server.put("size", jar.length);
			server.put("sha1", sha1);
			
			downloads.put("server", server);
		}
		version.put("id", id);
		if(id.startsWith("a") || id.startsWith("c") || id.startsWith("in")) {
			version.put("type", "old_alpha");
		}
		else if(id.startsWith("b")) {
			version.put("type", "old_beta");
		}
		else if(id.charAt(2) == 'w' || id.equals("1.2")) {
			version.put("type", "snapshot");
		} else {
			version.put("type", "release");
		}
		version.put("releaseTime", releaseTime);
		version.put("time", getTimeString(startTime));
		Instant releaseTimeInstant = getTime(releaseTime);
		if(releaseTimeInstant.compareTo(PAULSCODE_TIME) > 0) {
			version.put("libraries", libraries);
		} else {
			version.put("libraries", librariesNoSoundLib);
		}
		for(String key : preset.keySet()) {
			if(key.equals("libraries")) {
				continue;
			}
			version.put(key, preset.get(key));
		}
		String args = "--username ${auth_player_name} --sessionid ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}";
		int port = getPort(releaseTimeInstant, id);
		if(port != -1) {
			args += " --resourcesProxyPort " + port;
		}
		String skin = getSkin(releaseTimeInstant, id);
		if(skin != null) {
			args += " --skinProxy " + skin;
		}
		version.put("minecraftArguments", args);
		return version;
	}
}
