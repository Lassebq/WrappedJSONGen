package lbq.jsongen;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtil {

	public static JSONObject parseJSON(Path path) throws IOException {
		return parseJSON(Files.newInputStream(path));
	}

	public static JSONArray parseJSONArray(Path path) throws IOException {
		return parseJSONArray(Files.newInputStream(path));
	}

	public static JSONObject parseJSON(InputStream stream) throws IOException {
		byte[] bytes = Util.readAllBytes(stream);
		String content = new String(bytes);
		return new JSONObject(content);
	}

	public static JSONArray parseJSONArray(InputStream stream) throws IOException {
		byte[] bytes = Util.readAllBytes(stream);
		String content = new String(bytes);
		return new JSONArray(content);
	}

	public static JSONObject getPresetJSON() throws IOException {
		return parseJSON(ClassLoader.getSystemResourceAsStream("preset.json"));
	}
	
	public static JSONObject getManifest() throws IOException {
		return new JSONObject(new String(Util.readAllBytes(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream())));
	}
	
	public static JSONObject getLibraryArtifact(Path path, String url) throws IOException {
		JSONObject artifact = new JSONObject();
		if(url != null) {
			byte[] jar = Util.readAllBytes(Files.newInputStream(path));
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			artifact.put("url", url);
			artifact.put("size", jar.length);
			artifact.put("sha1", sha1);
		}
		return artifact;
	}
	
	public static void main(String[] args) throws IOException {
		JSONObject obj = getLibraryArtifact(Paths.get("D:\\Stuff\\git\\MCPHackers.github.io\\server\\c1.10.1.jar"), "https://mcphackers.github.io/server/c1.10.1.jar");
		try(BufferedWriter writer = Files.newBufferedWriter(Paths.get("C:/Users/Lassebq/Desktop/artifact.json"))) {
			obj.write(writer);
		}
	}
	
	public static Instant getTime(String time) {
		return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(time));
	}
	
	public static String getTimeString(Instant time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(time.truncatedTo(ChronoUnit.SECONDS)).replace("Z", "+00:00");
	}
}
