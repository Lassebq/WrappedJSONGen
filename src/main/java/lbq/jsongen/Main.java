package lbq.jsongen;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		if(args.length == 0) {
			System.out.println("Available parameters:");
			System.out.println("--path [path] - Specifiy jsons output directory");
			System.out.println("--packToFolders - Copy each json to a folder with the name of the version");
			System.out.println("--update - Updates existing jsons");
			System.out.println("--postfix - Append a string to the name of each json");
			System.out.println("--lwjgl3 - Force lwjgl3 in lwjgl2 versions using compat layer");
			System.out.println("--version - Only generate json for a single specified version");
			System.out.println("--multimc - Generate MultiMC components (use with --version)");
			System.out.println("--micromixin - Include launchwrapper-micromixin mod loader");
			return;
		}
		GeneratorBuilder builder = new GeneratorBuilder();
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(!arg.startsWith("--")) {
				continue;
			}
			String argName = arg.substring(2);
			boolean hasNext = i + 1 < args.length;
			if(hasNext && argName.equalsIgnoreCase("path")) {
				i++;
				builder.basePath(Paths.get(args[i]));
			}
			if(argName.equalsIgnoreCase("packToFolders")) {
				builder.packToFolders();
			}
			if(argName.equalsIgnoreCase("lwjgl3")) {
				builder.lwjglCompat();
			}
			if(argName.equalsIgnoreCase("multimc")) {
				builder.setMultiMC();
			}
			if(argName.equalsIgnoreCase("micromixin")) {
				builder.micromixin();
			}
			if(argName.equalsIgnoreCase("generateManifest")) {
				builder.generateManifest();
			}
			if(argName.equalsIgnoreCase("update")) {
				builder.updateFolder();
			}
			if(hasNext && argName.equalsIgnoreCase("postfix")) {
				i++;
				builder.setPostfix(args[i]);
			}
			if(hasNext && argName.equalsIgnoreCase("version")) {
				i++;
				builder.setVersion(args[i]);
			}
		}
		builder.build().generate();
	}
}
