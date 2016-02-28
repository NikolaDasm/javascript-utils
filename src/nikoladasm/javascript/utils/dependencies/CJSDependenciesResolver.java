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
import java.util.LinkedList;
import java.util.List;

import com.shapesecurity.shift.ast.*;
import com.shapesecurity.shift.parser.Parser;

public class CJSDependenciesResolver extends BaseJSDependenciesResolver {
	
	public CJSDependenciesResolver(SourceFileReader fileReader) {
		super(fileReader);
	}
	
	private void parseStatement(Statement statement, List<String> dependencies) {
		if (statement instanceof BlockStatement) {
			BlockStatement blockStatement = (BlockStatement) statement;
			blockStatement.block.statements.forEach(stmt -> parseStatement(stmt, dependencies));
		} else if (statement instanceof DoWhileStatement) {
			DoWhileStatement doWhileStatement = (DoWhileStatement) statement;
			parseExpression(doWhileStatement.test, dependencies);
			parseStatement(doWhileStatement.body, dependencies);
		} else if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			parseExpression(expressionStatement.expression, dependencies);
		} else if (statement instanceof ForInStatement) {
			ForInStatement forInStatement = (ForInStatement) statement;
			parseExpression(forInStatement.right, dependencies);
			parseStatement(forInStatement.body, dependencies);
		} else if (statement instanceof ForOfStatement) {
			ForOfStatement forOfStatement = (ForOfStatement) statement;
			parseExpression(forOfStatement.right, dependencies);
			parseStatement(forOfStatement.body, dependencies);
		} else if (statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement) statement;
			if(forStatement.init.isJust()) {
				if (forStatement.init.just() instanceof Expression) {
					parseExpression((Expression)forStatement.init.just(), dependencies);
				} else if (forStatement.init.just() instanceof VariableDeclaration) {
					VariableDeclaration variableDeclaration = (VariableDeclaration) forStatement.init.just();
					variableDeclaration.declarators.forEach(vDecl -> {
						if (vDecl.init.isJust()) parseExpression(vDecl.init.just(), dependencies);
					});
				}
			}
			if(forStatement.test.isJust()) parseExpression(forStatement.test.just(), dependencies);
			if(forStatement.update.isJust()) parseExpression(forStatement.update.just(), dependencies);
			parseStatement(forStatement.body, dependencies);
		} else if (statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement) statement;
			parseExpression(ifStatement.test, dependencies);
			parseStatement(ifStatement.consequent, dependencies);
			if(ifStatement.alternate.isJust()) parseStatement(ifStatement.alternate.just(), dependencies);
		} else if (statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement) statement;
			parseStatement(labeledStatement.body, dependencies);
		} else if (statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement) statement;
			if(returnStatement.expression.isJust()) parseExpression(returnStatement.expression.just(), dependencies);
		} else if (statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement) statement;
			parseExpression(switchStatement.discriminant, dependencies);
			switchStatement.cases.forEach(switchCase -> {
				parseExpression(switchCase.test, dependencies);
				switchCase.consequent.forEach(stmt -> parseStatement(stmt, dependencies));
			});
		} else if (statement instanceof SwitchStatementWithDefault) {
			SwitchStatementWithDefault switchStatementWithDefault =
				(SwitchStatementWithDefault) statement;
			parseExpression(switchStatementWithDefault.discriminant, dependencies);
			switchStatementWithDefault.preDefaultCases.forEach(switchCase -> {
				parseExpression(switchCase.test, dependencies);
				switchCase.consequent.forEach(stmt -> parseStatement(stmt, dependencies));
			});
			switchStatementWithDefault.defaultCase.consequent.forEach(stmt -> parseStatement(stmt, dependencies));
			switchStatementWithDefault.postDefaultCases.forEach(switchCase -> {
				parseExpression(switchCase.test, dependencies);
				switchCase.consequent.forEach(stmt -> parseStatement(stmt, dependencies));
			});
		} else if (statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement) statement;
			parseExpression(throwStatement.expression, dependencies);
		} else if (statement instanceof TryCatchStatement) {
			TryCatchStatement tryCatchStatement = (TryCatchStatement) statement;
			tryCatchStatement.body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
			tryCatchStatement.catchClause.body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
		} else if (statement instanceof TryFinallyStatement) {
			TryFinallyStatement tryFinallyStatement = (TryFinallyStatement) statement;
			tryFinallyStatement.body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
			if (tryFinallyStatement.catchClause.isJust())
				tryFinallyStatement.catchClause.just().body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
			tryFinallyStatement.finalizer.statements.forEach(stmt -> parseStatement(stmt, dependencies));
		} else if (statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement =
				(VariableDeclarationStatement) statement;
			variableDeclarationStatement.declaration.declarators.forEach(vDecl -> {
				if (vDecl.init.isJust()) parseExpression(vDecl.init.just(), dependencies);
			});
		} else if (statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement) statement;
			parseExpression(whileStatement.test, dependencies);
			parseStatement(whileStatement.body, dependencies);
		} else if (statement instanceof WithStatement) {
			WithStatement withStatement = (WithStatement) statement;
			parseExpression(withStatement._object, dependencies);
			parseStatement(withStatement.body, dependencies);
		}
	}
	
	private void parseExpression(Expression expression, List<String> dependencies) {
		if (expression instanceof ArrayExpression) {
			ArrayExpression arrayExpression = (ArrayExpression) expression;
			arrayExpression.elements.forEach(seExpr -> {
				if (seExpr.isJust()) {
					if (seExpr.just() instanceof Expression)
						parseExpression((Expression) seExpr.just(), dependencies);
					else if (seExpr.just() instanceof SpreadElement)
						parseExpression(((SpreadElement) seExpr.just()).expression, dependencies);
				}
			});
		} else if (expression instanceof ArrowExpression) {
			ArrowExpression arrowExpression = (ArrowExpression) expression;
			if (arrowExpression.body instanceof FunctionBody) {
				FunctionBody functionBody = (FunctionBody) arrowExpression.body;
				functionBody.statements.forEach(stmt -> parseStatement(stmt, dependencies));
			}
		} else if (expression instanceof AssignmentExpression) {
			AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
			parseExpression(assignmentExpression.expression, dependencies);
		} else if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			parseExpression(binaryExpression.left, dependencies);
			parseExpression(binaryExpression.right, dependencies);
		} else if (expression instanceof CallExpression) {
			CallExpression callExpression = (CallExpression) expression;
			List<Expression> expressions = new LinkedList<>();
			callExpression.arguments.forEach(seExpr -> {
				if (seExpr instanceof Expression)
					expressions.add((Expression) seExpr);
				else if (seExpr instanceof SpreadElement)
					expressions.add(((SpreadElement) seExpr).expression);
			});
			boolean isRequire = false;
			if (callExpression.callee instanceof IdentifierExpression) {
				IdentifierExpression identifierExpression = (IdentifierExpression) callExpression.callee;
				if ("require".equals(identifierExpression.name)) {
					Expression exp;
					if (expressions.size() == 1 &&
						((exp = expressions.get(0)) instanceof LiteralStringExpression)) {
						dependencies.add(((LiteralStringExpression) exp).value);
						isRequire = true;
					}
				}
			}
			if (!isRequire) {
				if (callExpression.callee instanceof Expression)
					parseExpression((Expression) callExpression.callee, dependencies);
				expressions.forEach(exp -> parseExpression(exp, dependencies));
			}
		} else if (expression instanceof ClassExpression) {
			ClassExpression classExpression = (ClassExpression) expression;
			if (classExpression._super.isJust())
				parseExpression(classExpression._super.just(), dependencies);
			classExpression.elements.forEach(classElement -> {
				classElement.method.body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
			});
		} else if (expression instanceof ComputedMemberExpression) {
			ComputedMemberExpression computedMemberExpression =
				(ComputedMemberExpression) expression;
			if (computedMemberExpression._object instanceof Expression)
				parseExpression((Expression) computedMemberExpression._object, dependencies);
			parseExpression(computedMemberExpression.expression, dependencies);
		} else if (expression instanceof ConditionalExpression) {
			ConditionalExpression conditionalExpression = (ConditionalExpression) expression;
			parseExpression(conditionalExpression.test, dependencies);
			parseExpression(conditionalExpression.consequent, dependencies);
			parseExpression(conditionalExpression.alternate, dependencies);
		} else if (expression instanceof FunctionExpression) {
			FunctionExpression functionExpression = (FunctionExpression) expression;
			functionExpression.body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
		} else if (expression instanceof NewExpression) {
			NewExpression newExpression = (NewExpression) expression;
			parseExpression(newExpression.callee, dependencies);
			newExpression.arguments.forEach(seExpr -> {
				if (seExpr instanceof Expression)
					parseExpression((Expression) seExpr, dependencies);
				else if (seExpr instanceof SpreadElement)
					parseExpression(((SpreadElement)seExpr).expression, dependencies);
			});
		} else if (expression instanceof ObjectExpression) {
			ObjectExpression objectExpression = (ObjectExpression) expression;
			objectExpression.properties.forEach(objectProperty -> {
				if (objectProperty instanceof DataProperty)
					parseExpression(((DataProperty) objectProperty).expression, dependencies);
				if (objectProperty instanceof Getter)
					((Getter) objectProperty).body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
				if (objectProperty instanceof Setter)
					((Setter) objectProperty).body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
				if (objectProperty instanceof Method)
					((Method) objectProperty).body.statements.forEach(stmt -> parseStatement(stmt, dependencies));
			});
		} else if (expression instanceof StaticMemberExpression) {
			StaticMemberExpression staticMemberExpression = (StaticMemberExpression) expression;
			if (staticMemberExpression._object instanceof Expression)
				parseExpression((Expression) staticMemberExpression._object, dependencies);
		} else if (expression instanceof TemplateExpression) {
			TemplateExpression templateExpression = (TemplateExpression) expression;
			if (templateExpression.tag.isJust()) parseExpression(templateExpression.tag.just(), dependencies);
		} else if (expression instanceof UnaryExpression) {
			parseExpression(((UnaryExpression) expression).operand, dependencies);
		} else if (expression instanceof UpdateExpression) {
			UpdateExpression updateExpression = (UpdateExpression) expression;
			if (updateExpression.operand instanceof Expression)
				parseExpression((Expression) updateExpression.operand, dependencies);
		} else if (expression instanceof YieldExpression) {
			YieldExpression yieldExpression = (YieldExpression) expression;
			if (yieldExpression.expression.isJust()) parseExpression(yieldExpression.expression.just(), dependencies);
		} else if (expression instanceof YieldGeneratorExpression) {
			parseExpression(((YieldGeneratorExpression) expression).expression, dependencies);
		}
	}
	
	@Override
	protected void buildIncludesNodeTree(DependenciesNode parent) throws Exception {
		try {
			String source = fileReader.read(parent.path);
			Script script = Parser.parseScript(source);
			List<String> requireFunctionArguments = new LinkedList<>();
			for (Statement statement : script.statements)
				parseStatement(statement, requireFunctionArguments);
			for (String requireFunctionArgument : requireFunctionArguments) {
				String dependency;
				if (requireFunctionArgument.startsWith("./"))
					dependency = requireFunctionArgument.substring(2);
				else
					dependency = requireFunctionArgument;
				Path require =
					getAbsoluteExistingPath(parent.path.getParent().resolve(dependency).normalize());
				parent.dependenciesMap.put(requireFunctionArgument, require);
				DependenciesNode node = parent.root.getNode(require);
				if (node != null) {
					parent.childNodes.add(node);
				} else {
					node = new DependenciesNode();
					node.root = parent.root;
					node.path = require;
					parent.childNodes.add(node);
					buildIncludesNodeTree(node);
				}
			}
		} catch (Exception e) {
			throw new Exception("Path"+parent.path, e);
		}
	}
}
