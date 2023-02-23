package lbq.jsongen;

import static lbq.jsongen.JSONConstants.PAULSCODE_TIME;
import static lbq.jsongen.JSONConstants.getPort;
import static lbq.jsongen.JSONConstants.getSkin;
import static lbq.jsongen.JSONUtil.getLibraryArtifact;
import static lbq.jsongen.JSONUtil.getManifest;
import static lbq.jsongen.JSONUtil.getPresetJSON;
import static lbq.jsongen.JSONUtil.getTime;
import static lbq.jsongen.JSONUtil.getTimeString;
import static lbq.jsongen.JSONUtil.parseJSON;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Generator {

	private final Path basePath;
	private JSONObject wrapperArtifact;
	private JSONObject wrapperArtifactSource;
	private boolean skipManifest;
	private boolean update;
	private boolean packToFolders;

	public Generator(Path dir, Path wrapperJar, Path wrapperSource, boolean updateJsons, boolean skipMan, boolean packToFolder) {
		basePath = dir;
		skipManifest = skipMan;
		update = updateJsons;
		packToFolders = packToFolder;
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
	}

	public void update() throws IOException {
		Files.list(basePath).forEach(p -> {
			String fileName = p.getFileName().toString();
			if(!Files.isRegularFile(p) || !fileName.endsWith(".json")) {
				return;
			}
			try {
				JSONObject json = parseJSON(p);
				JSONObject preset = getPresetJSON();
				JSONArray libraries = preset.getJSONArray("libraries");
				if(wrapperArtifact != null) {
					for(int i = libraries.length() - 1; i >= 0; i--) {
						JSONObject obj = libraries.getJSONObject(i);
						JSONObject downloads = obj.getJSONObject("downloads");
						if(obj.getString("name").equals("org.mcphackers:launchwrapper:1.0")) {
							downloads.put("artifact", wrapperArtifact);
							JSONObject classifiers = new JSONObject();
							classifiers.put("sources", wrapperArtifactSource);
							downloads.put("classifiers", classifiers);
						}
					}
				}
				JSONArray librariesNoSoundLib = new JSONArray(libraries);
				for(int i = librariesNoSoundLib.length() - 1; i >= 0; i--) {
					JSONObject obj = librariesNoSoundLib.getJSONObject(i);
					if(obj.getString("name").startsWith("com.paulscode:")) {
						librariesNoSoundLib.remove(i);
					}
				}
				boolean updated = false;
				JSONArray verLibs = json.getJSONArray("libraries");
				for(int i2 = verLibs.length() - 1; i2 >= 0; i2--) {
					//TODO change how libraries are removed
					JSONObject lib = verLibs.getJSONObject(i2);
					if(lib.getString("name").contains("lwjgl")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("jinput")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("jutils")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("paulscode")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("asm")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("org.json")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("launchwrapper")) {
						verLibs.remove(i2);
						continue;
					}
				}
				Instant time = getTime(json.getString("releaseTime"));
				updated = true; //TODO detect library changes?
				if(time.compareTo(PAULSCODE_TIME) > 0) {
					verLibs.putAll(libraries);
				} else {
					json.put("libraries", librariesNoSoundLib);
				}
				String args = json.getString("minecraftArguments");
				Instant releaseTimeInstant = getTime(json.getString("releaseTime"));
				int port = getPort(releaseTimeInstant);
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
				String skin = getSkin(releaseTimeInstant);
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
				json.put("minecraftArguments", args);
				if(updated) {
					json.put("time", getTimeString(Instant.now()));
				}
				try(BufferedWriter writer = Files.newBufferedWriter(p)) {
					json.write(writer);
				}
				String id = json.getString("id");
				if(packToFolders) {
					Files.copy(p, Files.createDirectory(basePath.resolve(id)).resolve(id + ".json"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void generateJSONs() throws IOException {
		JSONObject manifest = getManifest();
		JSONObject preset = getPresetJSON();
		JSONArray libraries = preset.getJSONArray("libraries");
		if(wrapperArtifact != null) {
			for(int i = libraries.length() - 1; i >= 0; i--) {
				JSONObject obj = libraries.getJSONObject(i);
				if(obj.getString("name").equals("org.mcphackers:launchwrapper:1.0")) {
					obj.getJSONObject("downloads").put("artifact", wrapperArtifact);
				}
			}
		}
		JSONArray librariesNoSoundLib = new JSONArray(libraries);
		
		Instant soundLibTime = PAULSCODE_TIME;
		for(int i = librariesNoSoundLib.length() - 1; i >= 0; i--) {
			JSONObject obj = librariesNoSoundLib.getJSONObject(i);
			if(obj.getString("name").startsWith("com.paulscode:")) {
				librariesNoSoundLib.remove(i);
			}
		}
		Files.createDirectories(basePath);
		List<String> savedJSONs = new ArrayList<>();
//		if(!skipCustom) {
//			JSONArray versionsList = Util.parseJSONArray(ClassLoader.getSystemResourceAsStream("versions.json"));
//			for(int i = 0; i < versionsList.length(); i++) {
//				JSONObject ver = versionsList.getJSONObject(i);
//				String id = ver.getString("id");
//				savedJSONs.add(id);
//				System.out.println(id);
//				Instant time = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(ver.getString("releaseTime")));
//				String clientURL = ver.optString("urlClient", null);
//				String serverURL = ver.optString("urlServer", null);
//				JSONObject version = createVersionJSON(id, clientURL, serverURL, DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(time).replace("Z", "+00:00"));
//				try(BufferedWriter writer = Files.newBufferedWriter(versionsDir.resolve(id + ".json"))) {
//					version.write(writer);
//				}
//			}
//		}
		if(!skipManifest) {
			JSONArray versions = manifest.getJSONArray("versions");
			for(int i = 0; i < versions.length(); i++) {
				JSONObject ver = versions.getJSONObject(i);
				URL versionUrl = new URL(ver.getString("url"));
				String id = ver.getString("id");
				if(savedJSONs.contains(id)) {
					continue;
				}
				if(!id.startsWith("1.7") && !id.startsWith("1.6") && !id.startsWith("13w")) {
					continue;
				}
				System.out.println(id);
				JSONObject version = parseJSON(versionUrl.openStream());
				if(!version.has("minecraftArguments")) {
					continue;
				}
				JSONArray verLibs = version.getJSONArray("libraries");
				for(int i2 = verLibs.length() - 1; i2 >= 0; i2--) {
					JSONObject lib = verLibs.getJSONObject(i2);
					if(lib.getString("name").contains("lwjgl")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("jinput")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("jutils")) {
						verLibs.remove(i2);
						continue;
					}
					if(lib.getString("name").contains("paulscode")) {
						verLibs.remove(i2);
						continue;
					}
				}
				Instant time = getTime(version.getString("releaseTime"));
				if(time.compareTo(soundLibTime) > 0) {
					verLibs.putAll(libraries);
				} else {
					verLibs.putAll(librariesNoSoundLib);
				}
				for(String key : preset.keySet()) {
					if(key.equals("libraries")) {
						continue;
					}
					version.put(key, preset.get(key));
				}
				String args = "--username ${auth_player_name} --session ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}";
				int port = getPort(time);
				if(port != -1) {
					args += " --resourcesProxyPort " + port;
				}
				String skin = getSkin(time);
				if(skin != null) {
					args += " --skinProxy " + skin;
				}
				version.put("minecraftArguments", args);
				Path jsonOut = basePath.resolve(id + ".json");
				try(BufferedWriter writer = Files.newBufferedWriter(jsonOut)) {
					version.write(writer);
				}
				if(packToFolders) {
					Files.copy(jsonOut, Files.createDirectory(basePath.resolve(id)).resolve(id + ".json"));
				}
			}
		}
	}

	public JSONObject createVersionJSON(String id, String clientURL, String serverURL, String releaseTime) throws IOException {
		JSONObject version = new JSONObject();
		JSONObject preset = getPresetJSON();
		JSONArray libraries = preset.getJSONArray("libraries");
		if(wrapperArtifact != null) {
			for(int i = libraries.length() - 1; i >= 0; i--) {
				JSONObject obj = libraries.getJSONObject(i);
				if(obj.getString("name").equals("org.mcphackers:launchwrapper:1.0")) {
					obj.getJSONObject("downloads").put("artifact", wrapperArtifact);
				}
			}
		}
		JSONArray librariesNoSoundLib = new JSONArray(libraries);
		
		for(int i = librariesNoSoundLib.length() - 1; i >= 0; i--) {
			JSONObject obj = librariesNoSoundLib.getJSONObject(i);
			if(obj.getString("name").startsWith("com.paulscode:")) {
				librariesNoSoundLib.remove(i);
			}
		}
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
		version.put("time", getTimeString(Instant.now()));
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
		int port = getPort(releaseTimeInstant);
		if(port != -1) {
			args += " --resourcesProxyPort " + port;
		}
		String skin = getSkin(releaseTimeInstant);
		if(skin != null) {
			args += " --skinProxy " + skin;
		}
		version.put("minecraftArguments", args);
		return version;
	}
}
