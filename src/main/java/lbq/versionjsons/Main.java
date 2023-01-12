package lbq.versionjsons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Available parameters:");
			System.out.println("--path [path] - Specifiy jsons output directory");
			System.out.println("--wrapperJar [path] - Update json libraries with the new wrapper artifact");
			System.out.println("--packToFolders - Copy each json to a folder with the name of the version");
		}
		Path dir = null;
		Path wrapperJar = null;
		boolean packToFolders = false;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			boolean hasNext = i + 1 < args.length;
			if(hasNext && arg.equals("--path")) {
				dir = Paths.get(args[i + 1]);
			}
			if(hasNext && arg.equals("--wrapperJar")) {
				wrapperJar = Paths.get(args[i + 1]);
			}
			if(arg.equals("--packToFolders")) {
				packToFolders = true;
			}
			i++;
		}
		if(dir != null) {
			try {
				VersionsWriter verWriter = new VersionsWriter(dir, wrapperJar);
				verWriter.generateJSONs();
				if(packToFolders) {
					verWriter.packToFolders();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Use --path to specify jsons output directory");
		}
	}
}
