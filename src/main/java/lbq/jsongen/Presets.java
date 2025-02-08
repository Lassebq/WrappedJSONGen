package lbq.jsongen;

import java.io.IOException;

import org.json.JSONObject;

public class Presets {
    
	public static JSONObject getPresetJSON(String presetName) {
		try {
			return JSONUtil.parseJSON(ClassLoader.getSystemResourceAsStream("preset_" + presetName + ".json"));
		}
		catch (IOException e) {
			return null;
		}
	}
}
