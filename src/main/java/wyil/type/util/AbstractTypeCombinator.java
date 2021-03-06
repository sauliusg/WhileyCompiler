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
package wyil.type.util;

import static wyc.lang.WhileyFile.TYPE_array;
import static wyc.lang.WhileyFile.TYPE_bool;
import static wyc.lang.WhileyFile.TYPE_byte;
import static wyc.lang.WhileyFile.TYPE_function;
import static wyc.lang.WhileyFile.TYPE_int;
import static wyc.lang.WhileyFile.TYPE_method;
import static wyc.lang.WhileyFile.TYPE_nominal;
import static wyc.lang.WhileyFile.TYPE_null;
import static wyc.lang.WhileyFile.TYPE_record;
import static wyc.lang.WhileyFile.TYPE_reference;
import static wyc.lang.WhileyFile.TYPE_staticreference;
import static wyc.lang.WhileyFile.TYPE_union;
import static wyc.util.ErrorMessages.errorMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import wyc.lang.WhileyFile.Type;
import wybs.lang.CompilationUnit;
import wybs.lang.NameResolver;
import wybs.lang.SyntacticItem;
import wybs.lang.SyntaxError;
import wybs.util.AbstractCompilationUnit.Ref;
import wybs.lang.NameResolver.ResolutionError;
import wyc.lang.WhileyFile.Decl;
import wyc.lang.WhileyFile.SemanticType;
import wyc.util.ErrorMessages;
import wycc.util.ArrayUtils;
import wyil.type.subtyping.EmptinessTest.LifetimeRelation;
import wyil.type.subtyping.SubtypeOperator;

public abstract class AbstractTypeCombinator {
	protected final NameResolver resolver;
	protected final SubtypeOperator subtyping;

	public AbstractTypeCombinator(NameResolver resolver, SubtypeOperator subtyping) {
		this.resolver = resolver;
		this.subtyping = subtyping;
	}

	protected Type apply(Type lhs, Type rhs, LifetimeRelation lifetimes) {
		return apply(lhs, rhs, lifetimes, new LinkageStack());
	}
	protected Type apply(Type lhs, Type rhs, LifetimeRelation lifetimes, LinkageStack stack) {
		// FIXME: this is obviously broken as it can infinite loop on recursive types!
		int lhsKind = normalise(lhs);
		int rhsKind = normalise(rhs);
		if (lhsKind == rhsKind) {
			// Easy case.
			switch (lhsKind) {
			case TYPE_null:
				return apply((Type.Null) lhs, (Type.Null) rhs, lifetimes, stack);
			case TYPE_bool:
				return apply((Type.Bool) lhs, (Type.Bool) rhs, lifetimes, stack);
			case TYPE_byte:
				return apply((Type.Byte) lhs, (Type.Byte) rhs, lifetimes, stack);
			case TYPE_int:
				return apply((Type.Int) lhs, (Type.Int) rhs, lifetimes, stack);
			case TYPE_array:
				return apply((Type.Array) lhs, (Type.Array) rhs, lifetimes, stack);
			case TYPE_reference:
				return apply((Type.Reference) lhs, (Type.Reference) rhs, lifetimes, stack);
			case TYPE_record:
				return apply((Type.Record) lhs, (Type.Record) rhs, lifetimes, stack);
			case TYPE_union:
				return apply((Type.Union) lhs, rhs, lifetimes, stack);
			case TYPE_function:
				return apply((Type.Function) lhs, (Type.Function) rhs, lifetimes, stack);
			case TYPE_method:
				return apply((Type.Method) lhs, (Type.Method) rhs, lifetimes, stack);
			case TYPE_nominal:
				return apply((Type.Nominal) lhs, (Type.Nominal) rhs, lifetimes, stack);
			default:
				throw new IllegalArgumentException("invalid type encountered: " + lhs);
			}
		} else if (lhs instanceof Type.Union) {
			return apply((Type.Union) lhs, rhs, lifetimes, stack);
		} else if (lhs instanceof Type.Nominal) {
			return apply((Type.Nominal) lhs, rhs, lifetimes, stack);
		} else if (rhs instanceof Type.Nominal) {
			return apply(lhs, (Type.Nominal) rhs, lifetimes, stack);
		} else if (rhs instanceof Type.Union) {
			return apply(lhs, (Type.Union) rhs, lifetimes, stack);
		} else {
			// Failed to combine them
			return null;
		}
	}

