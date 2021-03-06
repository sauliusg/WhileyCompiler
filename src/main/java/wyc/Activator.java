// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package wyc;

import wycc.cfg.Configuration;
import wycc.lang.Command;
import wycc.lang.Module;
import wycc.util.Logger;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.ID;
import wyfs.util.Trie;
import wyil.interpreter.ConcreteSemantics.RValue;
import wyil.interpreter.Interpreter;

import java.io.IOException;

import wyal.lang.WyalFile;
import wybs.lang.Build;
import wybs.lang.Build.Project;
import wybs.lang.Build.Task;
import wybs.lang.NameID;
import wybs.util.AbstractCompilationUnit.Tuple;
import wybs.util.AbstractCompilationUnit.Value;
import wyc.lang.WhileyFile;
import wyc.lang.WhileyFile.Type;
import wyc.task.CompileTask;

public class Activator implements Module.Activator {

	public static Trie SOURCE_CONFIG_OPTION = Trie.fromString("build/whiley/source");
	public static Trie TARGET_CONFIG_OPTION = Trie.fromString("build/whiley/target");
	private static Value.UTF8 SOURCE_DEFAULT = new Value.UTF8("src".getBytes());
	private static Value.UTF8 TARGET_DEFAULT = new Value.UTF8("bin".getBytes());

	public static Build.Platform WHILEY_PLATFORM = new Build.Platform() {
		private Trie source;
		// Specify directory where generated WyIL files are dumped.
		private Trie target;
		//
		@Override
		public String getName() {
			return "whiley";
		}

		@Override
		public Configuration.Schema getConfigurationSchema() {
			return Configuration.fromArray(
					Configuration.UNBOUND_STRING(SOURCE_CONFIG_OPTION, "Specify location for whiley source files", SOURCE_DEFAULT),
					Configuration.UNBOUND_STRING(TARGET_CONFIG_OPTION, "Specify location for generated wyil files", TARGET_DEFAULT));
		}

		@Override
		public void apply(Configuration configuration) {
			// Extract source path
			this.source = Trie.fromString(configuration.get(Value.UTF8.class, SOURCE_CONFIG_OPTION).unwrap());
			this.target = Trie.fromString(configuration.get(Value.UTF8.class, TARGET_CONFIG_OPTION).unwrap());
		}

		@Override
		public Task initialise(Build.Project project) {
			return new CompileTask(project);
		}

		@Override
		public Content.Type<?> getSourceType() {
			return WhileyFile.ContentType;
		}

		@Override
		public Content.Type<?> getTargetType() {
			return WhileyFile.BinaryContentType;
		}

		@Override
		public Content.Filter<?> getSourceFilter() {
			return Content.filter("**", WhileyFile.ContentType);
		}

		@Override
		public Content.Filter<?> getTargetFilter() {
			return Content.filter("**", WhileyFile.BinaryContentType);
		}

		@Override
		public Path.Root getSourceRoot(Path.Root root) throws IOException {
			return root.createRelativeRoot(source);
		}

		@Override
		public Path.Root getTargetRoot(Path.Root root) throws IOException {
			return root.createRelativeRoot(target);
		}

		@Override
		public void execute(Build.Project project, Path.ID id, String method, Value... args) {
			Type.Method sig = new Type.Method(new Tuple<>(new Type[0]), new Tuple<>(), new Tuple<>(), new Tuple<>());
			NameID name = new NameID(id, method);
			// Try to run the given function or method
			Interpreter interpreter = new Interpreter(project, System.out);
			RValue[] returns = interpreter.execute(name, sig, interpreter.new CallStack());
			// Print out any return values produced
			if (returns != null) {
				for (int i = 0; i != returns.length; ++i) {
					if (i != 0) {
						System.out.println(", ");
					}
					System.out.println(returns[i]);
				}
			}
		}
	};

	/**
	 * Default implementation of a content registry. This associates whiley and
	 * wyil files with their respective content types.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class Registry implements Content.Registry {
		@Override
		public void associate(Path.Entry e) {
			String suffix = e.suffix();

			if (suffix.equals("whiley")) {
				e.associate(WhileyFile.ContentType, null);
			} else if (suffix.equals("wyil")) {
				e.associate(WhileyFile.BinaryContentType, null);
			} else if (suffix.equals("wyal")) {
				e.associate(WyalFile.ContentType, null);
			}
		}

		@Override
		public String suffix(Content.Type<?> t) {
			return t.getSuffix();
		}
	}

	/**
	 * The master project content type registry. This is needed for the build
	 * system to determine the content type of files it finds on the file
	 * system.
	 */
	public final Content.Registry registry = new Registry();


	// =======================================================================
	// Start
	// =======================================================================

	@Override
	public Module start(Module.Context context) {
		// FIXME: logger is a hack!
		final Logger logger = new Logger.Default(System.err);
		// List of commands to use
		context.register(Build.Platform.class, WHILEY_PLATFORM);
		// List of content types
		context.register(Content.Type.class, WhileyFile.ContentType);
		context.register(Content.Type.class, WhileyFile.BinaryContentType);
		// Done
		return new Module() {
			// what goes here?
		};
	}

	// =======================================================================
	// Stop
	// =======================================================================

	@Override
	public void stop(Module module, Module.Context context) {
		// could do more here?
	}
}
