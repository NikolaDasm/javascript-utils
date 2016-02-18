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

package nikoladasm.javascript.utils.dependencies;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class BaseJSDependenciesResolver {
	
	protected static class DependenciesNode {
		
		public DependenciesNode root;
		public Path path;
		public List<DependenciesNode> childNodes = new LinkedList<>();
		public Map<String,Path> dependenciesMap = new HashMap<>();
		
		public DependenciesNode getNode(Path path) {
			if (this.path.equals(path)) return this;
			for (DependenciesNode node : childNodes) {
				if (node.path.equals(path)) return node;
				DependenciesNode childNode = node.getNode(path);
				if (childNode != null) return childNode;
			}
			return null;
		}
		
		public void populateDependenciesMap(Map<Path,Map<String,Path>> parent) {
			if (parent.containsKey(path)) return;
			parent.put(path, dependenciesMap);
			childNodes.forEach(node -> node.populateDependenciesMap(parent));
		}
	}
	
	public static final String JS_FILE_EXTENSION = ".js";
	public static final String JSX_FILE_EXTENSION = ".jsx";
	public static final String[] DEFAULT_JAVASCRIPT_FILE_EXTENSIONS =
		new String[]{JS_FILE_EXTENSION, JSX_FILE_EXTENSION};
	
	protected SourceFileReader fileReader;
	private String[] javaScriptFileExtensions = DEFAULT_JAVASCRIPT_FILE_EXTENSIONS;
	
	public BaseJSDependenciesResolver(SourceFileReader fileReader) {
		this.fileReader = fileReader;
	}
	
	public BaseJSDependenciesResolver javaScriptFileExtensions(String[] javaScriptFileExtensions) {
		this.javaScriptFileExtensions = javaScriptFileExtensions;
		return this;
	}
	
	public String[] javaScriptFileExtensions() {
		return javaScriptFileExtensions;
	}
	
	protected Path getAbsoluteExistingPath(Path originalPath) {
		Path path = (originalPath.isAbsolute()) ? originalPath : originalPath.toAbsolutePath();
		if (Files.exists(path) && !Files.isDirectory(path)) return path;
		for (String extension : javaScriptFileExtensions) {
			Path pathWithExtension = path.resolveSibling(path.getFileName() + extension);
			if (Files.exists(pathWithExtension) && !Files.isDirectory(pathWithExtension)) return pathWithExtension;
		}
		throw new RuntimeException("Invalid dependencies"+originalPath);
	}
	
	protected abstract void buildIncludesNodeTree(DependenciesNode parent) throws Exception;
	
	public Map<Path,Map<String,Path>> resolve(Path root) throws Exception {
		Path path = getAbsoluteExistingPath(root.normalize());
		DependenciesNode node = new DependenciesNode();
		node.root = node;
		node.path = path;
		buildIncludesNodeTree(node);
		Map<Path,Map<String,Path>> dependensiesMap = new HashMap<>();
		node.populateDependenciesMap(dependensiesMap);
		return dependensiesMap;
	}
}