	protected abstract Type apply(Type.Null lhs, Type.Null rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Bool lhs, Type.Bool rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Byte lhs, Type.Byte rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Int lhs, Type.Int rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Array lhs, Type.Array rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Reference lhs, Type.Reference rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Record lhs, Type.Record rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Function lhs, Type.Function rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected abstract Type apply(Type.Method lhs, Type.Method rhs, LifetimeRelation lifetimes, LinkageStack stack);

	protected Type apply(Type.Union lhs, Type rhs, LifetimeRelation lifetimes, LinkageStack stack) {
		Type[] types = new Type[lhs.size()];
		for (int i = 0; i != lhs.size(); ++i) {
			types[i] = apply(lhs.get(i), rhs, lifetimes, stack);
		}
		return union(types);
	}

	protected Type apply(Type lhs, Type.Union rhs, LifetimeRelation lifetimes, LinkageStack stack) {
		Type[] types = new Type[rhs.size()];
		for (int i = 0; i != types.length; ++i) {
			types[i] = apply(lhs, rhs.get(i), lifetimes, stack);
		}
		return union(types);
	}

	protected Type apply(Type.Nominal lhs, Type.Nominal rhs, LifetimeRelation lifetimes, LinkageStack stack) {
		// Check whether pairing seen before.
		Linkage linkage = stack.find(lhs, rhs);
		if (linkage != null) {
			// Yes, seen before. Therefore, create recursive type to represent this.
			Type.Recursive r = new Type.Recursive(new Ref<Type>(new Type.Unresolved()));
			linkage.links.add(r);
			return r;
		} else {
			// Not see before, so record and continue.
			stack.push(lhs, rhs);
			try {
				// Expand the lhs and continue
				Decl.Type decl = resolver.resolveExactly(lhs.getName(), Decl.Type.class);
				Type t = apply(decl.getVariableDeclaration().getType(), rhs, lifetimes, stack);
				stack.popAndLink(t);
				return t;
			} catch (ResolutionError e) {
				return syntaxError(errorMessage(ErrorMessages.RESOLUTION_ERROR, lhs.getName().toString()), lhs);
			}
		}
	}

	protected Type apply(Type.Nominal lhs, Type rhs, LifetimeRelation lifetimes, LinkageStack stack) {
		try {
			Decl.Type decl = resolver.resolveExactly(lhs.getName(), Decl.Type.class);
			return apply(decl.getVariableDeclaration().getType(), rhs, lifetimes, stack);
		} catch (ResolutionError e) {
			return syntaxError(errorMessage(ErrorMessages.RESOLUTION_ERROR, lhs.getName().toString()), lhs);
		}
	}

	protected Type apply(Type lhs, Type.Nominal rhs, LifetimeRelation lifetimes, LinkageStack stack) {
		try {
			Decl.Type decl = resolver.resolveExactly(rhs.getName(), Decl.Type.class);
			return apply(lhs, decl.getVariableDeclaration().getType(), lifetimes, stack);
		} catch (ResolutionError e) {
			return syntaxError(errorMessage(ErrorMessages.RESOLUTION_ERROR, rhs.getName().toString()), lhs);
		}
	}

	// ===================================================================================
	// Helpers
	// ===================================================================================

	protected final static class LinkageStack {
		private final ArrayList<Linkage> linkages;

		public LinkageStack() {
			this.linkages = new ArrayList<>();
		}

		public Linkage find(Type.Nominal lhs, Type.Nominal rhs) {
			for (int i = 0; i != linkages.size(); ++i) {
				Linkage l = linkages.get(i);
				if(l.lhs.equals(lhs) && l.rhs.equals(rhs)) {
					return l;
				}
			}
			return null;
		}

		public void push(Type.Nominal lhs, Type.Nominal rhs) {
			this.linkages.add(new Linkage(lhs,rhs));
		}

		public void popAndLink(Type head) {
			int last = linkages.size() - 1;
			// Pop item off stack
			Linkage l = linkages.get(last);
			linkages.remove(last);
			// Link all links
			for (int i = 0; i != l.links.size(); ++i) {
				l.links.get(i).setHead(new Ref<>(head));
			}
		}
	}

	protected final static class Linkage {
		public final Type.Nominal lhs;
		public final Type.Nominal rhs;
		public final ArrayList<Type.Recursive> links;

		public Linkage(Type.Nominal lhs, Type.Nominal rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			this.links = new ArrayList<>();
		}
	}

	protected static int normalise(SemanticType type) {
		int opcode = type.getOpcode();
		switch (opcode) {
		case TYPE_reference:
		case TYPE_staticreference:
			return TYPE_reference;
		default:
			return opcode;
		}
	}

	protected Type union(Type... types) {
		types = ArrayUtils.removeAll(types, Type.Void);
		switch (types.length) {
		case 0:
			return Type.Void;
		case 1:
			return types[0];
		default:
			return new Type.Union(types);
		}
	}

	protected <T> T syntaxError(String msg, SyntacticItem e) {
		// FIXME: this is a kludge
		CompilationUnit cu = (CompilationUnit) e.getHeap();
		throw new SyntaxError(msg, cu.getEntry(), e);
	}
}
