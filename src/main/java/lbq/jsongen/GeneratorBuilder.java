package lbq.jsongen;

import java.nio.file.Path;

public class GeneratorBuilder {

	private boolean skipManifest = false;
	private boolean update = false;
	private boolean packToFolders = false;
	private Path basePath;
	private Path wrapperJar;
	private Path wrapperSource;
	
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
	
	public GeneratorBuilder wrapperSource(Path path) {
		wrapperSource = path;
		return this;
	}
	
	public GeneratorBuilder wrapperJar(Path path) {
		wrapperJar = path;
		return this;
	}

	public GeneratorBuilder packToFolders() {
		packToFolders = true;
		return this;
	}
	
	public Generator build() {
		return new Generator(basePath, wrapperJar, wrapperSource, update, skipManifest, packToFolders);
	}
	
}
