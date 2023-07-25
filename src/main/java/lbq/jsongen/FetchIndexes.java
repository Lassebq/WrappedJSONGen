package lbq.jsongen;

import static lbq.jsongen.JSONUtil.fetchVersion;
import static lbq.jsongen.JSONUtil.getManifest;
import static lbq.jsongen.JSONUtil.parseJSON;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class FetchIndexes {
    public static void main(String[] args) throws IOException {
        JSONObject man;
        if(args.length >= 1) {
            Path manifest = Paths.get(args[0]);
            man = parseJSON(manifest);
        } else {
            man = getManifest();
        }
        List<String> uniqueIds = new ArrayList<>();

        JSONArray versions = man.getJSONArray("versions");
        for(int i = 0; i < versions.length(); i++) {
            JSONObject ver = versions.getJSONObject(i);
            URL versionUrl = new URL(ver.getString("url"));
            // String id = ver.getString("id");
            // System.out.println(id);
            JSONObject version = fetchVersion(versionUrl);
            if(version == null) {
                continue;
            }
            JSONObject assets = version.getJSONObject("assetIndex");
            String assetsId = assets.getString("id");
            if(!uniqueIds.contains(assetsId)) {
                uniqueIds.add(assetsId);
                System.out.println(assets.getString("url"));
            }
        }
    }
}
