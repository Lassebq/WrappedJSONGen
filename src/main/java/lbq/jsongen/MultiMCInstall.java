package lbq.jsongen;

import static lbq.jsongen.JSONUtil.*;
import static lbq.jsongen.Util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class MultiMCInstall {

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Available parameters:");
			System.out.println("--path [path] - Instance path");
			System.out.println("--lwjgl3 - Use lwjgl3 compat layer if possible");
			System.out.println("--micromixin - Use Micromixin");
			System.out.println("--version - Specify version if it cannot be detected in the instance");
			return;
		}
		Path basePath = null;
		boolean lwjglCompat = false;
		boolean micromixin = false;
		boolean fabric = false;
		boolean lwjgl3 = false;
		String id = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (!arg.startsWith("--")) {
				continue;
			}
			String argName = arg.substring(2);
			boolean hasNext = i + 1 < args.length;
			if (hasNext && argName.equalsIgnoreCase("path")) {
				i++;
				basePath = Paths.get(args[i]);
			}
			if (hasNext && argName.equalsIgnoreCase("version")) {
				i++;
				id = args[i];
			}
			if (argName.equalsIgnoreCase("micromixin")) {
				micromixin = true;
			}
			if (argName.equalsIgnoreCase("lwjgl3")) {
				lwjglCompat = true;
			}
		}
		if (basePath == null) {
			System.out.println("--path is required");
			return;
		}
		Files.createDirectories(basePath.resolve("patches"));
		Path lwjglJson = basePath.resolve("patches/org.lwjgl.json");
		Path lwJson = basePath.resolve("patches/org.mcphackers.launchwrapper.json");
		Path lwmmJson = basePath.resolve("patches/org.mcphackers.launchwrapper-micromixin.json");
		Path minecraft = basePath.resolve("patches/net.minecraft.json");
		Path paulscode = basePath.resolve("patches/com.paulscode.json");

		JSONObject mmcPack = parseJSON(basePath.resolve("mmc-pack.json"));
		JSONArray components = mmcPack.getJSONArray("components");
		JSONArray newComponents = new JSONArray();
		for (int i = 0; i < components.length(); i++) {
			if ("net.fabricmc.fabric-loader".equals(components.getJSONObject(i).optString("uid"))) {
				fabric = true;
			}
			if (id == null && "net.minecraft".equals(components.getJSONObject(i).optString("uid"))) {
				JSONObject mc = components.getJSONObject(i);
				id = mc.optString("cachedVersion");
				JSONArray requires = mc.optJSONArray("cachedRequires");
				if (requires != null) {
					for (int i2 = 0; i2 < requires.length(); i2++) {
						JSONObject obj = requires.getJSONObject(i2);
						if (obj.optString("uid").equals("org.lwjgl3")) {
							lwjgl3 = true;
							break;
						}
					}
				}
			}
		}

		if (lwjgl3) {
			JSONObject lwjgl3Preset = getPreset("lwjgl3");
			addMultiMCComponent(newComponents, "org.lwjgl3");
			writeJSON(lwjgl3Preset, lwjglJson);
		} else {
			if (lwjglCompat) {
				JSONObject lwjgl3Compat = getPreset("lwjgl3compat");
				addMultiMCComponent(newComponents, "org.lwjgl");
				JSONObject lwjgl3Component = addMultiMCComponent(newComponents, "org.lwjgl3");
				lwjgl3Component.put("dependencyOnly", true);
				lwjgl3Component.put("version", "3.3.3");
				writeJSON(lwjgl3Compat, lwjglJson);
			} else {
				JSONObject lwjgl2 = getPreset("lwjgl2");
				addMultiMCComponent(newComponents, "org.lwjgl");
				writeJSON(lwjgl2, lwjglJson);
			}
		}

		JSONObject verJson = null;
		JSONObject manifest = parseJSON(openStream("https://meta.omniarchive.uk/v1/manifest.json"));
		JSONArray versionsArr = manifest.getJSONArray("versions");
		JSONObject ver = null;
		Instant releaseTimeInstant = null;
		for (int i = 0; i < versionsArr.length(); i++) {
			ver = versionsArr.getJSONObject(i);
			if (ver.getString("id").equals(id)) {
				verJson = fetchVersion(ver.getString("url"));
				releaseTimeInstant = JSONUtil.getTime(ver.getString("releaseTime"));
				break;
			}
		}
		addMultiMCComponent(newComponents, "net.minecraft");
		if (verJson != null) {
			JSONArray libs = verJson.getJSONArray("libraries");
			removeLibrary(libs, "org.lwjgl.lwjgl", "lwjgl");
			removeLibrary(libs, "org.lwjgl.lwjgl", "lwjgl_util");
			removeLibrary(libs, "org.lwjgl.lwjgl", "lwjgl-platform");
			removeLibrary(libs, "net.java.jinput", "jinput");
			removeLibrary(libs, "net.java.jinput", "jinput-platform");
			removeLibrary(libs, "net.java.jutils", "jutils");
			verJson.put("uid", "net.minecraft");
			verJson.put("name", "Minecraft");
			verJson.put("version", id);
			writeJSON(verJson, minecraft);
		}
		newComponents.putAll(components);
		JSONObject launchwrapper = getPreset("launchwrapper");
		String minecraftArguments = launchwrapper.getString("minecraftArguments");
		List<String> argsList = new ArrayList<>();
		argsList.addAll(Arrays.asList(minecraftArguments.split(" ")));
		if (!argsList.contains("--skinProxy")) {
			String skin = JSONConstants.getSkin(releaseTimeInstant, id);
			if (skin != null) {
				argsList.add("--skinProxy");
				argsList.add(skin);
				launchwrapper.put("minecraftArguments", String.join(" ", argsList));
			}
		}

		addMultiMCComponent(newComponents, "org.mcphackers.launchwrapper");
		writeJSON(launchwrapper, lwJson);
		if (fabric) {
			JSONObject launchwrapperfabric = getPreset("launchwrapper_fabric");
			addMultiMCComponent(newComponents, "org.mcphackers.launchwrapper.fabric");
			writeJSON(launchwrapperfabric, lwmmJson);
		} else if (micromixin) {
			JSONObject launchwrapper_micromixin = getPreset("launchwrapper_micromixin");
			addMultiMCComponent(newComponents, "org.mcphackers.launchwrapper.micromixin");
			writeJSON(launchwrapper_micromixin, lwmmJson);
		}
		JSONObject paulscodePreset = getPreset("paulscode");
		addMultiMCComponent(newComponents, "com.paulscode");
		writeJSON(paulscodePreset, paulscode);
		mmcPack.put("components", newComponents);
		writeJSON(mmcPack, basePath.resolve("mmc-pack.json"));
	}

	public static JSONObject getPreset(String presetName) {
		JSONObject preset;
		try {
			preset = JSONUtil.parseJSON(ClassLoader.getSystemResourceAsStream("preset_" + presetName + ".json"));
		} catch (IOException e) {
			return null;
		}
		JSONArray libs = preset.optJSONArray("libraries");
		if (libs != null) {
			for (int i = 0; i < libs.length(); i++) {
				JSONObject obj = libs.getJSONObject(i);
				if (!obj.has("downloads") && !obj.has("url")) {
					obj.put("downloads", new JSONObject());
				}
			}
		}
		return preset;
	}

	public static JSONObject addMultiMCComponent(JSONArray components, String uid) {
		JSONObject component = new JSONObject();
		component.put("uid", uid);
		for (int j = components.length() - 1; j >= 0; j--) {
			if (components.getJSONObject(j).optString("uid").equals(uid)) {
				components.remove(j);
			}
		}
		components.put(component);
		return component;
	}

}
