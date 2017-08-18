// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyc.util;

import java.util.*;

import wybs.lang.CompilationUnit;
import wybs.lang.SyntacticItem;
import wybs.util.AbstractCompilationUnit;
import wybs.util.AbstractSyntacticItem;
import wybs.util.AbstractCompilationUnit.Tuple;
import wyc.util.AbstractWhileyFile.Declaration;
import wycc.util.ArrayUtils;
import wyfs.lang.Path;

/**
 * Provides the in-memory representation of a Whiley source file (a.k.a. an
 * "Abstract Syntax Tree"). This is implemented as a "heap" of syntactic items.
 * For example, consider the following simple Whiley source file:
 *
 * <pre>
 * function id(int x) -> (int y):
 *     return x
 * </pre>
 *
 * This is represented internally using a heap of syntactic items which might
 * look something like this:
 *
 * <pre>
 * [00] DECL_function(#0,#2,#6,#8)
 * [01] ITEM_utf8("id")
 * [02] ITEM_tuple(#3)
 * [03] DECL_variable(#4,#5)
 * [04] ITEM_utf8("x")
 * [05] TYPE_int
 * [06] ITEM_tuple(#7)
 * [07] DECL_variable(#8,#9)
 * [08] ITEM_utf8("y")
 * [09] TYPE_int
 * [10] STMT_block(#11)
 * [11] STMT_return(#12)
 * [12] EXPR_variable(#03)
 * </pre>
 *
 * Each of this syntactic items will additionally be associated with one or more
 * attributes (e.g. encoding line number information, etc).
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractWhileyFile<T extends CompilationUnit> extends AbstractCompilationUnit<T> {
	// DECLARATIONS: 00010000 (16) -- 00011111 (31)
	public static final int DECL_mask = 0b00010000;
	public static final int DECL_import = DECL_mask + 0;
	public static final int DECL_constant = DECL_mask + 1;
	public static final int DECL_type = DECL_mask + 2;
	public static final int DECL_function = DECL_mask + 3;
	public static final int DECL_method = DECL_mask + 4;
	public static final int DECL_property = DECL_mask + 5;
	public static final int DECL_variable = DECL_mask + 6;
	public static final int DECL_variableinitialiser = DECL_mask + 7;

	public static final int MOD_native = DECL_mask + 11;
	public static final int MOD_export = DECL_mask + 12;
	public static final int MOD_protected = DECL_mask + 13;
	public static final int MOD_private = DECL_mask + 14;
	public static final int MOD_public = DECL_mask + 15;
	// TYPES: 00100000 (32) -- 00111111 (63)
	public static final int TYPE_mask = 0b000100000;
	public static final int TYPE_void = TYPE_mask + 0;
	public static final int TYPE_any = TYPE_mask + 1;
	public static final int TYPE_null = TYPE_mask + 2;
	public static final int TYPE_bool = TYPE_mask + 3;
	public static final int TYPE_int = TYPE_mask + 4;
	public static final int TYPE_nom = TYPE_mask + 5;
	public static final int TYPE_ref = TYPE_mask + 6;
	public static final int TYPE_arr = TYPE_mask + 7;
	public static final int TYPE_rec = TYPE_mask + 8;
	public static final int TYPE_fun = TYPE_mask + 9;
	public static final int TYPE_meth = TYPE_mask + 10;
	public static final int TYPE_macro = TYPE_mask + 11;
	public static final int TYPE_inv = TYPE_mask + 12;
	public static final int TYPE_or = TYPE_mask + 13;
	public static final int TYPE_and = TYPE_mask + 14;
	public static final int TYPE_not = TYPE_mask + 15;
	public static final int TYPE_byte = TYPE_mask + 16;
	// STATEMENTS: 01000000 (64) -- 001011111 (95)
	public static final int STMT_mask = 0b01000000;
	public static final int STMT_block = STMT_mask + 0;
	public static final int STMT_namedblock = STMT_mask + 1;
	public static final int STMT_caseblock = STMT_mask + 2;
	public static final int STMT_assert = STMT_mask + 3;
	public static final int STMT_assign = STMT_mask + 4;
	public static final int STMT_assume = STMT_mask + 5;
	public static final int STMT_debug = STMT_mask + 6;
	public static final int STMT_skip = STMT_mask + 7;
	public static final int STMT_break = STMT_mask + 8;
	public static final int STMT_continue = STMT_mask + 9;
	public static final int STMT_dowhile = STMT_mask + 10;
	public static final int STMT_fail = STMT_mask + 11;
	public static final int STMT_for = STMT_mask + 12;
	public static final int STMT_foreach = STMT_mask + 13;
	public static final int STMT_if = STMT_mask + 14;
	public static final int STMT_ifelse = STMT_mask + 15;
	public static final int STMT_return = STMT_mask + 16;
	public static final int STMT_switch = STMT_mask + 17;
	public static final int STMT_while = STMT_mask + 18;
	// EXPRESSIONS: 01100000 (96) -- 10011111 (159)
	public static final int EXPR_mask = 0b01100000;
	public static final int EXPR_var = EXPR_mask + 0;
	public static final int EXPR_staticvar = EXPR_mask + 1;
	public static final int EXPR_const = EXPR_mask + 2;
	public static final int EXPR_cast = EXPR_mask + 3;
	public static final int EXPR_invoke = EXPR_mask + 4;
	public static final int EXPR_qualifiedinvoke = EXPR_mask + 5;
	public static final int EXPR_indirectinvoke = EXPR_mask + 6;
	// LOGICAL
	public static final int EXPR_not = EXPR_mask + 8;
	public static final int EXPR_and = EXPR_mask + 9;
	public static final int EXPR_or = EXPR_mask + 10;
	public static final int EXPR_implies = EXPR_mask + 11;
	public static final int EXPR_iff = EXPR_mask + 12;
	public static final int EXPR_exists = EXPR_mask + 13;
	public static final int EXPR_forall = EXPR_mask + 14;
	// COMPARATORS
	public static final int EXPR_eq = EXPR_mask + 16;
	public static final int EXPR_neq = EXPR_mask + 17;
	public static final int EXPR_lt = EXPR_mask + 18;
	public static final int EXPR_lteq = EXPR_mask + 19;
	public static final int EXPR_gt = EXPR_mask + 20;
	public static final int EXPR_gteq = EXPR_mask + 21;
	public static final int EXPR_is = EXPR_mask + 22;
	// ARITHMETIC
	public static final int EXPR_neg = EXPR_mask + 24;
	public static final int EXPR_add = EXPR_mask + 25;
	public static final int EXPR_sub = EXPR_mask + 26;
	public static final int EXPR_mul = EXPR_mask + 27;
	public static final int EXPR_div = EXPR_mask + 28;
	public static final int EXPR_rem = EXPR_mask + 29;
	// BITWISE
	public static final int EXPR_bitwisenot = EXPR_mask + 32;
	public static final int EXPR_bitwiseand = EXPR_mask + 33;
	public static final int EXPR_bitwiseor = EXPR_mask + 34;
	public static final int EXPR_bitwisexor = EXPR_mask + 35;
	public static final int EXPR_bitwiseshl = EXPR_mask + 36;
	public static final int EXPR_bitwiseshr = EXPR_mask + 37;
	// REFERENCES
	public static final int EXPR_deref = EXPR_mask + 40;
	public static final int EXPR_new = EXPR_mask + 41;
	public static final int EXPR_lambdaconst = EXPR_mask + 42;
	public static final int EXPR_lambdainit = EXPR_mask + 43;
	// RECORDS
	public static final int EXPR_recfield = EXPR_mask + 48;
	public static final int EXPR_recupdt = EXPR_mask + 49;
	public static final int EXPR_recinit = EXPR_mask + 50;
	// ARRAYS
	public static final int EXPR_arridx = EXPR_mask + 56;
	public static final int EXPR_arrlen = EXPR_mask + 57;
	public static final int EXPR_arrupdt = EXPR_mask + 58;
	public static final int EXPR_arrgen = EXPR_mask + 59;
	public static final int EXPR_arrinit = EXPR_mask + 60;

	// =========================================================================
	// Constructors
	// =========================================================================

	public AbstractWhileyFile(Path.Entry<T> entry) {
		super(entry);
	}

	public AbstractWhileyFile(Path.Entry<T> entry, CompilationUnit other) {
		super(entry,other);
	}

	// =========================================================================
	// Accessors
	// =========================================================================

	public Tuple<Declaration> getDeclarations() {
		throw new IllegalArgumentException("GOT HERE");
	}

	public <S extends Declaration.Named> S getDeclaration(Identifier name, Type signature, Class<S> kind) {
		List<S> matches = super.getSyntacticItems(kind);
		for (int i = 0; i != matches.size(); ++i) {
			S match = matches.get(i);
			if (match.getName().equals(name)) {
				if (signature != null && signature.equals(match.getSignature())) {
					return match;
				} else if (signature == match.getSignature()) {
					return match;
				}
			}
		}
		throw new IllegalArgumentException("unknown declarataion (" + name + "," + signature +")");
	}

	// ============================================================
	// Declarations
	// ============================================================
	public static interface Declaration extends CompilationUnit.Declaration {

		/**
		 * Represents an import declaration in a Whiley source file, which has
		 * the form:
		 *
		 * <pre>
		 * ImportDeclaration ::= "import" [Identifier|Star "from"] Identifier ('.' Identifier|'*')*
		 * </pre>
		 *
		 * The following illustrates a simple import statement:
		 *
		 * <pre>
		 * import println from std.io
		 * </pre>
		 *
		 * Here, the module is <code>std.io</code> and the symbol imported is
		 * <code>println</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Import extends AbstractSyntacticItem implements Declaration {
			public Import(Identifier... components) {
				super(DECL_import, components);
			}

			public Identifier[] getComponents() {
				return (Identifier[]) getOperands();
			}

			@Override
			public Identifier getOperand(int i) {
				return (Identifier) super.getOperand(i);
			}

			@Override
			public Import clone(SyntacticItem[] operands) {
				return new Import(ArrayUtils.toArray(Identifier.class, operands));
			}

			@Override
			public String toString() {
				String r = "import ";
				for (int i = 0; i != size(); ++i) {
					if (i != 0) {
						r += ".";
					}
					Identifier component = getOperand(i);
					if (component == null) {
						r += "*";
					} else {
						r += component.get();
					}
				}
				return r;
			}
		}

		/**
		 * A named declaration has an additional symbol name associated with it
		 *
		 * @author David J. Pearce
		 *
		 */
		public static abstract class Named extends AbstractSyntacticItem implements Declaration {

			public Named(int opcode, Tuple<Modifier> modifiers, Identifier name, SyntacticItem... rest) {
				super(opcode, ArrayUtils.append(new SyntacticItem[] { modifiers, name }, rest));
			}

			@SuppressWarnings("unchecked")
			public Tuple<Modifier> getModifiers() {
				return (Tuple<Modifier>) super.getOperand(2);
			}

			public Identifier getName() {
				return (Identifier) super.getOperand(1);
			}

			public abstract AbstractWhileyFile.Type getSignature();
		}

		/**
		 * Represents a constant declaration in a Whiley source file, which has
		 * the form:
		 *
		 * <pre>
		 * ConstantDeclaration ::= "constant" Identifier "is" Expression
		 * </pre>
		 *
		 * A simple example to illustrate is:
		 *
		 * <pre>
		 * constant PI is 3.141592654
		 * </pre>
		 *
		 * Here, we are defining a constant called <code>PI</code> which
		 * represents the decimal value "3.141592654". Constant declarations may
		 * also have modifiers, such as <code>public</code> and
		 * <code>private</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public class Constant extends Named {
			public Constant(Tuple<Modifier> modifiers, Identifier name, Expr cexpr) {
				super(DECL_constant, modifiers, name, cexpr);
			}

			public Expr getConstantExpr() {
				return (Expr) super.getOperand(1);
			}

			@Override
			public AbstractWhileyFile.Type getSignature() {
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Constant((Tuple<Modifier>) operands[0], (Identifier) operands[1], (Expr) operands[2]);
			}

		}

		/**
		 * Represents a <i>function declaration</i> or <i>method declaration</i>
		 * in a Whiley source file which have the form:
		 *
		 * <pre>
		 * FunctionDeclaration ::= "function" TypePattern "=>" TypePattern (FunctionMethodClause)* ':' NewLine Block
		 *
		 * MethodDeclaration ::= "method" TypePattern "=>" TypePattern (FunctionMethodClause)* ':' NewLine Block
		 *
		 * FunctionMethodClause ::= "throws" Type | "requires" Expression | "ensures" Expression
		 * </pre>
		 *
		 * Here, the first type pattern (i.e. before "=>") is referred to as the
		 * "parameter", whilst the second is referred to as the "return". There
		 * are three kinds of option clause:
		 *
		 * <ul>
		 * <li><b>Throws clause</b>. This defines the exceptions which may be
		 * thrown by this function. Multiple clauses may be given, and these are
		 * taken together as a union. Furthermore, the convention is to specify
		 * the throws clause before the others.</li>
		 * <li><b>Requires clause</b>. This defines a constraint on the
		 * permissible values of the parameters on entry to the function or
		 * method, and is often referred to as the "precondition". This
		 * expression may refer to any variables declared within the parameter
		 * type pattern. Multiple clauses may be given, and these are taken
		 * together as a conjunction. Furthermore, the convention is to specify
		 * the requires clause(s) before any ensure(s) clauses.</li>
		 * <li><b>Ensures clause</b>. This defines a constraint on the
		 * permissible values of the the function or method's return value, and
		 * is often referred to as the "postcondition". This expression may
		 * refer to any variables declared within either the parameter or return
		 * type pattern. Multiple clauses may be given, and these are taken
		 * together as a conjunction. Furthermore, the convention is to specify
		 * the requires clause(s) after the others.</li>
		 * </ul>
		 *
		 * <p>
		 * The following function declaration provides a small example to
		 * illustrate:
		 * </p>
		 *
		 * <pre>
		 * function max(int x, int y) -> (int z)
		 * // return must be greater than either parameter
		 * ensures x <= z && y <= z
		 * // return must equal one of the parmaeters
		 * ensures x == z || y == z:
		 *     ...
		 * </pre>
		 *
		 * <p>
		 * Here, we see the specification for the well-known <code>max()</code>
		 * function which returns the largest of its parameters. This does not
		 * throw any exceptions, and does not enforce any preconditions on its
		 * parameters.
		 * </p>
		 *
		 * <p>
		 * Function and method declarations may also have modifiers, such as
		 * <code>public</code> and <code>private</code>.
		 * </p>
		 */
		public static abstract class Callable extends Named {

			public Callable(int opcode, Tuple<Modifier> modifiers, Identifier name,
					Tuple<Variable> parameters, Tuple<Variable> returns, SyntacticItem... rest) {
				super(opcode, modifiers, name, ArrayUtils.append(new SyntacticItem[] { parameters, returns}, rest));
			}

			@SuppressWarnings("unchecked")
			public Tuple<Variable> getParameters() {
				return (Tuple<Variable>) getOperand(2);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Variable> getReturns() {
				return (Tuple<Variable>) getOperand(3);
			}

			@Override
			public abstract AbstractWhileyFile.Type.Callable getSignature();
		}

		public static abstract class FunctionOrMethod extends Callable {

			public FunctionOrMethod(int opcode, Tuple<Modifier> modifiers, Identifier name, Tuple<Variable> parameters,
					Tuple<Variable> returns, Tuple<Expr> requires, Tuple<Expr> ensures, Stmt.Block body, SyntacticItem... rest) {
				super(opcode, modifiers, name, parameters, returns,
						ArrayUtils.append(new SyntacticItem[] { requires, ensures, body }, rest));
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getRequires() {
				return (Tuple<Expr>) getOperand(4);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getEnsures() {
				return (Tuple<Expr>) getOperand(5);
			}

			public Stmt.Block getBody() {
				return (Stmt.Block) getOperand(6);
			}
		}

		/**
		 * Represents a function declaration in a Whiley source file. For
		 * example:
		 *
		 * <pre>
		 * function f(int x) -> (int y)
		 * // Parameter must be positive
		 * requires x > 0
		 * // Return must be negative
		 * ensures y < 0:
		 *    // body
		 *    return -x
		 * </pre>
		 *
		 * <p>
		 * Here, a function <code>f</code> is defined which accepts only
		 * positive integers and returns only negative integers. The special
		 * variable <code>$</code> is used to refer to the return value.
		 * Functions in Whiley may not have side-effects (i.e. they are
		 * <code>pure functions</code>).
		 * </p>
		 *
		 * <p>
		 * Function declarations may also have modifiers, such as
		 * <code>public</code> and <code>private</code>.
		 * </p>
		 *
		 * <p>
		 * <b>NOTE</b> see {@link Callable} for more
		 * information.
		 * </p>
		 *
		 * @see Callable
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Function extends FunctionOrMethod {

			public Function(Tuple<Modifier> modifiers, Identifier name, Tuple<Variable> parameters,
					Tuple<Variable> returns, Tuple<Expr> requires, Tuple<Expr> ensures, Stmt.Block body) {
				super(DECL_function, modifiers, name, parameters, returns, requires, ensures, body);
			}

			@Override
			public AbstractWhileyFile.Type.Function getSignature() {
				// FIXME: a better solution would be to have an actual signature
				// object
				Tuple<AbstractWhileyFile.Type> projectedParameters = getParameters().project(0, AbstractWhileyFile.Type.class);
				Tuple<AbstractWhileyFile.Type> projectedReturns = getReturns().project(0, AbstractWhileyFile.Type.class);
				return new AbstractWhileyFile.Type.Function(projectedParameters, projectedReturns);
			}

			@Override
			@SuppressWarnings("unchecked")
			public Function clone(SyntacticItem[] operands) {
				return new Function((Tuple<Modifier>) operands[0], (Identifier) operands[1],
						(Tuple<Variable>) operands[2], (Tuple<Variable>) operands[3], (Tuple<Expr>) operands[4],
						(Tuple<Expr>) operands[5], (Stmt.Block) operands[6]);
			}
		}

		/**
		 * Represents a method declaration in a Whiley source file. For example:
		 *
		 * <pre>
		 * method m(int x) -> (int y)
		 * // Parameter must be positive
		 * requires x > 0
		 * // Return must be negative
		 * ensures $ < 0:
		 *    // body
		 *    return -x
		 * </pre>
		 *
		 * <p>
		 * Here, a method <code>m</code> is defined which accepts only positive
		 * integers and returns only negative integers. The special variable
		 * <code>$</code> is used to refer to the return value. Unlike
		 * functions, methods in Whiley may have side-effects.
		 * </p>
		 *
		 * <p>
		 * Method declarations may also have modifiers, such as
		 * <code>public</code> and <code>private</code>.
		 * </p>
		 *
		 * <p>
		 * <b>NOTE</b> see {@link Callable} for more
		 * information.
		 * </p>
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Method extends FunctionOrMethod {

			public Method(Tuple<Modifier> modifiers, Identifier name, Tuple<Variable> parameters,
					Tuple<Variable> returns, Tuple<Expr> requires,
					Tuple<Expr> ensures, Stmt.Block body, Tuple<Identifier> lifetimes) {
				super(DECL_method, modifiers, name, parameters, returns, requires, ensures, body, lifetimes);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getLifetimes() {
				return (Tuple<Identifier>) getOperand(7);
			}

			@Override
			public AbstractWhileyFile.Type.Method getSignature() {
				// FIXME: a better solution would be to have an actual signature
				// object
				Tuple<AbstractWhileyFile.Type> projectedParameters = getParameters().project(0,
						AbstractWhileyFile.Type.class);
				Tuple<AbstractWhileyFile.Type> projectedReturns = getReturns().project(0,
						AbstractWhileyFile.Type.class);
				return new AbstractWhileyFile.Type.Method(projectedParameters, projectedReturns, new Tuple<>(),
						getLifetimes());
			}

			@SuppressWarnings("unchecked")
			@Override
			public Method clone(SyntacticItem[] operands) {
				return new Method((Tuple<Modifier>) operands[0], (Identifier) operands[1],
						(Tuple<Variable>) operands[2], (Tuple<Variable>) operands[3], (Tuple<Expr>) operands[4],
						(Tuple<Expr>) operands[5], (Stmt.Block) operands[6], (Tuple<Identifier>) operands[7]);
			}

			@Override
			public String toString() {
				return "method" + getSignature();
			}
		}

		public static class Property extends Callable {

			public Property(Tuple<Modifier> modifiers, Identifier name, Tuple<Variable> parameters,
					Tuple<Expr> invariant) {
				super(DECL_property, modifiers, name, parameters, new Tuple<Variable>(), invariant);
			}

			@Override
			public AbstractWhileyFile.Type.Property getSignature() {
				// FIXME: a better solution would be to have an actual signature
				// object
				Tuple<AbstractWhileyFile.Type> projectedParameters = getParameters().project(0, AbstractWhileyFile.Type.class);
				Tuple<AbstractWhileyFile.Type> projectedReturns = getReturns().project(0, AbstractWhileyFile.Type.class);
				return new AbstractWhileyFile.Type.Property(projectedParameters, projectedReturns);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getInvariant() {
				return (Tuple<Expr>) getOperand(4);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Property clone(SyntacticItem[] operands) {
				return new Property((Tuple<Modifier>) operands[0], (Identifier) operands[1],
						(Tuple<Variable>) operands[2], (Tuple<Expr>) operands[4]);
			}
		}

		/**
		 * Represents a type declaration in a Whiley source file, which has the
		 * form:
		 *
		 * <pre>
		 * "type" Identifier "is" TypePattern ["where" Expression]
		 * </pre>
		 *
		 * Here, the type pattern specifies a type which may additionally be
		 * adorned with variable names. The "where" clause is optional and is
		 * often referred to as the type's "constraint". Variables defined
		 * within the type pattern may be used within this constraint
		 * expressions. A simple example to illustrate is:
		 *
		 * <pre>
		 * type nat is (int x) where x >= 0
		 * </pre>
		 *
		 * Here, we are defining a <i>constrained type</i> called
		 * <code>nat</code> which represents the set of natural numbers (i.e the
		 * non-negative integers). Type declarations may also have modifiers,
		 * such as <code>public</code> and <code>private</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Type extends Named {

			public Type(Tuple<Modifier> modifiers, Identifier name, Variable vardecl, Tuple<Expr> invariant) {
				super(DECL_type, modifiers, name, vardecl, invariant);
			}

			public Variable getVariableDeclaration() {
				return (Variable) getOperand(2);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getInvariant() {
				return (Tuple<Expr>) getOperand(2);
			}

			@Override
			public AbstractWhileyFile.Type getSignature() {
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			public Type clone(SyntacticItem[] operands) {
				return new Type((Tuple<Modifier>) operands[0], (Identifier) operands[1], (Variable) operands[2],
						(Tuple<Expr>) operands[3]);
			}
		}

		// ============================================================
		// Variable Declaration
		// ============================================================

		/**
		 * Represents a variable declaration which has the form:
		 *
		 * <pre>
		 * Type Identifier ['=' Expression] NewLine
		 * </pre>
		 *
		 * The optional <code>Expression</code> assignment is referred to as an
		 * <i>initialiser</i>. If an initialiser is given, then this will be
		 * evaluated and assigned to the variable when the declaration is executed.
		 * Some example declarations:
		 *
		 * <pre>
		 * int x
		 * int y = 1
		 * int z = x + y
		 * </pre>
		 *
		 * Observe that, unlike C and Java, declarations that declare multiple
		 * variables (separated by commas) are not permitted.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Variable extends Named implements Stmt {
			public Variable(Tuple<Modifier> modifiers, Identifier name, AbstractWhileyFile.Type type) {
				super(DECL_variable, modifiers, name, type);
			}

			public Variable(Tuple<Modifier> modifiers, Identifier name, AbstractWhileyFile.Type type, Expr initialiser) {
				super(DECL_variableinitialiser, modifiers, name, type);
			}

			public boolean hasInitialiser() {
				return getOpcode() == DECL_variableinitialiser;
			}

			public AbstractWhileyFile.Type getType() {
				return (AbstractWhileyFile.Type) getOperand(2);
			}

			@Override
			public AbstractWhileyFile.Type getSignature() {
				return null;
			}


			public Expr getInitialiser() {
				return (Expr) getOperand(3);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Variable clone(SyntacticItem[] operands) {
				return new Variable((Tuple<Modifier>) operands[0], (Identifier) operands[1],
						(AbstractWhileyFile.Type) operands[2]);
			}
		}
	}

	// ============================================================
	// Stmt
	// ============================================================

	/**
	 * Provides classes for representing statements in Whiley's source language.
	 * Examples include <i>assignments</i>, <i>for-loops</i>, <i>conditions</i>,
	 * etc. Each class is an instance of <code>SyntacticElement</code> and, hence,
	 * can be adorned with certain information (such as source location, etc).
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Stmt extends SyntacticItem {

		public static class Block extends AbstractSyntacticItem implements Stmt {
			public Block(Stmt... stmts) {
				super(STMT_block, stmts);
			}

			@Override
			public Stmt getOperand(int i) {
				return (Stmt) super.getOperand(i);
			}

			@Override
			public Stmt[] getOperands() {
				return (Stmt[]) super.getOperands();
			}

			@Override
			public Block clone(SyntacticItem[] operands) {
				return new Block(ArrayUtils.toArray(Stmt.class, operands));
			}
		}

		/**
		 * Represents a named block, which has the form:
		 *
		 * <pre>
		 * NamedBlcok ::= LifetimeIdentifier ':' NewLine Block
		 * </pre>
		 *
		 * As an example:
		 *
		 * <pre>
		 * function sum():
		 *   &this:int x = new:this x
		 *   myblock:
		 *     &myblock:int y = new:myblock y
		 * </pre>
		 */
		public static class NamedBlock extends AbstractSyntacticItem implements Stmt {
			public NamedBlock(Identifier name, Stmt.Block block) {
				super(STMT_namedblock, name, block);
			}

			public Identifier getName() {
				return (Identifier) super.getOperand(0);
			}

			public Block getBlock() {
				return (Block) super.getOperand(1);
			}

			@Override
			public NamedBlock clone(SyntacticItem[] operands) {
				return new NamedBlock((Identifier) operands[0], (Block) operands[1]);
			}
		}

		/**
		 * Represents a assert statement of the form <code>assert e</code>, where
		 * <code>e</code> is a boolean expression. The following illustrates:
		 *
		 * <pre>
		 * function abs(int x) -> int:
		 *     if x < 0:
		 *         x = -x
		 *     assert x >= 0
		 *     return x
		 * </pre>
		 *
		 * Assertions are either statically checked by the verifier, or turned into
		 * runtime checks.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Assert extends AbstractSyntacticItem implements Stmt {
			public Assert(Expr condition) {
				super(STMT_assert, condition);
			}

			public Expr getCondition() {
				return (Expr) super.getOperand(0);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Assert((Expr) operands[0]);
			}
		}

		/**
		 * Represents an assignment statement of the form <code>lhs = rhs</code>.
		 * Here, the <code>rhs</code> is any expression, whilst the <code>lhs</code>
		 * must be an <code>LVal</code> --- that is, an expression permitted on the
		 * left-side of an assignment. The following illustrates different possible
		 * assignment statements:
		 *
		 * <pre>
		 * x = y       // variable assignment
		 * x.f = y     // field assignment
		 * x[i] = y    // list assignment
		 * x[i].f = y  // compound assignment
		 * </pre>
		 *
		 * The last assignment here illustrates that the left-hand side of an
		 * assignment can be arbitrarily complex, involving nested assignments into
		 * lists and records.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Assign extends AbstractSyntacticItem implements Stmt {
			public Assign(Tuple<LVal> lvals, Tuple<Expr> rvals) {
				super(STMT_assign, lvals, rvals);
			}

			@SuppressWarnings("unchecked")
			public Tuple<LVal> getLeftHandSide() {
				return (Tuple<LVal>) super.getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getRightHandSide() {
				return (Tuple<Expr>) super.getOperand(1);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Assign((Tuple<LVal>) operands[0], (Tuple<Expr>) operands[1]);
			}
		}

		/**
		 * Represents an assume statement of the form <code>assume e</code>, where
		 * <code>e</code> is a boolean expression. The following illustrates:
		 *
		 * <pre>
		 * function abs(int x) -> int:
		 *     if x < 0:
		 *         x = -x
		 *     assume x >= 0
		 *     return x
		 * </pre>
		 *
		 * Assumptions are assumed by the verifier and, since this may be unsound,
		 * always turned into runtime checks.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Assume extends AbstractSyntacticItem implements Stmt {
			public Assume(Expr condition) {
				super(STMT_assume, condition);
			}

			public Expr getCondition() {
				return (Expr) super.getOperand(0);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Assume((Expr) operands[0]);
			}
		}

		public static class Debug extends AbstractSyntacticItem implements Stmt {
			public Debug(Expr condition) {
				super(STMT_debug, condition);
			}

			public Expr getCondition() {
				return (Expr) super.getOperand(0);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Debug((Expr) operands[0]);
			}
		}

		public static class Skip extends AbstractSyntacticItem implements Stmt {
			public Skip() {
				super(STMT_skip);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Skip();
			}
		}

		public static class Break extends AbstractSyntacticItem implements Stmt {
			public Break() {
				super(STMT_break);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Break();
			}
		}

		public static class Continue extends AbstractSyntacticItem implements Stmt {
			public Continue() {
				super(STMT_continue);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Continue();
			}
		}

		/**
		 * Represents a do-while statement whose body is made up from a block of
		 * statements separated by indentation. As an example:
		 *
		 * <pre>
		 * function sum([int] xs) -> int
		 * requires |xs| > 0:
		 *   int r = 0
		 *   int i = 0
		 *   do:
		 *     r = r + xs[i]
		 *     i = i + 1
		 *   while i < |xs| where i >= 0
		 *   return r
		 * </pre>
		 *
		 * Here, the <code>where</code> is optional, and commonly referred to as the
		 * <i>loop invariant</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class DoWhile extends AbstractSyntacticItem implements Stmt {
			public DoWhile(Expr condition, Tuple<Expr> invariant, Stmt.Block body) {
				super(STMT_dowhile, condition, invariant, body);
			}

			public Expr getCondition() {
				return (Expr) super.getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getInvariant() {
				return (Tuple<Expr>) super.getOperand(1);
			}

			public Stmt.Block getBody() {
				return (Stmt.Block) super.getOperand(2);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new DoWhile((Expr) operands[0], (Tuple<Expr>) operands[1], (Stmt.Block) operands[2]);
			}
		}

		/**
		 * Represents a fail statement.
		 */
		public static class Fail extends AbstractSyntacticItem implements Stmt {
			public Fail() {
				super(STMT_fail);
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Fail();
			}
		}

		/**
		 * Represents a classical if-else statement, which is has the form:
		 *
		 * <pre>
		 * "if" Expression ':' NewLine Block ["else" ':' NewLine Block]
		 * </pre>
		 *
		 * The first expression is referred to as the <i>condition</i>, while the
		 * first block is referred to as the <i>true branch</i>. The optional second
		 * block is referred to as the <i>false branch</i>. The following
		 * illustrates:
		 *
		 * <pre>
		 * function max(int x, int y) -> int:
		 *   if(x > y):
		 *     return x
		 *   else if(x == y):
		 *   	return 0
		 *   else:
		 *     return y
		 * </pre>
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class IfElse extends AbstractSyntacticItem implements Stmt {
			public IfElse(Expr condition, Stmt.Block trueBranch) {
				super(STMT_if, condition, trueBranch);
			}
			public IfElse(Expr condition, Stmt.Block trueBranch, Stmt.Block falseBranch) {
				super(STMT_ifelse, condition, trueBranch, falseBranch);
			}

			public boolean hasFalseBranch() {
				return getOpcode() == STMT_ifelse;
			}

			public Expr getCondition() {
				return (Expr) super.getOperand(0);
			}

			public Stmt.Block getTrueBranch() {
				return (Stmt.Block) super.getOperand(1);
			}

			public Stmt.Block getFalseBranch() {
				return (Stmt.Block) super.getOperand(2);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				if (operands.length == 2) {
					return new IfElse((Expr) operands[0], (Stmt.Block) operands[1]);
				} else {
					return new IfElse((Expr) operands[0], (Stmt.Block) operands[1], (Stmt.Block) operands[2]);
				}
			}
		}

		/**
		 * Represents a return statement, which has the form:
		 *
		 * <pre>
		 * ReturnStmt ::= "return" [Expression] NewLine
		 * </pre>
		 *
		 * The optional expression is referred to as the <i>return value</i>. Note
		 * that, the returned expression (if there is one) must begin on the same
		 * line as the return statement itself.
		 *
		 * The following illustrates:
		 *
		 * <pre>
		 * function f(int x) -> int:
		 * 	  return x + 1
		 * </pre>
		 *
		 * Here, we see a simple <code>return</code> statement which returns an
		 * <code>int</code> value.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Return extends AbstractSyntacticItem implements Stmt {
			public Return(Expr... returns) {
				super(STMT_return, returns);
			}

			@Override
			public Expr getOperand(int i) {
				return (Expr) super.getOperand(i);
			}

			@Override
			public Expr[] getOperands() {
				return (Expr[]) super.getOperands();
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Return(ArrayUtils.toArray(Expr.class, operands));
			}
		}

		public static class Switch extends AbstractSyntacticItem implements Stmt {
			public Switch(Expr condition, Tuple<Case> cases) {
				super(STMT_switch,condition,cases);
			}

			public Expr getCondition() {
				return (Expr) getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Case> getCases() {
				return (Tuple<Case>) getOperand(1);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Switch((Expr) operands[0], (Tuple<Case>) operands[1]);
			}
		}

		public static class Case extends AbstractSyntacticItem {

			public Case(Tuple<Expr> conditions, Stmt.Block block) {
				super(STMT_caseblock, conditions, block);
			}

			public boolean isDefault() {
				return getConditions().size() == 0;
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getConditions() {
				return (Tuple<Expr>) getOperand(0);
			}

			public Stmt.Block getBlock() {
				return (Stmt.Block) getOperand(1);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Case((Tuple<Expr>) operands[0], (Stmt.Block) operands[1]);
			}
		}

		/**
		 * Represents a while statement, which has the form:
		 *
		 * <pre>
		 * WhileStmt ::= "while" Expression (where Expression)* ':' NewLine Block
		 * </pre>
		 *
		 * As an example:
		 *
		 * <pre>
		 * function sum([int] xs) -> int:
		 *   int r = 0
		 *   int i = 0
		 *   while i < |xs| where i >= 0:
		 *     r = r + xs[i]
		 *     i = i + 1
		 *   return r
		 * </pre>
		 *
		 * The optional <code>where</code> clause(s) are commonly referred to as the
		 * "loop invariant". When multiple clauses are given, these are combined
		 * using a conjunction. The combined invariant defines a condition which
		 * must be true on every iteration of the loop.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class While extends AbstractSyntacticItem implements Stmt {
			public While(Expr condition, Tuple<Expr> invariant, Stmt.Block body) {
				super(STMT_while, condition, invariant, body);
			}

			public Expr getCondition() {
				return (Expr) super.getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getInvariant() {
				return (Tuple<Expr>) super.getOperand(1);
			}

			public Stmt.Block getBody() {
				return (Stmt.Block) super.getOperand(2);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new While((Expr) operands[0], (Tuple<Expr>) operands[1], (Stmt.Block) operands[2]);
			}
		}
	}

	public interface LVal extends Expr {

	}

	public interface Expr extends Stmt {

		// =========================================================================
		// General Expressions
		// =========================================================================

		/**
		 * Represents a cast expression of the form "<code>(T) e</code>" where
		 * <code>T</code> is the <i>cast type</i> and <code>e</code> the
		 * <i>casted expression</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Cast extends AbstractSyntacticItem implements Expr {
			public Cast(Type type, Expr rhs) {
				super(EXPR_cast, type, rhs);
			}

			public Type getCastType() {
				return (Type) super.getOperand(0);
			}

			public Expr getCastedExpr() {
				return (Expr) super.getOperand(1);
			}

			@Override
			public Cast clone(SyntacticItem[] operands) {
				return new Cast((Type) operands[0], (Expr) operands[1]);
			}

			@Override
			public String toString() {
				return "(" + getCastType() + ") " + getCastedExpr();
			}
		}

		/**
		 * Represents the use of a constant within some expression. For example,
		 * in <code>x + 1</code> the expression <code>1</code> is a constant
		 * expression.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Constant extends AbstractSyntacticItem implements Expr {
			public Constant(Value value) {
				super(EXPR_const, value);
			}

			public Value getValue() {
				return (Value) getOperand(0);
			}

			@Override
			public Constant clone(SyntacticItem[] operands) {
				return new Constant((Value) operands[0]);
			}

			@Override
			public String toString() {
				return getValue().toString();
			}
		}

		public static class StaticVariableAccess extends AbstractSyntacticItem implements Expr {
			public StaticVariableAccess(Name name) {
				super(EXPR_staticvar, name);
			}

			public Name getName() {
				return (Name) getOperand(0);
			}

			@Override
			public StaticVariableAccess clone(SyntacticItem[] operands) {
				return new StaticVariableAccess((Name) operands[0]);
			}

			@Override
			public String toString() {
				return getName().toString();
			}
		}

		/**
		 * Represents a <i>type test expression</i> of the form
		 * "<code>e is T</code>" where <code>e</code> is the <i>test
		 * expression</i> and <code>T</code> is the <i>test type</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Is extends AbstractSyntacticItem implements Expr {
			public Is(Expr lhs, Type rhs) {
				super(EXPR_is, lhs, rhs);
			}

			public Expr getTestExpr() {
				return (Expr) getOperand(0);
			}

			public Type getTestType() {
				return (Type) getOperand(1);
			}

			@Override
			public Is clone(SyntacticItem[] operands) {
				return new Is((Expr) operands[0], (Type) operands[1]);
			}

			@Override
			public String toString() {
				return getTestExpr() + " is " + getTestType();
			}
		}

		/**
		 * Represents an invocation of the form "<code>x.y.f(e1,..en)</code>".
		 * Here, <code>x.y.f</code> constitute a <i>partially-</i> or
		 * <i>fully-qualified name</i> and <code>e1</code> ... <code>en</code>
		 * are the <i>argument expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Invoke extends AbstractSyntacticItem implements Expr {

			public Invoke(Name name, Tuple<Identifier> lifetimes, Tuple<Expr> arguments) {
				super(EXPR_invoke, name, lifetimes, arguments);
			}

			public Invoke(Name name, Tuple<Identifier> lifetimes, Tuple<Expr> arguments, Type.Callable type) {
				super(EXPR_qualifiedinvoke, name, lifetimes, arguments, type);
			}

			public Name getName() {
				return (Name) getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getLifetimes() {
				return (Tuple<Identifier>) getOperand(1);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getArguments() {
				return (Tuple<Expr>) getOperand(2);
			}

			public Type.Callable getSignatureType() {
				return (Type.Callable) getOperand(3);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Invoke clone(SyntacticItem[] operands) {
				if(operands.length == 3) {
					return new Invoke((Name) operands[0], (Tuple<Identifier>) operands[1], (Tuple<Expr>) operands[2]);
				} else {
					return new Invoke((Name) operands[0], (Tuple<Identifier>) operands[1], (Tuple<Expr>) operands[2],
							(Type.Callable) operands[3]);
				}
			}

			@Override
			public String toString() {
				String r = getName().toString();
				r += getArguments();
				return r;
			}
		}

		/**
		 * Represents an indirect invocation of the form
		 * "<code>x.y(e1,..en)</code>". Here, <code>x.y</code> returns a
		 * function value and <code>e1</code> ... <code>en</code> are the
		 * <i>argument expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class IndirectInvoke extends AbstractSyntacticItem implements Expr {

			public IndirectInvoke(Expr source, Tuple<Identifier> lifetimes, Tuple<Expr> arguments) {
				super(EXPR_indirectinvoke, source, lifetimes, arguments);
			}

			public Expr getSource() {
				return (Expr) getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getLifetimes() {
				return (Tuple<Identifier>) getOperand(1);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Expr> getArguments() {
				return (Tuple<Expr>) getOperand(2);
			}

			@SuppressWarnings("unchecked")
			@Override
			public IndirectInvoke clone(SyntacticItem[] operands) {
				return new IndirectInvoke((Expr) operands[0], (Tuple<Identifier>) operands[1], (Tuple<Expr>) operands[2]);
			}

			@Override
			public String toString() {
				String r = getSource().toString();
				r += getArguments();
				return r;
			}
		}

		/**
		 * Represents an abstract operator expression over one or more
		 * <i>operand expressions</i>. For example. in <code>arr[i+1]</code> the
		 * expression <code>i+1</code> is an operator expression.
		 *
		 * @author David J. Pearce
		 *
		 */
		public abstract static class Operator extends AbstractSyntacticItem implements Expr {
			public Operator(int opcode, Expr... operands) {
				super(opcode, operands);
			}

			@Override
			public Expr getOperand(int i) {
				return (Expr) super.getOperand(i);
			}

			@Override
			public Expr[] getOperands() {
				return (Expr[]) super.getOperands();
			}

			@Override
			public abstract Expr clone(SyntacticItem[] operands);
		}

		/**
		 * Represents an abstract quantified expression of the form
		 * "<code>forall(T v1, ... T vn).e</code>" or
		 * "<code>exists(T v1, ... T vn).e</code>" where <code>T1 v1</code> ...
		 * <code>Tn vn</code> are the <i>quantified variable declarations</i>
		 * and <code>e</code> is the body.
		 *
		 * @author David J. Pearce
		 *
		 */
		public abstract static class Quantifier extends AbstractSyntacticItem implements Expr {
			public Quantifier(int opcode, Declaration.Variable[] parameters, Expr body) {
				super(opcode, new Tuple<>(parameters), body);
			}

			public Quantifier(int opcode, Tuple<Declaration.Variable> parameters, Expr body) {
				super(opcode, parameters, body);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Declaration.Variable> getParameters() {
				return (Tuple<Declaration.Variable>) getOperand(0);
			}

			public Expr getBody() {
				return (Expr) getOperand(1);
			}

			@Override
			public abstract Expr clone(SyntacticItem[] operands);
		}

		/**
		 * Represents an unbounded universally quantified expression of the form
		 * "<code>forall(T v1, ... T vn).e</code>" where <code>T1 v1</code> ...
		 * <code>Tn vn</code> are the <i>quantified variable declarations</i>
		 * and <code>e</code> is the body.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class UniversalQuantifier extends Quantifier {
			public UniversalQuantifier(Declaration.Variable[] parameters, Expr body) {
				super(EXPR_forall, new Tuple<>(parameters), body);
			}

			public UniversalQuantifier(Tuple<Declaration.Variable> parameters, Expr body) {
				super(EXPR_forall, parameters, body);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Expr clone(SyntacticItem[] operands) {
				return new UniversalQuantifier((Tuple<Declaration.Variable>) operands[0], (Expr) operands[1]);
			}

			@Override
			public String toString() {
				String r = "forall";
				r += getParameters();
				r += ".";
				r += getBody();
				return r;
			}
		}

		/**
		 * Represents an unbounded existentially quantified expression of the
		 * form "<code>some(T v1, ... T vn).e</code>" where <code>T1 v1</code>
		 * ... <code>Tn vn</code> are the <i>quantified variable
		 * declarations</i> and <code>e</code> is the body.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class ExistentialQuantifier extends Quantifier {
			public ExistentialQuantifier(Declaration.Variable[] parameters, Expr body) {
				super(EXPR_exists, new Tuple<>(parameters), body);
			}

			public ExistentialQuantifier(Tuple<Declaration.Variable> parameters, Expr body) {
				super(EXPR_exists, parameters, body);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Expr clone(SyntacticItem[] operands) {
				return new ExistentialQuantifier((Tuple<Declaration.Variable>) operands[0], (Expr) operands[1]);
			}

			@Override
			public String toString() {
				String r = "exists";
				r += getParameters();
				r += ".";
				r += getBody();
				return r;
			}
		}

		/**
		 * Represents a use of some variable within an expression. For example,
		 * in <code>x + 1</code> the expression <code>x</code> is a variable
		 * access expression. Every variable access is associated with a
		 * <i>variable declaration</i> that unique identifies which variable is
		 * being accessed.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class VariableAccess extends AbstractSyntacticItem implements LVal {
			public VariableAccess(Declaration.Variable decl) {
				super(EXPR_var, decl);
			}

			public Declaration.Variable getVariableDeclaration() {
				return (Declaration.Variable) getOperand(0);
			}

			@Override
			public VariableAccess clone(SyntacticItem[] operands) {
				return new VariableAccess((Declaration.Variable) operands[0]);
			}

			@Override
			public String toString() {
				return getVariableDeclaration().getName().toString();
			}
		}

		public abstract static class InfixOperator extends Operator {
			public InfixOperator(int opcode, Expr... operands) {
				super(opcode, operands);
			}

			@Override
			public String toString() {
				String str = getOperatorString();
				String r = "";
				for (int i = 0; i != size(); ++i) {
					if (i != 0) {
						r += str;
					}
					r += getOperand(i);
				}
				return r;
			}

			protected abstract String getOperatorString();
		}

		// =========================================================================
		// Logical Expressions
		// =========================================================================
		/**
		 * Represents a <i>logical conjunction</i> of the form
		 * "<code>e1 && .. && en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LogicalAnd extends InfixOperator {
			public LogicalAnd(Expr... operands) {
				super(EXPR_and, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LogicalAnd(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " && ";
			}
		}

		/**
		 * Represents a <i>logical disjunction</i> of the form
		 * "<code>e1 || .. || en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LogicalOr extends InfixOperator {
			public LogicalOr(Expr... operands) {
				super(EXPR_or, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LogicalOr(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " && ";
			}
		}

		/**
		 * Represents a <i>logical implication</i> of the form
		 * "<code>e1 ==> ... ==> en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LogicalImplication extends InfixOperator {
			public LogicalImplication(Expr... operands) {
				super(EXPR_implies, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LogicalImplication(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " ==> ";
			}
		}

		/**
		 * Represents a <i>logical biconditional</i> of the form
		 * "<code>e1 <==> ... <==> en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LogicalIff extends InfixOperator {
			public LogicalIff(Expr... operands) {
				super(EXPR_iff, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LogicalIff(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " <==> ";
			}
		}

		/**
		 * Represents a <i>logical negation</i> of the form "<code>!e</code>"
		 * where <code>e</code> is the <i>operand expression</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LogicalNot extends Operator {
			public LogicalNot(Expr operand) {
				super(EXPR_not, operand);
			}

			public Expr getOperand() {
				return getOperand(0);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LogicalNot((Expr) operands[0]);
			}
		}

		// =========================================================================
		// Comparator Expressions
		// =========================================================================

		/**
		 * Represents an equality expression of the form
		 * "<code>e1 == ... == en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Equal extends InfixOperator {
			public Equal(Expr... operands) {
				super(EXPR_eq, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Equal(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			public String getOperatorString() {
				return " == ";
			}
		}

		/**
		 * Represents an unequality expression of the form
		 * "<code>e1 != ... != en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class NotEqual extends InfixOperator {
			public NotEqual(Expr... operands) {
				super(EXPR_neq, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new NotEqual(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " != ";
			}
		}

		/**
		 * Represents a strict <i>inequality expression</i> of the form
		 * "<code>e1 < ... < en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LessThan extends InfixOperator {
			public LessThan(Expr... operands) {
				super(EXPR_lt, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LessThan(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " < ";
			}
		}

		/**
		 * Represents a non-strict <i>inequality expression</i> of the form
		 * "<code>e1 <= ... <= en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class LessThanOrEqual extends InfixOperator {
			public LessThanOrEqual(Expr... operands) {
				super(EXPR_lteq, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new LessThanOrEqual(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " <= ";
			}
		}

		/**
		 * Represents a strict <i>inequality expression</i> of the form
		 * "<code>e1 > ... > en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class GreaterThan extends InfixOperator {
			public GreaterThan(Expr... operands) {
				super(EXPR_gt, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new GreaterThan(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " > ";
			}
		}

		/**
		 * Represents a non-strict <i>inequality expression</i> of the form
		 * "<code>e1 >= ... >= en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class GreaterThanOrEqual extends InfixOperator {
			public GreaterThanOrEqual(Expr... operands) {
				super(EXPR_gteq, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new GreaterThanOrEqual(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " >= ";
			}
		}

		// =========================================================================
		// Arithmetic Expressions
		// =========================================================================

		/**
		 * Represents an arithmetic <i>addition expression</i> of the form
		 * "<code>e1 + ... + en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Addition extends InfixOperator {
			public Addition(Expr... operands) {
				super(EXPR_add, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Addition(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " + ";
			}
		}

		/**
		 * Represents an arithmetic <i>subtraction expression</i> of the form
		 * "<code>e1 - ... - en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Subtraction extends InfixOperator {
			public Subtraction(Expr... operands) {
				super(EXPR_sub, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Subtraction(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " - ";
			}
		}

		/**
		 * Represents an arithmetic <i>multiplication expression</i> of the form
		 * "<code>e1 * ... * en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Multiplication extends InfixOperator {
			public Multiplication(Expr... operands) {
				super(EXPR_mul, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Multiplication(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " * ";
			}
		}

		/**
		 * Represents an arithmetic <i>division expression</i> of the form
		 * "<code>e1 / ... / en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Division extends InfixOperator {
			public Division(Expr... operands) {
				super(EXPR_div, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Division(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " / ";
			}
		}

		/**
		 * Represents an arithmetic <i>remainder expression</i> of the form
		 * "<code>e1 / ... / en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Remainder extends InfixOperator {
			public Remainder(Expr... operands) {
				super(EXPR_rem, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Remainder(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " % ";
			}
		}

		/**
		 * Represents an arithmetic <i>negation expression</i> of the form
		 * "<code>-e</code>" where <code>e</code> is the <i>operand
		 * expression</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Negation extends Operator {
			public Negation(Expr operand) {
				super(EXPR_neg, operand);
			}

			public Expr getOperand() {
				return getOperand(0);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Negation((Expr) operands[0]);
			}

			@Override
			public String toString() {
				return "-" + getOperand();
			}
		}

		// =========================================================================
		// Bitwise Expressions
		// =========================================================================

		/**
		 * Represents a <i>bitwise shift left</i> of the form
		 * "<code>e << i</code>" where <code>e</code> is the expression being
		 * shifted and <code>i</code> the amount it is shifted by.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class BitwiseShiftLeft extends InfixOperator {
			public BitwiseShiftLeft(Expr lhs, Expr rhs) {
						super(EXPR_bitwiseshl, lhs,rhs);
					}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 2) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new BitwiseShiftLeft((Expr) operands[0], (Expr) operands[1]);
			}

			@Override
			protected String getOperatorString() {
				return " << ";
			}
		}

		/**
		 * Represents a <i>bitwise shift right</i> of the form
		 * "<code>e >> i</code>" where <code>e</code> is the expression being
		 * shifted and <code>i</code> the amount it is shifted by.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class BitwiseShiftRight extends InfixOperator {
			public BitwiseShiftRight(Expr lhs, Expr rhs) {
						super(EXPR_bitwiseshr, lhs,rhs);
					}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 2) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new BitwiseShiftRight((Expr) operands[0], (Expr) operands[1]);
			}

			@Override
			protected String getOperatorString() {
				return " >> ";
			}
		}


		/**
		 * Represents a <i>bitwise and</i> of the form
		 * "<code>e1 & .. & en</code>" where <code>e1</code> ... <code>en</code>
		 * are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class BitwiseAnd extends InfixOperator {
			public BitwiseAnd(Expr... operands) {
						super(EXPR_bitwiseand, operands);
					}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new BitwiseAnd(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " & ";
			}
		}

		/**
		 * Represents a <i>bitwise or</i> of the form
		 * "<code>e1 | .. | en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class BitwiseOr extends InfixOperator {
			public BitwiseOr(Expr... operands) {
				super(EXPR_bitwiseor, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new BitwiseOr(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " & ";
			}
		}

		/**
		 * Represents a <i>bitwise xor</i> of the form
		 * "<code>e1 ^ .. ^ en</code>" where <code>e1</code> ...
		 * <code>en</code> are the <i>operand expressions</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class BitwiseXor extends InfixOperator {
			public BitwiseXor(Expr... operands) {
				super(EXPR_bitwiseor, operands);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length <= 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new BitwiseXor(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			protected String getOperatorString() {
				return " ^ ";
			}
		}

		/**
		 * Represents a <i>bitwise complement</i> of the form "<code>~e</code>"
		 * where <code>e</code> is the <i>operand expression</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class BitwiseComplement extends Operator {
			public BitwiseComplement(Expr operand) {
				super(EXPR_bitwisenot, operand);
			}

			public Expr getOperand() {
				return getOperand(0);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new BitwiseComplement((Expr) operands[0]);
			}
		}

		// =========================================================================
		// Reference Expressions
		// =========================================================================
		public static class Dereference extends Operator implements LVal {
			public Dereference(Expr operand) {
				super(EXPR_deref, operand);
			}

			public Expr getOperand() {
				return getOperand(0);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 1) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new Dereference((Expr) operands[0]);
			}

			@Override
			public String toString() {
				return "*" + getOperand();
			}
		}

		public static class New extends AbstractSyntacticItem implements LVal {
			public New(Expr operand, Identifier lifetime) {
				super(EXPR_new, operand, lifetime);
			}

			public Expr getOperand() {
				return (Expr) super.getOperand(0);
			}

			public Identifier getLifetime() {
				return (Identifier) super.getOperand(1);
			}

			@Override
			public Expr clone(SyntacticItem[] operands) {
				if (operands.length != 2) {
					throw new IllegalArgumentException("invalid number of operands");
				}
				return new New((Expr) operands[0], (Identifier) operands[1]);
			}

			@Override
			public String toString() {
				return "new " + getOperand();
			}
		}

		public static class LambdaConstant extends AbstractSyntacticItem implements Expr {

			public LambdaConstant(Name name, Tuple<Type> parameters) {
				super(EXPR_lambdaconst, name, parameters);
			}

			public Name getName() {
				return (Name) getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Type> getParameterTypes() {
				return (Tuple<Type>) getOperand(1);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new LambdaConstant((Name) operands[0], (Tuple<Type>) operands[1]);
			}
		}

		public static class LambdaInitialiser extends AbstractSyntacticItem implements Expr {

			public LambdaInitialiser(Tuple<Declaration.Variable> parameters, Tuple<Identifier> captures, Tuple<Identifier> lifetimes, Expr body) {
				super(EXPR_lambdaconst, parameters, captures, lifetimes, body);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Declaration.Variable> getParameterTypes() {
				return (Tuple<Declaration.Variable>) getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getCaptures() {
				return (Tuple<Identifier>) getOperand(1);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getLifetimes() {
				return (Tuple<Identifier>) getOperand(2);
			}

			public Expr getBody() {
				return (Expr) getOperand(3);
			}

			@SuppressWarnings("unchecked")
			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new LambdaInitialiser((Tuple<Declaration.Variable>) operands[0], (Tuple<Identifier>) operands[1],
						(Tuple<Identifier>) operands[2], (Expr) operands[3]);
			}
		}

		// =========================================================================
		// Array Expressions
		// =========================================================================

		/**
		 * Represents an <i>array access expression</i> of the form
		 * "<code>arr[e]</code>" where <code>arr</code> is the <i>source
		 * array</i> and <code>e</code> the <i>subscript expression</i>. This
		 * returns the value held in the element determined by <code>e</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class ArrayAccess extends Expr.Operator implements LVal {
			public ArrayAccess(Expr src, Expr index) {
				super(EXPR_arridx, src, index);
			}

			public Expr getSource() {
				return (Expr) getOperand(0);
			}

			public Expr getSubscript() {
				return (Expr) getOperand(1);
			}

			@Override
			public ArrayAccess clone(SyntacticItem[] operands) {
				return new ArrayAccess((Expr) operands[0], (Expr) operands[1]);
			}
		}

		/**
		 * Represents an <i>array update expression</i> of the form
		 * "<code>arr[e1:=e2]</code>" where <code>arr</code> is the <i>source
		 * array</i>, <code>e1</code> the <i>subscript expression</i> and
		 * <code>e2</code> is the value expression. This returns a new array
		 * which is equivalent to <code>arr</code> but where the element
		 * determined by <code>e1</code> has the value resulting from
		 * <code>e2</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class ArrayUpdate extends Expr.Operator {
			public ArrayUpdate(Expr src, Expr index, Expr value) {
				super(EXPR_arrupdt, src, index, value);
			}

			public Expr getSource() {
				return (Expr) getOperand(0);
			}

			public Expr getSubscript() {
				return (Expr) getOperand(1);
			}

			public Expr getValue() {
				return (Expr) getOperand(2);
			}

			@Override
			public ArrayUpdate clone(SyntacticItem[] operands) {
				return new ArrayUpdate((Expr) operands[0], (Expr) operands[1], (Expr) operands[2]);
			}

			@Override
			public String toString() {
				return getSource() + "[" + getSubscript() + ":=" + getValue() + "]";
			}
		}

		/**
		 * Represents an <i>array initialiser expression</i> of the form
		 * "<code>[e1,...,en]</code>" where <code>e1</code> ... <code>en</code>
		 * are the <i>initialiser expressions</i>. Thus returns a new array made
		 * up from those values resulting from the initialiser expressions.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class ArrayInitialiser extends Expr.Operator {
			public ArrayInitialiser(Expr... elements) {
				super(EXPR_arrinit, elements);
			}

			@Override
			public ArrayInitialiser clone(SyntacticItem[] operands) {
				return new ArrayInitialiser(ArrayUtils.toArray(Expr.class, operands));
			}

			@Override
			public String toString() {
				return Arrays.toString(getOperands());
			}
		}

		/**
		 * Represents an <i>array generator expression</i> of the form
		 * "<code>[e1;e2]</code>" where <code>e1</code> is the <i>element
		 * expression</i> and <code>e2</code> is the <i>length expression</i>.
		 * This returns a new array whose length is determined by
		 * <code>e2</code> and where every element has contains the value
		 * determined by <code>e1</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class ArrayGenerator extends Expr.Operator {
			public ArrayGenerator(Expr value, Expr length) {
				super(EXPR_arrgen, value, length);
			}

			public Expr getValue() {
				return (Expr) getOperand(0);
			}

			public Expr getLength() {
				return (Expr) getOperand(1);
			}

			@Override
			public ArrayGenerator clone(SyntacticItem[] operands) {
				return new ArrayGenerator((Expr) operands[0], (Expr) operands[1]);
			}
		}

		/**
		 * Represents an <i>array length expression</i> of the form
		 * "<code>|arr|</code>" where <code>arr</code> is the <i>source
		 * array</i>. This simply returns the length of array <code>arr</code>.
		 * <code>e</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class ArrayLength extends Expr.Operator {
			public ArrayLength(Expr src) {
				super(EXPR_arrlen, src);
			}

			public Expr getSource() {
				return (Expr) getOperand(0);
			}

			@Override
			public ArrayLength clone(SyntacticItem[] operands) {
				return new ArrayLength((Expr) operands[0]);
			}

			@Override
			public String toString() {
				return "|" + getSource() + "|";
			}
		}

		// =========================================================================
		// Record Expressions
		// =========================================================================

		/**
		 * Represents a <i>record access expression</i> of the form
		 * "<code>rec.f</code>" where <code>rec</code> is the <i>source
		 * record</i> and <code>f</code> is the <i>field</i>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class RecordAccess extends AbstractSyntacticItem implements LVal {
			public RecordAccess(Expr lhs, Identifier rhs) {
				super(EXPR_recfield, lhs, rhs);
			}

			public Expr getSource() {
				return (Expr) getOperand(0);
			}

			public Identifier getField() {
				return (Identifier) getOperand(1);
			}

			@Override
			public RecordAccess clone(SyntacticItem[] operands) {
				return new RecordAccess((Expr) operands[0], (Identifier) operands[1]);
			}

			@Override
			public String toString() {
				return getSource() + "." + getField();
			}
		}

		/**
		 * Represents a <i>record initialiser</i> expression of the form
		 * <code>{ f1: e1, ..., fn: en }</code> where <code>f1: e1</code> ...
		 * <code>fn: en</code> are <i>field initialisers</code>. This returns a
		 * new record where each field holds the value resulting from its
		 * corresponding expression.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class RecordInitialiser extends AbstractSyntacticItem implements Expr {
			@SafeVarargs
			public RecordInitialiser(Pair<Identifier, Expr>... fields) {
				super(EXPR_recinit, fields);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Pair<Identifier, Expr> getOperand(int i) {
				return (Pair<Identifier,Expr>) super.getOperand(i);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Pair<Identifier, Expr>[] getOperands() {
				return ArrayUtils.toArray(Pair.class, super.getOperands());
			}

			@SuppressWarnings("unchecked")
			@Override
			public RecordInitialiser clone(SyntacticItem[] operands) {
				return new RecordInitialiser(ArrayUtils.toArray(Pair.class, operands));
			}
		}

		/**
		 * Represents a <i>record update expression</i> of the form
		 * "<code>rec[f:=e]</code>" where <code>rec</code> is the <i>source
		 * record</i>, <code>f</code> is the <i>field</i> and <code>e</code> is
		 * the <i>value expression</i>. This returns a new record which is
		 * equivalent to <code>rec</code> but where the element in field
		 * <code>f</code> has the value resulting from <code>e</code>.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class RecordUpdate extends AbstractSyntacticItem implements Expr {
			public RecordUpdate(Expr lhs, Identifier mhs, Expr rhs) {
				super(EXPR_recupdt, lhs, mhs, rhs);
			}

			public Expr getSource() {
				return (Expr) getOperand(0);
			}

			public Identifier getField() {
				return (Identifier) getOperand(1);
			}

			public Expr getValue() {
				return (Expr) getOperand(2);
			}

			@Override
			public RecordUpdate clone(SyntacticItem[] operands) {
				return new RecordUpdate((Expr) operands[0], (Identifier) operands[1], (Expr) operands[2]);
			}

			@Override
			public String toString() {
				return getSource() + "{" + getField() + ":=" + getValue() + "}";
			}
		}
	}

	// =========================================================================
	// Types
	// =========================================================================

	public interface Type extends SyntacticItem {

		public static final Any Any = new Any();
		public static final Void Void = new Void();
		public static final Bool Bool = new Bool();
		public static final Byte Byte = new Byte();
		public static final Int Int = new Int();
		public static final Null Null = new Null();

		public interface Primitive extends Type {

		}

		public static abstract class Atom extends AbstractSyntacticItem implements Type {
			public Atom(int opcode) {
				super(opcode);
			}

			public Atom(int opcode, SyntacticItem item) {
				super(opcode, item);
			}

			public Atom(int opcode, SyntacticItem first, SyntacticItem second) {
				super(opcode, first, second);
			}

			public Atom(int opcode, SyntacticItem[] items) {
				super(opcode, items);
			}
		}

		/**
		 * The type <code>any</code> represents the type whose variables may
		 * hold any possible value. <b>NOTE:</b> the any type is top in the type
		 * lattice.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Any extends Atom implements Primitive {
			public Any() {
				super(TYPE_any);
			}

			@Override
			public Any clone(SyntacticItem[] operands) {
				return new Any();
			}

			@Override
			public String toString() {
				return "any";
			}
		}

		/**
		 * A void type represents the type whose variables cannot exist! That
		 * is, they cannot hold any possible value. Void is used to represent
		 * the return type of a function which does not return anything.
		 * However, it is also used to represent the element type of an empty
		 * list of set. <b>NOTE:</b> the void type is a subtype of everything;
		 * that is, it is bottom in the type lattice.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Void extends Atom implements Primitive {
			public Void() {
				super(TYPE_void);
			}

			@Override
			public Void clone(SyntacticItem[] operands) {
				return new Void();
			}

			@Override
			public String toString() {
				return "void";
			}
		}

		/**
		 * The null type is a special type which should be used to show the
		 * absence of something. It is distinct from void, since variables can
		 * hold the special <code>null</code> value (where as there is no
		 * special "void" value). With all of the problems surrounding
		 * <code>null</code> and <code>NullPointerException</code>s in languages
		 * like Java and C, it may seem that this type should be avoided.
		 * However, it remains a very useful abstraction to have around and, in
		 * Whiley, it is treated in a completely safe manner (unlike e.g. Java).
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Null extends Atom implements Primitive {
			public Null() {
				super(TYPE_null);
			}

			@Override
			public Null clone(SyntacticItem[] operands) {
				return new Null();
			}

			@Override
			public String toString() {
				return "null";
			}
		}

		/**
		 * Represents the set of boolean values (i.e. true and false)
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Bool extends Atom implements Primitive {
			public Bool() {
				super(TYPE_bool);
			}

			@Override
			public Bool clone(SyntacticItem[] operands) {
				return new Bool();
			}

			@Override
			public String toString() {
				return "bool";
			}
		}

		/**
		 * Represents a sequence of 8 bits. Note that, unlike many languages,
		 * there is no representation associated with a byte. For example, to
		 * extract an integer value from a byte, it must be explicitly decoded
		 * according to some representation (e.g. two's compliment) using an
		 * auxillary function (e.g. <code>Byte.toInt()</code>).
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Byte extends Atom implements Primitive {
			public Byte() {
				super(TYPE_byte);
			}

			@Override
			public Null clone(SyntacticItem[] operands) {
				return new Null();
			}

			@Override
			public String toString() {
				return "byte";
			}
		}

		/**
		 * Represents the set of (unbound) integer values. Since integer types
		 * in Whiley are unbounded, there is no equivalent to Java's
		 * <code>MIN_VALUE</code> and <code>MAX_VALUE</code> for
		 * <code>int</code> types.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Int extends Atom implements Primitive {
			public Int() {
				super(TYPE_int);
			}

			@Override
			public Int clone(SyntacticItem[] operands) {
				return new Int();
			}

			@Override
			public String toString() {
				return "int";
			}
		}

		/**
		 * Represents an array type, which is of the form:
		 *
		 * <pre>
		 * ArrayType ::= Type '[' ']'
		 * </pre>
		 *
		 * An array type describes array values whose elements are subtypes of
		 * the element type. For example, <code>[1,2,3]</code> is an instance of
		 * array type <code>int[]</code>; however, <code>[false]</code> is not.
		 *
		 * @return
		 */
		public static class Array extends Atom {
			public Array(Type element) {
				super(TYPE_arr, element);
			}

			public Type getElement() {
				return (Type) getOperand(0);
			}

			@Override
			public Array clone(SyntacticItem[] operands) {
				return new Array((Type) operands[0]);
			}

			@Override
			public String toString() {
				return "(" + getElement() + ")[]";
			}
		}

		/**
		 * Parse a reference type, which is of the form:
		 *
		 * <pre>
		 * ReferenceType ::= '&' Type
		 * </pre>
		 *
		 * Represents a reference to an object in Whiley. For example,
		 * <code>&this:int</code> is the type of a reference to a location
		 * allocated in the enclosing scope which holds an integer value.
		 *
		 * @return
		 */
		public static class Reference extends Atom {
			public Reference(Type element, Identifier lifetime) {
				super(TYPE_ref, element, lifetime);
			}

			public Type getElement() {
				return (Type) getOperand(0);
			}

			public Identifier getLifetime() {
				return (Identifier) getOperand(1);
			}

			@Override
			public Reference clone(SyntacticItem[] operands) {
				return new Reference((Type) operands[0], (Identifier) operands[1]);
			}

			@Override
			public String toString() {
				Identifier lifetime = getLifetime();
				if (lifetime != null) {
					return "&(" + getElement() + ")";
				} else {
					return "&" + lifetime + ":(" + getElement() + ")";
				}
			}
		}

		/**
		 * Represents record type, which is of the form:
		 *
		 * <pre>
		 * RecordType ::= '{' Type Identifier (',' Type Identifier)* [ ',' "..." ] '}'
		 * </pre>
		 *
		 * A record is made up of a number of fields, each of which has a unique
		 * name. Each field has a corresponding type. One can think of a record
		 * as a special kind of "fixed" map (i.e. where we know exactly which
		 * entries we have).
		 *
		 * @return
		 */
		public static class Record extends Atom {
			public Record(boolean isOpen, Tuple<Declaration.Variable> fields) {
				this(new Value.Bool(isOpen), fields);
			}

			public Record(Value.Bool isOpen, Tuple<Declaration.Variable> fields) {
				super(TYPE_rec, isOpen, fields);
			}

			private Record(SyntacticItem[] operands) {
				super(TYPE_rec, operands);
			}

			public boolean isOpen() {
				Value.Bool flag = (Value.Bool) getOperand(0);
				return flag.get();
			}

			@SuppressWarnings("unchecked")
			public Tuple<Declaration.Variable> getFields() {
				return (Tuple<Declaration.Variable>) getOperand(1);
			}

			@Override
			public Record clone(SyntacticItem[] operands) {
				return new Record(operands);
			}

			@Override
			public String toString() {
				String r = "{";
				Tuple<Declaration.Variable> fields = getFields();
				for (int i = 0; i != fields.size(); ++i) {
					if (i != 0) {
						r += ",";
					}
					Declaration.Variable field = fields.getOperand(i);
					r += field.getType() + " " + field.getName();
				}
				if (isOpen()) {
					if (fields.size() > 0) {
						r += ", ...";
					} else {
						r += "...";
					}
				}
				return r + "}";
			}
		}

		/**
		 * Represents a nominal type, which is of the form:
		 *
		 * <pre>
		 * NominalType ::= Identifier ('.' Identifier)*
		 * </pre>
		 *
		 * A nominal type specifies the name of a type defined elsewhere. In
		 * some cases, this type can be expanded (or "inlined"). However,
		 * visibility modifiers can prevent this and, thus, give rise to true
		 * nominal types.
		 *
		 * @return
		 */
		public static class Nominal extends AbstractSyntacticItem implements Type {
			public Nominal(Name name) {
				super(TYPE_nom, name);
			}

			public Name getName() {
				return (Name) getOperand(0);
			}

			@Override
			public Nominal clone(SyntacticItem[] operands) {
				return new Nominal((Name) operands[0]);
			}

			@Override
			public String toString() {
				return getName().toString();
			}
		}

		/**
		 * Parse a negation type, which is of the form:
		 *
		 * <pre>
		 * ReferenceType ::= '!' Type
		 * </pre>
		 *
		 * Represents the set of types which are not in a given type. For
		 * example, <code>!int</code> is the set of all values which are not
		 * integers. Thus, for example, the type <code>bool</code> is a subtype
		 * of <code>!int</code> .
		 *
		 * @return
		 */
		public static class Negation extends AbstractSyntacticItem implements Type {
			public Negation(Type element) {
				super(TYPE_not, element);
			}

			public Type getElement() {
				return (Type) getOperand(0);
			}

			@Override
			public Negation clone(SyntacticItem[] operands) {
				return new Negation((Type) operands[0]);
			}

			@Override
			public String toString() {
				return "!(" + getElement() + ")";
			}
		}

		public abstract static class UnionOrIntersection extends AbstractSyntacticItem implements Type {
			public UnionOrIntersection(int kind, Type[] types) {
				super(kind, types);
			}

			@Override
			public Type getOperand(int i) {
				return (Type) super.getOperand(i);
			}

			@Override
			public Type[] getOperands() {
				return ArrayUtils.toArray(Type.class, super.getOperands());
			}
		}

		/**
		 * Represents a union type, which is of the form:
		 *
		 * <pre>
		 * UnionType ::= IntersectionType ('|' IntersectionType)*
		 * </pre>
		 *
		 * Union types are used to compose types together. For example, the type
		 * <code>int|null</code> represents the type which is either an
		 * <code>int</code> or <code>null</code>.
		 *
		 * Represents the union of one or more types together. For example, the
		 * union of <code>int</code> and <code>null</code> is
		 * <code>int|null</code>. Any variable of this type may hold any integer
		 * or the null value. Furthermore, the types <code>int</code> and
		 * <code>null</code> are collectively referred to as the "bounds" of
		 * this type.
		 *
		 * @return
		 */
		public static class Union extends UnionOrIntersection {
			public Union(Type[] types) {
				super(TYPE_or, types);
			}

			@Override
			public Union clone(SyntacticItem[] operands) {
				return new Union(ArrayUtils.toArray(Type.class, operands));
			}

			@Override
			public String toString() {
				String r = "";
				for (int i = 0; i != size(); ++i) {
					if (i != 0) {
						r += "|";
					}
					r += getOperand(i);
				}
				return "(" + r + ")";
			}
		}

		/**
		 * Represents an intersection type, which is of the form:
		 *
		 * <pre>
		 * IntersectionType ::= BaseType ('&' BaseType)*
		 * </pre>
		 *
		 * Intersection types are used to unify types together. For example, the
		 * type <code>{int x, int y}&MyType</code> represents the type which is
		 * both an instanceof of <code>{int x, int y}</code> and an instance of
		 * <code>MyType</code>.
		 *
		 * Represents the intersection of one or more types together. For
		 * example, the intersection of <code>T1</code> and <code>T2</code> is
		 * <code>T1&T2</code>. Furthermore, any variable of this type must be
		 * both an instanceof <code>T1</code> and an instanceof <code>T2</code>.
		 *
		 * @return
		 */
		public static class Intersection extends UnionOrIntersection {
			public Intersection(Type[] types) {
				super(TYPE_and, types);
			}

			@Override
			public Intersection clone(SyntacticItem[] operands) {
				return new Intersection(ArrayUtils.toArray(Type.class, operands));
			}

			@Override
			public String toString() {
				String r = "";
				for (int i = 0; i != size(); ++i) {
					if (i != 0) {
						r += "&";
					}
					r += getOperand(i);
				}
				return "(" + r + ")";
			}
		}

		/**
		 * Represents the set of all functions, methods and properties. These
		 * are values which can be called using an indirect invoke expression.
		 * Each function or method accepts zero or more parameters and will
		 * produce zero or more returns.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static abstract class Callable extends Atom implements Type {
			public Callable(int opcode, Tuple<Type> parameters, Tuple<Type> returns) {
				super(opcode, parameters, returns);
			}

			public Callable(int opcode, SyntacticItem... operands) {
				super(opcode, operands);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Type> getParameters() {
				return (Tuple<Type>) getOperand(0);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Type> getReturns() {
				return (Tuple<Type>) getOperand(1);
			}

			@Override
			public String toString() {
				return getParameters().toString() + "->" + getReturns();
			}
		}

		/**
		 * Represents the set of all function values. These are pure functions,
		 * sometimes also called "mathematical" functions. A function cannot
		 * have any side-effects and must always return the same values given
		 * the same inputs. A function cannot have zero returns, since this
		 * would make it a no-operation.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Function extends Callable implements Type {
			public Function(Tuple<Type> parameters, Tuple<Type> returns) {
				super(TYPE_fun, parameters, returns);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Function clone(SyntacticItem[] operands) {
				return new Function((Tuple<Type>) operands[0], (Tuple<Type>) operands[1]);
			}

			@Override
			public String toString() {
				return "function" + super.toString();
			}
		}

		/**
		 * Represents the set of all method values. These are impure and may
		 * have side-effects (e.g. performing I/O, updating non-local state,
		 * etc). A method may have zero returns and, in such case, the effect of
		 * a method comes through other side-effects. Methods may also have
		 * captured lifetime arguments, and may themselves declare lifetime
		 * arguments.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Method extends Callable implements Type {

			public Method(Tuple<Type> parameters, Tuple<Type> returns, Tuple<Identifier> captures,
					Tuple<Identifier> lifetimes) {
				super(TYPE_meth, parameters, returns, captures, lifetimes);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Method clone(SyntacticItem[] operands) {
				return new Method((Tuple<Type>) operands[0], (Tuple<Type>) operands[1], (Tuple<Identifier>) operands[2],
						(Tuple<Identifier>) operands[3]);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getCapturedLifetimes() {
				return (Tuple<Identifier>) getOperand(2);
			}

			@SuppressWarnings("unchecked")
			public Tuple<Identifier> getLifetimeParameters() {
				return (Tuple<Identifier>) getOperand(3);
			}

			@Override
			public String toString() {
				return "method" + super.toString();
			}
		}

		/**
		 * Represents the set of all proeprty values. These are pure predicates,
		 * sometimes also called "mathematical" functions. A property cannot
		 * have any side-effects and always returns the boolean true.
		 *
		 * @author David J. Pearce
		 *
		 */
		public static class Property extends Callable implements Type {
			public Property(Tuple<Type> parameters) {
				super(TYPE_macro, parameters, new Tuple<>(new Type.Bool()));
			}

			private Property(Tuple<Type> parameters, Tuple<Type> returns) {
				super(TYPE_macro, parameters, returns);
			}

			@SuppressWarnings("unchecked")
			@Override
			public Property clone(SyntacticItem[] operands) {
				return new Property((Tuple<Type>) operands[0], (Tuple<Type>) operands[1]);
			}

			@Override
			public String toString() {
				return "macro" + super.toString();
			}
		}
	}

	/**
	 * <p>
	 * Represents a protection modifier on a module item. For example, all
	 * declarations (e.g. functions, types, etc) can be marked as
	 * <code>public</code> or <code>private</code>.
	 * </p>
	 * <p>
	 * The modifiers <code>native</code> and <code>export</code> are used to
	 * enable inter-operation with other languages. By declaring a function or
	 * method as <code>native</code> you are signaling that its implementation
	 * is provided elsewhere (e.g. it's implemented in Java code directly). By
	 * marking a function or method with <code>export</code>, you are declaring
	 * that external code may call it. For example, you have some Java code that
	 * needs to call it. The modifier is required because, by default, all the
	 * names of all methods and functions are <i>mangled</i> to include type
	 * information and enable overloading. Therefore, a method/function marked
	 * with <code>export</code> will generate a function without name mangling.
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Modifier extends SyntacticItem {

		public static final class Public extends AbstractSyntacticItem implements Modifier {
			public Public() {
				super(MOD_public);
			}

			@Override
			public String toString() {
				return "public";
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Public();
			}
		}

		public static final class Private extends AbstractSyntacticItem implements Modifier {
			public Private() {
				super(MOD_private);
			}

			@Override
			public String toString() {
				return "private";
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Private();
			}
		}

		public static final class Native extends AbstractSyntacticItem implements Modifier {
			public Native() {
				super(MOD_native);
			}

			@Override
			public String toString() {
				return "native";
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Native();
			}
		}

		public static final class Export extends AbstractSyntacticItem implements Modifier {
			public Export() {
				super(MOD_export);
			}

			@Override
			public String toString() {
				return "export";
			}

			@Override
			public SyntacticItem clone(SyntacticItem[] operands) {
				return new Export();
			}
		}

	}
}
