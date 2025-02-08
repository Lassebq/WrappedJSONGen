package lbq.jsongen;

import static lbq.jsongen.JSONUtil.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

public class MultiMCInstall {

    public static void main(String[] args) throws IOException {
		if(args.length == 0) {
			System.out.println("Available parameters:");
			System.out.println("--path [path] - Instance path");
			System.out.println("--lwjgl3 - Use lwjgl3 compat layer if possible");
			System.out.println("--micromixin - Use Micromixin");
			return;
		}
        Path basePath = null;
        boolean lwjglCompat = false;
        boolean micromixin = false;
        boolean fabric = false;
        boolean lwjgl3 = false;
        for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(!arg.startsWith("--")) {
				continue;
			}
			String argName = arg.substring(2);
			boolean hasNext = i + 1 < args.length;
			if(hasNext && argName.equalsIgnoreCase("path")) {
				i++;
				basePath = Paths.get(args[i]);
			}
            if(argName.equalsIgnoreCase("micromixin")) {
                micromixin = true;
            }
            if(argName.equalsIgnoreCase("lwjgl3")) {
                lwjglCompat = true;
            }
        }
        if(basePath == null) {
			System.out.println("--path is required");
            return;
        }
        Files.createDirectories(basePath.resolve("patches"));
        Path lwjglJson = basePath.resolve("patches/org.lwjgl.json");
        Path lwJson = basePath.resolve("patches/org.mcphackers.launchwrapper.json");
        Path lwmmJson = basePath.resolve("patches/org.mcphackers.launchwrapper-micromixin.json");
        Path paulscode = basePath.resolve("patches/com.paulscode.json");
        Path minecraft = basePath.resolve("patches/net.minecraft.json");
        JSONObject mc = parseJSON(minecraft);
        JSONArray requires = mc.optJSONArray("requires");
        if(requires != null) {
            for(int i = 0; i < requires.length(); i++) {
                JSONObject obj = requires.getJSONObject(i);
                if(obj.optString("uid").equals("org.lwjgl3")) {
                    lwjgl3 = true;
                    break;
                }
            }
        }
        String version = mc.optString("version");
        
        JSONObject mmcPack = parseJSON(basePath.resolve("mmc-pack.json"));
        JSONArray components = mmcPack.getJSONArray("components");

        if(lwjgl3) {
            JSONObject lwjgl3Preset = getPreset("lwjgl3");
            addMultiMCComponent(components, lwjgl3Preset);
            writeJSON(lwjgl3Preset, lwjglJson);
        } else {
            if(lwjglCompat) {
                JSONObject lwjgl3Compat = getPreset("lwjgl3compat");
                addMultiMCComponent(components, lwjgl3Compat);
                writeJSON(lwjgl3Compat, lwjglJson);
            } else {
                JSONObject lwjgl2 = getPreset("lwjgl2");
                addMultiMCComponent(components, lwjgl2);
                writeJSON(lwjgl2, lwjglJson);
            }
        }
        JSONObject launchwrapper = getPreset("launchwrapper");
        addMultiMCComponent(components, launchwrapper);
        writeJSON(launchwrapper, lwJson);
        if(micromixin) {
            JSONObject launchwrapper_micromixin = getPreset("launchwrapper_micromixin");
            addMultiMCComponent(components, launchwrapper_micromixin);
            writeJSON(launchwrapper_micromixin, lwmmJson);
        }
        JSONObject paulscodePreset = getPreset("paulscode");
        addMultiMCComponent(components, paulscodePreset);
        writeJSON(paulscodePreset, paulscode);
        writeJSON(mmcPack, basePath.resolve("mmc-pack.json"));
    }

    public static JSONObject getPreset(String name) {
        JSONObject preset = Presets.getPresetJSON(name);
        JSONArray libs = preset.optJSONArray("libraries");
        if(libs != null) {
            for(int i = 0; i < libs.length(); i++) {
                JSONObject obj = libs.getJSONObject(i);
                if(!obj.has("downloads") && !obj.has("url")) {
                    obj.put("downloads", new JSONObject());
                }
            }
        }
        return preset;
    }

	public static void addMultiMCComponent(JSONArray components, JSONObject preset) {
		JSONObject component = new JSONObject();
		component.put("uid", preset.getString("uid"));
		components.put(component);
	}
    
}
