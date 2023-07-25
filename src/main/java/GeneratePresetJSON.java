import static lbq.jsongen.JSONUtil.generateLibraryEntry;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import lbq.jsongen.Util;

public class GeneratePresetJSON {
    public static void main(String[] args) throws IOException {
        String repo1 = "https://mcphackers.github.io/libraries/";
        String[] libraries1 = {
            "org.mcphackers:launchwrapper:1.0",
            "org.mcphackers.rdi:rdi:1.0",
            "org.lwjgl.lwjgl:lwjgl:2.9.4",
            "org.lwjgl.lwjgl:lwjgl_util:2.9.4",
            "org.json:json:20230311",
            "com.paulscode:codecjorbis:20230120"
        };

        String repo2 = "https://libraries.minecraft.net/";
        String[] libraries2 = {
            "org.lwjgl.lwjgl:lwjgl-platform:2.9.3",
            "com.paulscode:codecwav:20101023",
            "com.paulscode:libraryjavasound:20101123",
            "com.paulscode:librarylwjglopenal:20100824",
            "com.paulscode:soundsystem:20120107"
        };

        String repo3 = "https://repo.maven.apache.org/maven2/";
        String[] libraries3 = {
            "net.java.jutils:jutils:1.0.0",
            "net.java.jinput:jinput:2.0.7",
            "net.java.jinput:jinput-platform:2.0.7",
            "org.ow2.asm:asm:9.2",
            "org.ow2.asm:asm-tree:9.2"
        };

        String assetIndex = "https://mcphackers.github.io/assets/empty.json";
        String mainClass = "org.mcphackers.launchwrapper.Launch";

        JSONObject presetJSON = new JSONObject();
        JSONObject assetIndexObj = new JSONObject();
        JSONObject javaVersion = new JSONObject();
        JSONArray librariesArray = new JSONArray(libraries1.length + libraries2.length + libraries3.length);

        javaVersion.put("component", "jre-legacy");
        javaVersion.put("majorVersion", 8);

		byte[] jar = Util.readAllBytes(new URL(assetIndex).openStream());
		String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
        String assetsId = assetIndex.substring(assetIndex.lastIndexOf('/') + 1).replace(".json", "");
        assetIndexObj.put("id", assetsId);
        assetIndexObj.put("size", jar.length);
        assetIndexObj.put("sha1", sha1);
        assetIndexObj.put("url", assetIndex);

        presetJSON.put("assetIndex", assetIndexObj);
        presetJSON.put("mainClass", mainClass);
        presetJSON.put("complianceLevel", 0);
        presetJSON.put("assets", assetsId);
        presetJSON.put("javaVersion", javaVersion);

        int i = 0;
        for(String lib : libraries1) {
            librariesArray.put(i, generateLibraryEntry(repo1, lib));
            i++;
        }
        for(String lib : libraries2) {
            librariesArray.put(i, generateLibraryEntry(repo2, lib));
            i++;
        }
        for(String lib : libraries3) {
            librariesArray.put(i, generateLibraryEntry(repo3, lib));
            i++;
        }
        presetJSON.put("libraries", librariesArray);

        // If we are in a dev env, write newly generated config to resources
        Path out = Paths.get("src/main/resources/preset.json");
        if(Files.exists(out)) {
            try(BufferedWriter writer = Files.newBufferedWriter(out)) {
                presetJSON.write(writer);
            }
        }
    }
}
