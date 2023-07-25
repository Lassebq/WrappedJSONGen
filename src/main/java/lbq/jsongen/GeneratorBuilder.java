package lbq.jsongen;

import java.nio.file.Path;

public class GeneratorBuilder {

	private boolean skipManifest = false;
	private boolean update = false;
	private boolean packToFolders = false;
	private boolean generateManifest = false;
	private Path basePath;
	
	public GeneratorBuilder skipManifest() {
		skipManifest = true;
		return this;
	}
	
	public GeneratorBuilder updateFolder() {
		update = true;
		return this;
	}
	
	public GeneratorBuilder basePath(Path path) {
		basePath = path;
		return this;
	}

	public GeneratorBuilder packToFolders() {
		packToFolders = true;
		return this;
	}
	
	public GeneratorBuilder generateManifest() {
		generateManifest = true;
		return this;
	}
	
	public Generator build() {
		return new Generator(basePath, update, skipManifest, packToFolders, generateManifest);
	}
	
}
