/*
 *  JavaScript Utils
 *  Copyright (C) 2016  Nikolay Platov
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nikoladasm.javascript.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.google.javascript.jscomp.CommandLineRunner;

import nikoladasm.javascript.utils.dependencies.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public class JSUtils {

	public static final String JS_FILE_EXTENSION =
		BaseJSDependenciesResolver.JS_FILE_EXTENSION;
	public static final String JSX_FILE_EXTENSION =
		BaseJSDependenciesResolver.JSX_FILE_EXTENSION;

	private static final String INTEFNAL_BABEL_SCRIPT_PATH = "resources/jslib/babel/babel.min.js";
	private static final String INPUT_SCRIPT_VAR = "input";
	private static final String JSX_TRANSFORM_COMMAND = "Babel.transform(input, { presets: ['react'] }).code";
	private static final String ES2015_TRANSFORM_COMMAND = "Babel.transform(input, { presets: ['es2015'] }).code";
	private static final String JSX_AND_ES2015_TRANSFORM_COMMAND = "Babel.transform(input, { presets: ['react','es2015'] }).code";
	private static final String[] UGLIFYJS2_SCRIPT_PATHS = new String[]{
		"resources/jslib/uglifyjs2/utils.js",
		"resources/jslib/uglifyjs2/ast.js",
		"resources/jslib/uglifyjs2/parse.js",
		"resources/jslib/uglifyjs2/transform.js",
		"resources/jslib/uglifyjs2/scope.js",
		"resources/jslib/uglifyjs2/output.js",
		"resources/jslib/uglifyjs2/compress.js",
		"resources/jslib/uglifyjs2/sourcemap.js",
		"resources/jslib/uglifyjs2/mozilla-ast.js",
		"resources/jslib/uglifyjs2/propmangle.js",
		"resources/jslib/uglifyjs2/exports.js",
		"resources/jslib/uglifyjs2/init.js"
	};
	private Reader babelScript;
	private String externalBabelScriptPath;
	private ScriptEngine babelScriptEngine;
	private StringWriter babelScriptEngineStringWriter;
	private SimpleBindings babelBindings = new SimpleBindings();
	private ES2015ModuleImportResolver es2015DependenciesResolver;
	private CJSDependenciesResolver cJSDependenciesResolver;
	private ScriptEngine uglifyJS2ScriptEngine;
	private StringWriter uglifyJS2ScriptEngineStringWriter;
	private SimpleBindings uglifyJS2Bindings = new SimpleBindings();
	
	public static String readFile(Path path, Charset encoding) throws IOException {
		StringBuffer sb = new StringBuffer();
		String ls = System.getProperty("line.separator");
		List<String> lines = Files.readAllLines(path, encoding);
		for (String line : lines)
			sb.append(line).append(ls);
		return sb.toString();
	}
	
	public static void writeFile(String content, Path path, Charset encoding) throws IOException {
		try (BufferedWriter bwr = new BufferedWriter(
			new OutputStreamWriter(
				Files.newOutputStream(path, CREATE), encoding))) {
			bwr.write(content);
		}
	}
	
	public JSUtils() {
		SourceFileReader sfr = path -> readFile(path, UTF_8);
		es2015DependenciesResolver = new ES2015ModuleImportResolver(sfr);
		cJSDependenciesResolver = new CJSDependenciesResolver(sfr);
	}
	
	public JSUtils externalBabelScriptPath(String externalBabelScriptPath) {
		this.externalBabelScriptPath = externalBabelScriptPath;
		return this;
	}
	
	public String externalBabelScriptPath() {
		return externalBabelScriptPath;
	}
	
	public JSUtils setES2015ResolverFileReader(SourceFileReader sfr) {
		es2015DependenciesResolver = new ES2015ModuleImportResolver(sfr);
		return this;
	}
	
	public JSUtils setCJSResolverFileReader(SourceFileReader sfr) {
		cJSDependenciesResolver = new CJSDependenciesResolver(sfr);
		return this;
	}
	
	public JSUtils babelScriptEngineStringWriter(StringWriter babelScriptEngineStringWriter) {
		this.babelScriptEngineStringWriter = babelScriptEngineStringWriter;
		return this;
	}
	
	public StringWriter babelScriptEngineStringWriter() {
		return babelScriptEngineStringWriter;
	}
	
	public JSUtils uglifyJS2ScriptEngineStringWriter(StringWriter stringWriter) {
		this.uglifyJS2ScriptEngineStringWriter = stringWriter;
		return this;
	}
	
	public StringWriter uglifyJS2ScriptEngineStringWriter() {
		return uglifyJS2ScriptEngineStringWriter;
	}
	
	private BufferedReader resourceReader(String resource) throws IOException {
		InputStream cis = this.getClass().getResourceAsStream(resource);
		InputStream clis = this.getClass().getClassLoader().getResourceAsStream(resource);
		InputStream is = null;
		if (cis != null) is = cis;
		else if (clis != null) is = clis;
		return new BufferedReader(new InputStreamReader(is, UTF_8));
	}
	
	private BufferedReader fileReader(Path filePath) throws IOException {
		return new BufferedReader(
				new InputStreamReader(
					Files.newInputStream(
						filePath, READ), UTF_8));
	}
	
	private Reader babelScript() {
		if (babelScript != null) return babelScript;
		if (externalBabelScriptPath == null) {
			try {
				babelScript = resourceReader(INTEFNAL_BABEL_SCRIPT_PATH);
			} catch (IOException | NullPointerException e) {
				throw new JSUtilsException("Can't load internal babel script", e);
			}
		} else {
			try {
				babelScript = fileReader(Paths.get(externalBabelScriptPath));
			} catch (IOException | NullPointerException e) {
				throw new JSUtilsException("Can't load external babel script", e);
			}
		}
		return babelScript;
	}
	
	private ScriptEngine babelScriptEngine() throws ScriptException {
		if (babelScriptEngine != null) return babelScriptEngine;
		babelScriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
		babelScriptEngine.eval(babelScript(), babelBindings);
		return babelScriptEngine;
	}
	
	private ScriptEngine uglifyJS2ScriptEngine() {
		if (uglifyJS2ScriptEngine != null) return uglifyJS2ScriptEngine;
		uglifyJS2ScriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			uglifyJS2ScriptEngine.eval("var exports = {};\n", uglifyJS2Bindings);
			for (String path : UGLIFYJS2_SCRIPT_PATHS)
				uglifyJS2ScriptEngine.eval(resourceReader(path), uglifyJS2Bindings);
			return uglifyJS2ScriptEngine;
		} catch (ScriptException | IOException e) {
			throw new JSUtilsException("Can't initialize uglifyJS2 script", e);
		}
	}
	
	public String transformJSXtoJS(String jsxSource) {
		babelBindings.put(INPUT_SCRIPT_VAR, jsxSource);
		try {
			babelScriptEngine().getContext().setWriter(babelScriptEngineStringWriter);
			return babelScriptEngine().eval(JSX_TRANSFORM_COMMAND, babelBindings).toString();
		} catch (ScriptException e) {
			throw new JSUtilsException("Can't transform JSX", e);
		}
	}
	
	public String transformES2015toES5(String es2015Source) {
		babelBindings.put(INPUT_SCRIPT_VAR, es2015Source);
		try {
			babelScriptEngine().getContext().setWriter(babelScriptEngineStringWriter);
			return babelScriptEngine().eval(ES2015_TRANSFORM_COMMAND, babelBindings).toString();
		} catch (ScriptException e) {
			throw new JSUtilsException("Can't transform ES2015", e);
		}
	}

	public String transformJSXAndES2015toES5(String jsxAndES2015Source) {
		babelBindings.put(INPUT_SCRIPT_VAR, jsxAndES2015Source);
		try {
			babelScriptEngine().getContext().setWriter(babelScriptEngineStringWriter);
			return babelScriptEngine().eval(JSX_AND_ES2015_TRANSFORM_COMMAND, babelBindings).toString();
		} catch (ScriptException e) {
			throw new JSUtilsException("Can't transform JSX or ES2015", e);
		}
	}
	
	public Map<Path,Map<String,Path>> getES2015DependenciesMap(Path topModule) {
		try {
			return es2015DependenciesResolver.resolve(topModule);
		} catch (Exception e) {
			throw new JSUtilsException("Can't resolve es2015 dependencies", e);
		}
	}

	public Map<Path,Map<String,Path>> getCJSDependenciesMap(Path topModule) {
		try {
			return cJSDependenciesResolver.resolve(topModule);
		} catch (Exception e) {
			throw new JSUtilsException("Can't resolve CJS dependencies", e);
		}
	}
	
	public void runClousureCompilerOptimizer(String[] args) {
		CommandLineRunner.main(args);
	}
	
	public String optimizeByUglifyJS2Script(String source, String jsOptionsObject) {
		uglifyJS2Bindings.put(INPUT_SCRIPT_VAR, source);
		if (jsOptionsObject == null) jsOptionsObject = "{}"; 
		try {
			uglifyJS2ScriptEngine().getContext().setWriter(uglifyJS2ScriptEngineStringWriter);
			return uglifyJS2ScriptEngine().eval("UglifyJS.minify(input, "+jsOptionsObject+");", uglifyJS2Bindings).toString();
		} catch (ScriptException e) {
			e.printStackTrace();
			throw new JSUtilsException("Can't optimize by uglifyJS", e);
		}
	}
}
