package lbq.jsongen;

import static lbq.jsongen.JSONUtil.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

public class GenArtifact {
	public static void main(String[] args) throws IOException {
		String prefab_name = "preset_launchwrapper";
		JSONObject json = JSONUtil.parseJSON(Paths.get("src/main/resources/" + prefab_name + "_prefab.json"));
		Path out = Paths.get("src/main/resources/" + prefab_name + ".json");
		JSONArray libraries = json.getJSONArray("libraries");
		JSONArray newLibraries = new JSONArray();
		for(Object o : libraries) {
			JSONObject lib = (JSONObject)o;
			JSONObject downloads = new JSONObject();
			JSONObject classifiersNew = null;
			JSONObject classifiers = null;
			String urlBase = lib.optString("url", null);
			String name = lib.getString("name");
			String url = urlBase == null ? null : getArtifactURL(urlBase, name, null);
			if(lib.has("downloads")) {
				JSONObject downloadsLib = lib.getJSONObject("downloads");
				if(downloadsLib.has("artifact")) {
					url = downloadsLib.getJSONObject("artifact").optString("url", url);
				}
				if(downloadsLib.has("classifiers")) {
					classifiers = downloadsLib.getJSONObject("classifiers");
				}
			}
			if(url != null) {
				JSONObject artifact = getArtifact(url, getArtifactPath(name, null), true);
				downloads.put("artifact", artifact);
			}
			if(classifiers != null) {
				classifiersNew = new JSONObject();
				for(String key : classifiers.keySet()) {
					JSONObject classifierArtifact = classifiers.getJSONObject(key);
					url = classifierArtifact.optString("url", getArtifactURL(urlBase, name, key));
					classifiersNew.put(key, getArtifact(url, getArtifactPath(name, key), true));
				}
				downloads.put("classifiers", classifiersNew);
			}
			lib.put("downloads", downloads);
			newLibraries.put(lib);
		}
		json.put("libraries", newLibraries);
		try (BufferedWriter writer = Files.newBufferedWriter(out)) {
			json.write(writer);
		}
	}
}
