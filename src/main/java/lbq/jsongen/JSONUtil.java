package lbq.jsongen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
	
	public static JSONObject getLibraryArtifact(String url) throws IOException {
		JSONObject artifact = new JSONObject();
		byte[] jar = Util.readAllBytes(new URL(url).openStream());
		String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
		artifact.put("url", url);
		artifact.put("size", jar.length);
		artifact.put("sha1", sha1);
		return artifact;
	}
	
	public static void replaceLibrary(JSONArray libraries, JSONObject library) {
		String[] libraryName = library.getString("name").split(":");
		boolean replaced = false;
		for(int i = libraries.length() - 1; i >= 0 ; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			if(libraryName[0].equals(libraryName2[0]) && libraryName[1].equals(libraryName2[1])) {
				if(!replaced) {
					libraries.put(i, library);
					replaced = true;
				} else {
					libraries.remove(i);
				}
			}
		}
		if(!replaced) {
			libraries.put(library);
		}
	}
	
	public static void removeLibrary(JSONArray libraries, String org, String name) {
		for(int i = libraries.length() - 1; i >= 0 ; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			if(org.equals(libraryName2[0]) && name.equals(libraryName2[1])) {
				libraries.remove(i);
			}
		}
	}
	
	public static Instant getTime(String time) {
		return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(time));
	}
	
	public static String getTimeString(Instant time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(time.truncatedTo(ChronoUnit.SECONDS)).replace("Z", "+00:00");
	}
}
