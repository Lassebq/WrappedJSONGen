package lbq.jsongen;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Available parameters:");
			System.out.println("--path [path] - Specifiy jsons output directory");
			System.out.println("--packToFolders - Copy each json to a folder with the name of the version");
			System.out.println("--update - Updates existing jsons");
			System.out.println("--generateManifest - Generates version manifest");
			System.out.println("--postfix - Append a string to the name of each json");
			System.out.println("--lwjgl3 - Force lwjgl3 in lwjgl2 versions using compat layer");
			System.out.println("--micromixin - Include launchwrapper-micromixin mod loader");
			return;
		}
		Path basePath = null;
		boolean packToFolders = false;
		boolean lwjglCompat = false;
		boolean micromixin = false;
		boolean manifest = false;
		boolean update = false;
		boolean clean = false;
		String postfix = null;
		String version = null;
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
			if (argName.equalsIgnoreCase("packToFolders")) {
				packToFolders = true;
			}
			if (argName.equalsIgnoreCase("lwjgl3")) {
				lwjglCompat = true;
			}
			if (argName.equalsIgnoreCase("micromixin")) {
				micromixin = true;
			}
			if (argName.equalsIgnoreCase("generateManifest")) {
				manifest = true;
			}
			if (argName.equalsIgnoreCase("update")) {
				update = true;
			}
			if (argName.equalsIgnoreCase("clean")) {
				clean = true;
			}
			if (hasNext && argName.equalsIgnoreCase("postfix")) {
				i++;
				postfix = args[i];
			}
			if (hasNext && argName.equalsIgnoreCase("version")) {
				i++;
				version = args[i];
			}
		}
		new Generator(basePath, update, packToFolders, manifest, postfix, version, lwjglCompat, micromixin, clean)
				.generate();
	}
}
