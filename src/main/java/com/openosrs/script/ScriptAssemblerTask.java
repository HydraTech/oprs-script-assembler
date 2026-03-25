package com.openosrs.script;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import net.runelite.cache.definitions.ScriptDefinition;
import net.runelite.cache.definitions.savers.ScriptSaver;
import net.runelite.cache.script.RuneLiteInstructions;
import net.runelite.cache.script.assembler.Assembler;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public abstract class ScriptAssemblerTask extends DefaultTask {
	private String scriptDirectory_;
	private String outputDirectory_;
	private String packagedName_;

	private final Logger log = getLogger();

	@InputDirectory
	public String getScriptDirectory() {
		return scriptDirectory_;
	}

	public void setScriptDirectory(String scriptDirectory_) {
		this.scriptDirectory_ = scriptDirectory_;
	}

	@OutputDirectory
	public String getOutputDirectory() {
		return outputDirectory_;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory_ = outputDirectory;
	}

	@Input
	public String getPackagedName() {
		return packagedName_;
	}

	public void setPackagedName(String packagedName) {
		this.packagedName_ = packagedName;
	}

	@Input
	public abstract Property<Boolean> getLongSupport();

	@TaskAction
	public void assemble() {
		File scriptDirectory = new File(scriptDirectory_);
		File outputDirectory = new File(outputDirectory_);

		RuneLiteInstructions instructions = new RuneLiteInstructions();
		instructions.init();

		Assembler assembler = new Assembler(instructions);
		ScriptSaver saver = new ScriptSaver(getLongSupport().getOrElse(true));

		int count = 0;

		// Clear the target directory to remove stale entries
		try {
			MoreFiles.deleteDirectoryContents(outputDirectory.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
		} catch (IOException e) {
			throw new RuntimeException("Could not clear outputDirectory: " + outputDirectory, e);
		}

		File scriptOut = new File(outputDirectory, packagedName_);
		scriptOut.mkdirs();

		for (File scriptFile : scriptDirectory.listFiles((dir, name) -> name.endsWith(".rs2asm"))) {
			log.lifecycle("[debug] Assembling " + scriptFile);

			try (FileInputStream fin = new FileInputStream(scriptFile)) {
				ScriptDefinition script = assembler.assemble(fin);
				byte[] packedScript = saver.save(script);

				File targetFile = new File(scriptOut, Integer.toString(script.getId()));
				Files.write(packedScript, targetFile);

				File hashFile = new File(scriptFile.getParentFile().getPath(), Files.getNameWithoutExtension(scriptFile.getName()) + ".hash");
				if (hashFile.exists()) {
					Files.copy(hashFile, new File(scriptOut, script.getId() + ".hash"));
				}

				count++;
			} catch (IOException ex) {
				throw new RuntimeException("unable to open file", ex);
			}
		}

		log.lifecycle("[info] Assembled " + count + " scripts");
	}
}
