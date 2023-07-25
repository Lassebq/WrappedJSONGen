package lbq.jsongen;

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
import java.util.Arrays;

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

	public static JSONObject fetchVersion(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		if(connection.getResponseCode() == 404) {
			System.err.println(url.toExternalForm() + " not found!");
			return null;
		}
		while(connection.getResponseCode() == 429) {
			System.err.println("Too many requests. Waiting for 429 to pass...");
			connection = (HttpURLConnection)url.openConnection();
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
		boolean replaced = false;
		boolean changed = false;
		for(int i = libraries.length() - 1; i >= 0 ; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			if(libraryName[0].equals(libraryName2[0]) && libraryName[1].equals(libraryName2[1])) {
				changed = changed || !library.similar(libraries.getJSONObject(i));
				if(!replaced) {
					libraries.put(i, library);
					replaced = true;
				} else {
					libraries.remove(i);
				}
			}
		}
		if(!replaced) {
			changed = true;
			libraries.put(library);
		}
		return changed;
	}
	
	public static boolean removeLibrary(JSONArray libraries, String org, String name) {
		boolean removed = false;
		for(int i = libraries.length() - 1; i >= 0 ; i--) {
			String[] libraryName2 = libraries.getJSONObject(i).getString("name").split(":");
			if(org.equals(libraryName2[0]) && name.equals(libraryName2[1])) {
				libraries.remove(i);
				removed = true;
			}
		}
		return removed;
	}
	
	public static Instant getTime(String time) {
		return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(time));
	}
	
	public static String getTimeString(Instant time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(time.truncatedTo(ChronoUnit.SECONDS)).replace("Z", "+00:00");
	}

	public static JSONObject generateLibraryEntry(String repo, String library) throws IOException {
		return generateLibraryEntry(repo, library, true);
	}

	public static JSONObject generateLibraryEntry(String repo, String library, boolean fast) throws IOException {
		System.out.println(library);
		String[] path = library.split(":");
		String basePath = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2];
		String lib = basePath + ".jar";
		String sources = basePath + "-sources.jar";

		HttpURLConnection urlConnection = (HttpURLConnection)(new URL(repo + lib).openConnection());
		if(urlConnection.getResponseCode() == 404) {
			return null;
		}

		JSONObject libraryObject = new JSONObject();
		JSONObject downloads = new JSONObject();
		JSONObject artifact = new JSONObject();
		artifact.put("url", repo + lib);
		artifact.put("path", lib);
		if(fast) {
			artifact.put("size", urlConnection.getContentLengthLong());
			HttpURLConnection sha1urlConnection = (HttpURLConnection)(new URL(repo + lib + ".sha1").openConnection());
			if(sha1urlConnection.getResponseCode() == 404) {
				byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
				String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
				artifact.put("sha1", sha1);
			} else {
				artifact.put("sha1", Util.readString(sha1urlConnection.getInputStream()));
			}
		} else {
			byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
			String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
			artifact.put("size", jar.length);
			artifact.put("sha1", sha1);
		}
		downloads.put("artifact", artifact);
		JSONObject classifiers = new JSONObject();
		boolean hasClassifiers = false;
		urlConnection = (HttpURLConnection)(new URL(repo + sources).openConnection());
		if(urlConnection.getResponseCode() != 404) {
			hasClassifiers = true;
			JSONObject sourcesObj = new JSONObject();
			sourcesObj.put("url", repo + sources);
			sourcesObj.put("path", sources);
			if(fast) {
				sourcesObj.put("size", urlConnection.getContentLengthLong());
				HttpURLConnection sha1urlConnection = (HttpURLConnection)(new URL(repo + sources + ".sha1").openConnection());
				if(sha1urlConnection.getResponseCode() == 404) {
					byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
					String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
					sourcesObj.put("sha1", sha1);
				} else {	
					sourcesObj.put("sha1", Util.readString(sha1urlConnection.getInputStream()));
				}
			} else {
				byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
				String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
				sourcesObj.put("size", jar.length);
				sourcesObj.put("sha1", sha1);
			}
			classifiers.put("sources", sourcesObj);
		}
		boolean natives = false;
		JSONObject nativesList = new JSONObject();
		for(String s : new String[] {"linux", "osx", "windows"}) {
			String nativesPath = basePath + "natives-" + s + ".jar";
			urlConnection = (HttpURLConnection)(new URL(repo + nativesPath).openConnection());
			if(urlConnection.getResponseCode() == 404) {
				// FIXME Break? I doubt there are native libraries with only specific platform support
				continue;
			}
			hasClassifiers = true;
			nativesList.put(s, "natives-" + s);
			natives = true;
			JSONObject nativesObj = new JSONObject();
			nativesObj.put("url", repo + nativesPath);
			nativesObj.put("path", nativesPath);
			if(fast) {
				nativesObj.put("size", urlConnection.getContentLengthLong());
				HttpURLConnection sha1urlConnection = (HttpURLConnection)(new URL(repo + nativesPath + ".sha1").openConnection());
				if(sha1urlConnection.getResponseCode() == 404) {
					byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
					String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
					nativesObj.put("sha1", sha1);
				} else {
					nativesObj.put("sha1", Util.readString(sha1urlConnection.getInputStream()));
				}
			} else {
				byte[] jar = Util.readAllBytes(urlConnection.getInputStream());
				String sha1 = Util.getSHA1(new ByteArrayInputStream(jar));
				nativesObj.put("size", jar.length);
				nativesObj.put("sha1", sha1);
			}
			classifiers.put("natives-" + s, nativesObj);
		}
		if(natives) {
			JSONObject extract = new JSONObject();
			JSONArray exclude = new JSONArray(Arrays.asList("META-INF/"));
			extract.put("exclude", exclude);
			libraryObject.put("extract", extract);
			libraryObject.put("natives", nativesList);
		}
		if(hasClassifiers) {
			downloads.put("classifiers", classifiers);
		}
		libraryObject.put("downloads", downloads);
		libraryObject.put("name", library);
		return libraryObject;
	}
}
