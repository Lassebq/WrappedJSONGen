package lbq.versionjsons;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class VersionsWriter {

	private final Path versionsDir;
	private JSONObject wrapperArtifact;
	private boolean skipManifest;

	public VersionsWriter(Path dir, Path wrapperJar) {
		versionsDir = dir;
		try {
			if(wrapperJar != null) {
				wrapperArtifact = getLibraryArtifact(wrapperJar, "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar");
				wrapperArtifact.getJSONObject("artifact").put("path", "org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final String PAULSCODE_TIME = "2009-12-21T22:00:00+00:00";

	public void generateJSONs() throws IOException {
		JSONObject manifest = getManifest();
		JSONObject preset = getPresetJSON();
		JSONArray libraries = preset.getJSONArray("libraries");
		if(wrapperArtifact != null) {
			for(int i = libraries.length() - 1; i >= 0; i--) {
				JSONObject obj = libraries.getJSONObject(i);
				if(obj.getString("name").equals("org.mcphackers:launchwrapper:1.0")) {
					obj.put("downloads", wrapperArtifact);
				}
			}
		}
		JSONArray librariesNoSoundLib = new JSONArray(libraries);
		
		Instant soundLibTime = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(PAULSCODE_TIME));
		for(int i = librariesNoSoundLib.length() - 1; i >= 0; i--) {
			JSONObject obj = librariesNoSoundLib.getJSONObject(i);
			if(obj.getString("name").startsWith("com.paulscode:")) {
				librariesNoSoundLib.remove(i);
			}
		}
		List<String> savedJSONs = new ArrayList<>();
		JSONArray versionsList = Util.parseJSONArray(ClassLoader.getSystemResourceAsStream("versions.json"));
		for(int i = 0; i < versionsList.length(); i++) {
			JSONObject ver = versionsList.getJSONObject(i);
			String id = ver.getString("id");
			savedJSONs.add(id);
			System.out.println(id);
			Instant time = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(ver.getString("releaseTime")));
			String clientURL = ver.optString("urlClient", null);
			String serverURL = ver.optString("urlServer", null);
			JSONObject version = createVersionJSON(id, clientURL, serverURL, DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(time).replace("Z", "+00:00"));
			try(BufferedWriter writer = Files.newBufferedWriter(versionsDir.resolve(id + ".json"))) {
				version.write(writer);
			}
		}
		if(!skipManifest) {
			JSONArray versions = manifest.getJSONArray("versions");
			Files.createDirectories(versionsDir);
			for(int i = 0; i < versions.length(); i++) {
				JSONObject ver = versions.getJSONObject(i);
				URL versionUrl = new URL(ver.getString("url"));
				String id = ver.getString("id");
				if(savedJSONs.contains(id)) {
					continue;
				}
				System.out.println(id);
				JSONObject version = Util.parseJSON(versionUrl.openStream());
				if(!version.has("minecraftArguments")) {
					continue;
				}
				Instant time = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(version.getString("releaseTime")));
				if(time.compareTo(soundLibTime) > 0) {
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
				version.put("minecraftArguments", version.getString("minecraftArguments")
						.replace("${auth_player_name} ${auth_session}", "--username ${auth_player_name} --sessionid ${auth_session}"));
				try(BufferedWriter writer = Files.newBufferedWriter(versionsDir.resolve(id + ".json"))) {
					version.write(writer);
				}
			}
		}
	}

	public void packToFolders() throws IOException {
		Files.list(versionsDir).forEach(p -> {
			String fileName = p.getFileName().toString();
			if(!Files.isRegularFile(p) || !fileName.endsWith(".json")) {
				return;
			}
			try {
				Path dir = versionsDir.resolve(fileName.substring(0, fileName.lastIndexOf(".")));
				Files.createDirectory(dir);
				Files.copy(p, dir.resolve(fileName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public JSONObject createVersionJSON(String id, String clientURL, String serverURL, String releaseTime) throws IOException {
		JSONObject version = new JSONObject();
		JSONObject preset = getPresetJSON();
		JSONArray libraries = preset.getJSONArray("libraries");
		if(wrapperArtifact != null) {
			for(int i = libraries.length() - 1; i >= 0; i--) {
				JSONObject obj = libraries.getJSONObject(i);
				if(obj.getString("name").equals("org.mcphackers:launchwrapper:1.0")) {
					obj.put("downloads", wrapperArtifact);
				}
			}
		}
		JSONArray librariesNoSoundLib = new JSONArray(libraries);
		
		Instant soundLibInstant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(PAULSCODE_TIME));
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
		version.put("time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(Instant.now()).replace("Z", "+00:00"));
		Instant releaseTimeInstant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(releaseTime));
		if(releaseTimeInstant.compareTo(soundLibInstant) > 0) {
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
		version.put("minecraftArguments", "--username ${auth_player_name} --sessionid ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}");
		return version;
	}

	private JSONObject getPresetJSON() throws IOException {
		return Util.parseJSON(ClassLoader.getSystemResourceAsStream("preset.json"));
	}
	
	private static JSONObject getManifest() throws IOException {
		return new JSONObject(new String(Util.readAllBytes(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream())));
	}
	
	private static JSONObject getLibraryArtifact(Path path, String url) throws IOException {
		JSONObject obj = new JSONObject();
		JSONObject artifact = new JSONObject();
		obj.put("artifact", artifact);
		if(url != null) {
			byte[] jar = Util.readAllBytes(Files.newInputStream(path));
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			artifact.put("url", url);
			artifact.put("size", jar.length);
			artifact.put("sha1", sha1);
		}
		return obj;
	}

	public void skipManifest() {
		skipManifest = true;
	}
}
