package wyjc.compiler;

import wyil.lang.Module;
import wyil.stages.ModuleTransform;
import wyil.util.Logger;

public class WyilTransform implements Compiler.Stage {
	private String name;
	private ModuleTransform transform;
	
	public WyilTransform(String name, ModuleTransform transform) {
		this.name = name;
		this.transform = transform;
	}
	
	public String name() {
		return name;
	}
	
	public Module process(Module module, Logger logout) {
		long start = System.currentTimeMillis();
			
		try {
			module = transform.apply(module);
			logout.logTimedMessage("[" + module.filename() + "] applied " + name,
					System.currentTimeMillis() - start);
			return module;
		} catch(RuntimeException ex) {
			logout.logTimedMessage("[" + module.filename()
					+ "] failed on " + name + " (" + ex.getMessage() + ")",
					System.currentTimeMillis() - start);			
			throw ex;			
		}
	}
}
