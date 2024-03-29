package lbq.jsongen;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		if(args.length == 0) {
			System.out.println("Available parameters:");
			System.out.println("--path [path] - Specifiy jsons output directory");
			System.out.println("--packToFolders - Copy each json to a folder with the name of the version");
			System.out.println("--skipManifest - Skips jsons from manifest");
			System.out.println("--update - Updates existing jsons");
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
				builder.basePath(Paths.get(args[i + 1]));
			}
			if(argName.equalsIgnoreCase("packToFolders")) {
				builder.packToFolders();
			}
			if(argName.equalsIgnoreCase("skipManifest")) {
				builder.skipManifest();
			}
			if(argName.equalsIgnoreCase("generateManifest")) {
				builder.generateManifest();
			}
			if(argName.equalsIgnoreCase("update")) {
				builder.updateFolder();
			}
		}
		builder.build().generate();
	}
}
