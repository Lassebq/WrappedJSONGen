package lbq.jsongen;

import java.nio.file.Path;

public class GeneratorBuilder {

	private boolean update = false;
	private boolean packToFolders = false;
	private boolean generateManifest = false;
	private boolean lwjglCompat = false;
	private boolean multimc = false;
	private boolean micromixin = false;
	private Path basePath;
	private String postfix;
	private String onlyver;

	public GeneratorBuilder lwjglCompat() {
		lwjglCompat = true;
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

    public GeneratorBuilder setPostfix(String string) {
		postfix = string;
		return this;
    }

    public GeneratorBuilder setVersion(String string) {
		onlyver = string;
		return this;
    }

    public GeneratorBuilder setMultiMC() {
		multimc = true;
		return this;
    }

    public GeneratorBuilder micromixin() {
		micromixin = true;
		return this;
    }
	
	public Generator build() {
		return new Generator(basePath, update, packToFolders, generateManifest, postfix, onlyver, lwjglCompat, multimc, micromixin);
	}
	
}
