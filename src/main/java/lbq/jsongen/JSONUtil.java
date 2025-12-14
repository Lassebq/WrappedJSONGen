package lbq.jsongen;

import static lbq.jsongen.Util.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtil {

	public static boolean mergeLibraries(JSONArray source, JSONArray target) throws JSONException {
		boolean modified = false;
		for (int i = 0; i < source.length(); i++) {
			modified |= replaceLibrary(target, source.getJSONObject(i));
		}
		return modified;
	}

	public static boolean mergePreset(JSONObject source, JSONObject target) throws JSONException {
		boolean modified = false;
		for (String key : source.keySet()) {
			Object value = source.get(key);
			if (key.equals("libraries") && value instanceof JSONArray) {
				JSONArray libraries = (JSONArray) value;
				JSONArray librariesTarget = target.getJSONArray(key);
				modified |= mergeLibraries(libraries, librariesTarget);
				continue;
			}
			// existing value for "key":
			if (!target.has(key)) {
				modified = true;
			} else {
				if(target.get(key) instanceof JSONObject) {
					modified |= !((JSONObject)(target.get(key))).similar(value);
				} else {
					modified |= !target.get(key).equals(value);
				}
			}
			target.put(key, value);
		}
		return modified;
	}

	public static void writeJSON(JSONObject j, Path p) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(p)) {
			j.write(writer);
		}
	}

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

	public static JSONObject fetchVersion(String url) throws IOException {
		HttpURLConnection connection = openConnection(url);
		if (connection.getResponseCode() == 404) {
			System.err.println(url + " not found!");
			return null;
		}
		while (connection.getResponseCode() == 429) {
			System.err.println("Too many requests. Waiting for 429 to pass...");
			connection = openConnection(url);
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return parseJSON(connection.getInputStream());
	}

	public static JSONObject getLibraryArtifact(InputStream is, String url, String path) throws IOException {
		JSONObject artifact = getLibraryArtifact(is, url);
		artifact.put("path", path);
		return artifact;
	}

	public static JSONObject getLibraryArtifact(InputStream is, String url) throws IOException {
		JSONObject artifact = new JSONObject();
		byte[] jar = Util.readAllBytes(is);
		String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
		artifact.put("url", url);
		artifact.put("size", jar.length);
		artifact.put("sha1", sha1);
		return artifact;
	}

	public static boolean replaceLibrary(JSONArray libraries, JSONObject library) {
		String[] libraryName = library.getString("name").split(":");
		String s1 = libraryName.length == 4 ? libraryName[3] : "";
		boolean replaced = false;
		boolean changed = false;
		for (int i = libraries.length() - 1; i >= 0; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			String s2 = libraryName2.length == 4 ? libraryName2[3] : "";
			if (libraryName[0].equals(libraryName2[0]) && libraryName[1].equals(libraryName2[1]) && s1.equals(s2)) {
				changed = changed || !library.similar(libraries.getJSONObject(i));
				if (!replaced) {
					libraries.put(i, library);
					replaced = true;
				} else {
					libraries.remove(i);
				}
			}
		}
		if (!replaced) {
			changed = true;
			libraries.put(library);
		}
		return changed;
	}

	public static boolean containsLibrary(JSONArray libraries, String org, String name) {
		boolean found = false;
		for (int i = libraries.length() - 1; i >= 0; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			if (org.equals(libraryName2[0]) && name.equals(libraryName2[1])) {
				found = true;
			}
		}
		return found;
	}

	public static boolean removeLibrary(JSONArray libraries, String org, String name) {
		boolean removed = false;
		for (int i = libraries.length() - 1; i >= 0; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			if (org.equals(libraryName2[0]) && name.equals(libraryName2[1])) {
				libraries.remove(i);
				removed = true;
			}
		}
		return removed;
	}

	public static JSONArray getStringsAsArray(String... list) {
		JSONArray arr = new JSONArray();
		for(String s : list) {
			arr.put(s);
		}
		return arr;
	}

	public static Instant getTime(String time) {
		return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(time));
	}

	public static String getTimeString(Instant time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))
				.format(time.truncatedTo(ChronoUnit.SECONDS)).replace("Z", "+00:00");
	}

	// public static JSONObject generateLibraryEntry(String repo, String library) throws IOException {
	// 	return generateLibraryEntry(repo, library, true);
	// }

	public static String getArtifactURL(String urlBase, String library, String classifier) {
		return urlBase + getArtifactPath(library, classifier);
	}

	public static String getArtifactPath(String library, String classifier) {
		String[] lib = library.split(":");
		String basePath = lib[0].replace('.', '/') + "/" + lib[1] + "/" + lib[2] + "/" + lib[1] + "-" + lib[2];
		if(classifier == null && lib.length >= 4) {
			classifier = lib[3];
		}
		return basePath + (classifier != null ? ("-" + classifier) : "") + ".jar";
	}

	public static JSONObject getArtifact(String url, String path, boolean fast) throws IOException {
		JSONObject artifact = new JSONObject();
		artifact.put("url", url);
		artifact.put("path", path);
		HttpURLConnection urlConnection = (HttpURLConnection) (new URL(url).openConnection());
		if (urlConnection.getResponseCode() != 200) {
			return null;
		}
		if (fast) {
			HttpURLConnection sha1urlConnection = (HttpURLConnection) (new URL(url + ".sha1")
					.openConnection());
			if (sha1urlConnection.getResponseCode() != 200) {
				byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
				String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
				artifact.put("size", jar.length);
				artifact.put("sha1", sha1);
			} else {
				artifact.put("size", urlConnection.getContentLengthLong());
				artifact.put("sha1", Util.readString(sha1urlConnection.getInputStream()).trim());
			}
		} else {
			byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			artifact.put("size", jar.length);
			artifact.put("sha1", sha1);
		}
		return artifact;
	}

	// public static JSONObject generateLibraryEntry(String repo, String library, boolean fast) throws IOException {
	// 	// System.out.println(library);
	// 	JSONObject libraryObject = new JSONObject();

	// 	JSONObject artifact;
	// 	JSONObject downloads = new JSONObject();

	// 	String pathStr = getArtifactPath(library, null);
	// 	artifact = getArtifact(repo + pathStr, pathStr, fast);
	// 	if(artifact != null) {
	// 		downloads.put("artifact", artifact);
	// 	}
	// 	// if (path.length != 4) {
	// 		JSONObject classifiers = new JSONObject();
	// 		boolean hasClassifiers = false;
	// 		pathStr = getArtifactPath(library, "sources");
	// 		artifact = getArtifact(repo + pathStr, pathStr, fast);
	// 		if(artifact != null) {
	// 			hasClassifiers = true;
	// 			classifiers.put("sources", artifact);
	// 		}
	// 		boolean natives = false;
	// 		JSONObject nativesList = new JSONObject();
	// 		for (String s : new String[] { "linux", "osx", "windows" }) {
	// 			pathStr = getArtifactPath(library, "natives-" + s);
	// 			artifact = getArtifact(repo + pathStr, pathStr, fast);
	// 			if(artifact == null) {
	// 				continue;
	// 			}
	// 			classifiers.put("natives-" + s, artifact);
	// 			nativesList.put(s, "natives-" + s);
	// 			natives = true;
	// 		}
	// 		if (natives) {
	// 			JSONObject extract = new JSONObject();
	// 			JSONArray exclude = new JSONArray(Arrays.asList("META-INF/"));
	// 			extract.put("exclude", exclude);
	// 			libraryObject.put("extract", extract);
	// 			libraryObject.put("natives", nativesList);
	// 		}
	// 		if (hasClassifiers) {
	// 			downloads.put("classifiers", classifiers);
	// 		}
	// 	// }
	// 	libraryObject.put("downloads", downloads);
	// 	libraryObject.put("name", library);
	// 	return libraryObject;
	// }
}
