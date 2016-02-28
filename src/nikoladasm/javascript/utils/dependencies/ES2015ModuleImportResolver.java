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

import java.nio.file.Path;

import com.shapesecurity.shift.ast.*;
import com.shapesecurity.shift.parser.Parser;

public class ES2015ModuleImportResolver extends BaseJSDependenciesResolver {
	
	public ES2015ModuleImportResolver(SourceFileReader fileReader) {
		super(fileReader);
	}
	
	@Override
	protected void buildIncludesNodeTree(DependenciesNode parent) throws Exception {
		try {
			String source = fileReader.read(parent.path);
			Module module = Parser.parseModule(source);
			for (ImportDeclarationExportDeclarationStatement astNode : module.items) {
				if (astNode instanceof Import) {
					Import moduleImport = (Import) astNode;
					String moduleSpecifier = moduleImport.moduleSpecifier;
					String dependency;
					if (moduleSpecifier.startsWith("./"))
						dependency = moduleSpecifier.substring(2);
					else
						dependency = moduleSpecifier;
					Path include =
						getAbsoluteExistingPath(parent.path.getParent().resolve(dependency).normalize());
					parent.dependenciesMap.put(moduleSpecifier, include);
					DependenciesNode node = parent.root.getNode(include);
					if (node != null) {
						parent.childNodes.add(node);
					} else {
						node = new DependenciesNode();
						node.root = parent.root;
						node.path = include;
						parent.childNodes.add(node);
						buildIncludesNodeTree(node);
					}
				}
			}
		} catch (Exception e) {
			throw new Exception("Path"+parent.path, e);
		}
	}
}
