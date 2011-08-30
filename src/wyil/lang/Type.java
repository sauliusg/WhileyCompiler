// This file is part of the Whiley-to-Java Compiler (wyjc).
//
// The Whiley-to-Java Compiler is free software; you can redistribute 
// it and/or modify it under the terms of the GNU General Public 
// License as published by the Free Software Foundation; either 
// version 3 of the License, or (at your option) any later version.
//
// The Whiley-to-Java Compiler is distributed in the hope that it 
// will be useful, but WITHOUT ANY WARRANTY; without even the 
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
// PURPOSE.  See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Whiley-to-Java Compiler. If not, see 
// <http://www.gnu.org/licenses/>
//
// Copyright 2010, David James Pearce. 

package wyil.lang;

import java.util.*;

import static wyts.lang.type.Node.*;
import wyil.util.Pair;
import wyts.lang.type.*;

/**
 * A structural type. See
 * http://whiley.org/2011/03/07/implementing-structural-types/ for more on how
 * this class works.
 * 
 * @author djp
 * 
 */
public abstract class Type {
	
	// =============================================================
	// Type Constructors
	// =============================================================

	public static final Any T_ANY = new Any();
	public static final Void T_VOID = new Void();
	public static final Null T_NULL = new Null();	
	public static final Bool T_BOOL = new Bool();
	public static final Byte T_BYTE = new Byte();
	public static final Char T_CHAR = new Char();
	public static final Int T_INT = new Int();		
	public static final Real T_REAL = new Real();
	public static final Strung T_STRING = new Strung();	
	public static final Meta T_META = new Meta();
	
	/**
	 * Construct a tuple type using the given element types.
	 * 
	 * @param element
	 */
	public static final Tuple T_TUPLE(Type... elements) {
		int len = 1;
		for(Type b : elements) {
			// could be optimised slightly
			len += nodes(b).length;
		}		
		Node[] nodes = new Node[len];
		int[] children = new int[elements.length];
		int start = 1;
		for(int i=0;i!=elements.length;++i) {
			children[i] = start;
			Node[] comps = nodes(elements[i]);
			Node.insertNodes(start,comps,nodes);
			start += comps.length;
		}
		nodes[0] = new Node(K_TUPLE, children);		
		return new Tuple(nodes);
	}
	
	/**
	 * Construct a tuple type using the given element types.
	 * 
	 * @param element
	 */
	public static final Tuple T_TUPLE(java.util.List<Type> elements) {
		int len = 1;
		for(Type b : elements) {
			// could be optimised slightly
			len += nodes(b).length;
		}		
		Node[] nodes = new Node[len];
		int[] children = new int[elements.size()];
		int start = 1;
		for(int i=0;i!=elements.size();++i) {
			children[i] = start;
			Node[] comps = nodes(elements.get(i));
			Node.insertNodes(start,comps,nodes);
			start += comps.length;
		}
		nodes[0] = new Node(K_TUPLE, children);		
		return new Tuple(nodes);
	}
	
	/**
	 * Construct a process type using the given element type.
	 * 
	 * @param element
	 */
	public static final Process T_PROCESS(Type element) {
		if (element instanceof Leaf) {
			return new Process(new Node[] { new Node(K_PROCESS, 1),
					new Node(Node.leafKind((Leaf) element), null) });
		} else {
			// Compound type
			Node[] nodes = Node.insertComponent(((Compound) element).nodes);
			nodes[0] = new Node(K_PROCESS, 1);
			return new Process(nodes);
		}
	}
	
	public static final Existential T_EXISTENTIAL(NameID name) {
		if (name == null) {
			throw new IllegalArgumentException(
					"existential name cannot be null");
		}
		return new Existential(name);
	}
	
	/**
	 * Construct a set type using the given element type.
	 * 
	 * @param element
	 */
	public static final Set T_SET(Type element) {
		if (element instanceof Leaf) {
			return new Set(new Node[] { new Node(K_SET, 1),
					new Node(Node.leafKind((Leaf) element), null) });
		} else {
			// Compound type
			Node[] nodes = Node.insertComponent(((Compound) element).nodes);
			nodes[0] = new Node(K_SET, 1);
			return new Set(nodes);
		}
	}
	
	/**
	 * Construct a list type using the given element type.
	 * 
	 * @param element
	 */
	public static final List T_LIST(Type element) {
		if (element instanceof Leaf) {
			return new List(new Node[] { new Node(K_LIST, 1),
					new Node(Node.leafKind((Leaf) element), null) });
		} else {
			// Compound type
			Node[] nodes = Node.insertComponent(((Compound) element).nodes);
			nodes[0] = new Node(K_LIST, 1);
			return new List(nodes);
		}
	}
	
	/**
	 * Construct a dictionary type using the given key and value types.
	 * 
	 * @param element
	 */
	public static final Dictionary T_DICTIONARY(Type key, Type value) {
		Node[] keyComps = nodes(key);
		Node[] valueComps = nodes(value);
		Node[] nodes = new Node[1 + keyComps.length + valueComps.length];
			
		Node.insertNodes(1,keyComps,nodes);
		Node.insertNodes(1+keyComps.length,valueComps,nodes);
		nodes[0] = new Node(K_DICTIONARY, new Pair(1,1+keyComps.length));
		return new Dictionary(nodes);		
	}
	
	/**
	 * Construct a union type using the given type bounds
	 * 
	 * @param element
	 */
	public static final Type T_UNION(Collection<Type> bounds) {		
		Type[] ts = new Type[bounds.size()];
		int i = 0;
		for(Type t : bounds) {
			ts[i++] = t;
		}		
		return T_UNION(ts);
	}
	
	/**
	 * Construct a union type using the given type bounds
	 * 
	 * @param element
	 */
	public static final Type T_UNION(Type... bounds) {		
		int len = 1;		
		for(Type b : bounds) {							
			// could be optimised slightly
			len += nodes(b).length;
		}		
		Node[] nodes = new Node[len];
		int[] children = new int[bounds.length];
		int start = 1;
		for(int i=0;i!=bounds.length;++i) {
			children[i] = start;
			Node[] comps = nodes(bounds[i]);
			Node.insertNodes(start,comps,nodes);
			start += comps.length;			
		}
		nodes[0] = new Node(K_UNION, children);
		return new Union(nodes);		
	}	
	
	/**
	 * Construct an intersection type using the given type bounds
	 * 
	 * @param element
	 */
	public static final Type T_INTERSECTION(Collection<Type> bounds) {		
		Type[] ts = new Type[bounds.size()];
		int i = 0;
		for(Type t : bounds) {
			ts[i++] = t;
		}		
		return T_INTERSECT(ts);
	}
	
	/**
	 * Construct an intersection type using the given type bounds
	 * 
	 * @param element
	 */
	public static final Type T_INTERSECT(Type... bounds) {		
		int len = 1;		
		for(Type b : bounds) {							
			// could be optimised slightly
			len += nodes(b).length;
		}		
		Node[] nodes = new Node[len];
		int[] children = new int[bounds.length];
		int start = 1;
		for(int i=0;i!=bounds.length;++i) {
			children[i] = start;
			Node[] comps = nodes(bounds[i]);
			Node.insertNodes(start,comps,nodes);
			start += comps.length;			
		}
		nodes[0] = new Node(K_INTERSECTION, children);
		return new Intersection(nodes);		
	}	
	
	/**
	 * Construct a difference of two types.
	 * 
	 * @param element
	 */
	public static final Type T_DIFFERENCE(Type left, Type right) {		
		Node[] leftComps = nodes(left);
		Node[] rightComps = nodes(right);
		Node[] nodes = new Node[1 + leftComps.length + rightComps.length];
			
		Node.insertNodes(1,leftComps,nodes);
		Node.insertNodes(1+leftComps.length,rightComps,nodes);
		nodes[0] = new Node(K_DIFFERENCE, new int[]{1,1+leftComps.length});
		return new Difference(nodes);			
	}
	
	public static final Fun T_FUN(Type ret,
			Collection<Type> params) {
		Type[] ts = new Type[params.size()];
		int i = 0;
		for (Type t : params) {
			ts[i++] = t;
		}
		return T_FUN(ret, ts);
	}
	
	/**
	 * Construct a function type using the given return and parameter types.
	 * 
	 * @param element
	 */
	public static final Fun T_FUN(Type ret, Type... params) {
		Node[] reccomps;		
		reccomps = new Node[0];		
		Node[] retcomps = nodes(ret); 
		int len = 1 + reccomps.length + retcomps.length;
		for(Type b : params) {
			// could be optimised slightly
			len += nodes(b).length;
		}		
		Node[] nodes = new Node[len];
		int[] children = new int[2 + params.length];
		Node.insertNodes(1,reccomps,nodes);
		Node.insertNodes(1+reccomps.length,retcomps,nodes);
		children[0] = -1;
		children[1] = 1 + reccomps.length;
		int start = 1 + reccomps.length + retcomps.length;		
		for(int i=0;i!=params.length;++i) {
			children[i+2] = start;
			Node[] comps = nodes(params[i]);
			Node.insertNodes(start,comps,nodes);
			start += comps.length;
		}
		nodes[0] = new Node(K_FUNCTION, children);
		return new Fun(nodes);		
	}
	
	public static final Meth T_METH(Process receiver, Type ret,
			Collection<Type> params) {
		Type[] ts = new Type[params.size()];
		int i = 0;
		for (Type t : params) {
			ts[i++] = t;
		}
		return T_METH(receiver, ret, ts);
	}
	
	/**
	 * Construct a function type using the given return and parameter types.
	 * 
	 * @param element
	 */
	public static final Meth T_METH(Process receiver, Type ret, Type... params) {				
		if(ret == null) {
			throw new IllegalArgumentException("T_METH ret cannot be null");			
		} 
		Node[] reccomps;
		if(receiver != null) {
			reccomps = nodes(receiver);				
		} else {
			reccomps = new Node[0];
		}		
		Node[] retcomps = nodes(ret); 
		int len = 1 + reccomps.length + retcomps.length;
		for(Type b : params) {
			// could be optimised slightly
			len += nodes(b).length;
		}		
		Node[] nodes = new Node[len];
		int[] children = new int[2 + params.length];
		Node.insertNodes(1,reccomps,nodes);
		Node.insertNodes(1+reccomps.length,retcomps,nodes);
		children[0] = receiver == null ? -1 : 1;
		children[1] = 1 + reccomps.length;
		int start = 1 + reccomps.length + retcomps.length;		
		for(int i=0;i!=params.length;++i) {
			children[i+2] = start;
			Node[] comps = nodes(params[i]);
			Node.insertNodes(start,comps,nodes);
			start += comps.length;
		}
		nodes[0] = new Node(K_METHOD, children);
		return new Meth(nodes);		
	}
	
	/**
	 * Construct a record type using the given fields.
	 * 
	 * @param element
	 */
	public static final Record T_RECORD(Map<String,Type> fields) {		
		ArrayList<String> keys = new ArrayList<String>(fields.keySet());
		Collections.sort(keys);		
		int len = 1;
		for(int i=0;i!=keys.size();++i) {
			String k = keys.get(i);
			Type t = fields.get(k);			
			len += nodes(t).length;
		}
		Node[] nodes = new Node[len];
		Pair<String,Integer>[] children = new Pair[fields.size()];
		int start = 1;
		for(int i=0;i!=children.length;++i) {			
			String k = keys.get(i);
			children[i] = new Pair<String,Integer>(k,start);
			Node[] comps = nodes(fields.get(k));
			Node.insertNodes(start,comps,nodes);
			start += comps.length;
		}
		nodes[0] = new Node(K_RECORD,children);
		return new Record(nodes);
	}

	/**
	 * Construct a label type. These are used in the construction of recursive
	 * types. Essentially, a label corresponds to the leaf of a recursive type,
	 * which we can then "close" later on as we build up the type. For example,
	 * we construct the recursive type <code>X<null|{X next}></code> as follows:
	 * 
	 * <pre>
	 * HashMap<String,Type> fields = new HashMap<String,Type>();
	 * fields.put("next",T_LABEL("X"));	 * 
	 * Type tmp = T_UNION(T_NULL,T_RECORD(fields));
	 * Type type = T_RECURSIVE("X",tmp);
	 * </pre>
	 * 
	 * <b>NOTE:</b> a type containing a label is not considered valid until it
	 * is closed using a recursive type.
	 * 
	 * @param label
	 * @return
	 */
	public static final Type T_LABEL(String label) {
		return new Compound(new Node[]{new Node(K_LABEL,label)});
	}

	/**
	 * Close a recursive type using a given label. Essentially, this traverses
	 * the given type and routes each occurrence of the label to recursively
	 * point to the type's root. For example, we construct the recursive type
	 * <code>X<null|{X next}></code> as follows:
	 * 
	 * <pre>
	 * HashMap<String,Type> fields = new HashMap<String,Type>();
	 * fields.put("next",T_LABEL("X"));	 * 
	 * Type tmp = T_UNION(T_NULL,T_RECORD(fields));
	 * Type type = T_RECURSIVE("X",tmp);
	 * </pre>
	 * 
	 * <b>NOTE:</b> it is invalid to close a type which does not contain at
	 * least one instance of the given label. Doing this will cause an exception
	 * to be raised.
	 * 
	 * @param label
	 *            --- label to be used for closing.
	 * @param type
	 *            --- type to be closed.
	 * @return
	 */
	public static final Type T_RECURSIVE(String label, Type type) {
		// first stage, identify all matching labels
		if(type instanceof Leaf) { throw new IllegalArgumentException("cannot close a leaf type"); }
		Compound compound = (Compound) type;
		Node[] nodes = compound.nodes;
		int[] rmap = new int[nodes.length];		
		int nmatches = 0;
		for(int i=0;i!=nodes.length;++i) {
			Node c = nodes[i];
			if(c.kind == K_LABEL && c.data.equals(label)) {
				rmap[i] = 0;
				nmatches++;
			} else {
				rmap[i] = i - nmatches;
			}
		}
		if (nmatches == 0) {
			throw new IllegalArgumentException(
					"type cannot be closed, as it contains no matching labels");
		}
		Node[] newnodes = new Node[nodes.length-nmatches];
		nmatches = 0;
		for(int i=0;i!=nodes.length;++i) {
			Node c = nodes[i];
			if(c.kind == K_LABEL && c.data.equals(label)) {				
				nmatches++;
			} else {
				newnodes[i-nmatches] = Node.remap(nodes[i],rmap);
			}
		}
		return construct(newnodes);
	}

	/**
	 * The following code converts a "type string" into an actual type. This is
	 * useful, amongst other things, for debugging.
	 * 
	 * @param str
	 * @return
	 */
	public static Type fromString(String str) {
		return new Parser(str).parse();
	}

		
	/**
	 * This is a utility helper for constructing types. In particular, it's
	 * useful for determine whether or not a type needs to be closed. An open
	 * type is one which contains a "dangling" reference to some node which
	 * needs to be connected to back to form a cycle.
	 * 
	 * @param label
	 * @param t
	 * @return
	 */
	public static boolean isOpen(String label, Type t) {
		if (t instanceof Leaf) {
			return false;
		}
		Compound graph = (Compound) t;
		for (Node n : graph.nodes) {
			if (n.kind == K_LABEL && n.data.equals(label)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This is a utility helper for constructing types. In particular, it's
	 * useful to check that a type has been built sanely.
	 * 
	 * @param label
	 * @param t
	 * @return
	 */
	public static boolean isOpen(Type t) {
		if (t instanceof Leaf) {
			return false;
		}
		Compound graph = (Compound) t;
		for (Node n : graph.nodes) {			
			if (n.kind == K_LABEL) {
				return true;
			}
		}
		return false;
	}
	
	// =============================================================
	// Serialisation Helpers
	// =============================================================

		
	
	/**
	 * Construct a copy of a given type using a given type builder. The normal
	 * reason to do this is that the representation of the type being built is
	 * not the internal one --- e.g. it's a textual or binary representation for
	 * serialisation.
	 * 
	 * @param writer
	 * @param type
	 */
	public static void build(TypeBuilder writer, Type type) {
		if(type instanceof Leaf) {			
			writer.initialise(1);
			writer.buildPrimitive(0,(Type.Leaf)type);
		} else {				
			Compound graph = (Compound) type;
			writer.initialise(graph.nodes.length);
			Node[] nodes = graph.nodes;
			for(int i=0;i!=nodes.length;++i) {
				Node node = nodes[i];						
				switch (node.kind) {
				case K_VOID:
					writer.buildPrimitive(i,T_VOID);
					break;
				case K_ANY:
					writer.buildPrimitive(i,T_ANY);
					break;
				case K_NULL:
					writer.buildPrimitive(i,T_NULL);
					break;
				case K_BOOL:
					writer.buildPrimitive(i,T_BOOL);
					break;
				case K_BYTE:
					writer.buildPrimitive(i,T_BYTE);
					break;
				case K_CHAR:
					writer.buildPrimitive(i,T_CHAR);
					break;
				case K_INT:
					writer.buildPrimitive(i,T_INT);
					break;
				case K_RATIONAL:
					writer.buildPrimitive(i,T_REAL);
					break;
				case K_STRING:
					writer.buildPrimitive(i,T_STRING);
					break;
				case K_SET:
					writer.buildSet(i,(Integer) node.data);
					break;
				case K_LIST:
					writer.buildList(i,(Integer) node.data);												
					break;
				case K_PROCESS:
					writer.buildProcess(i,(Integer) node.data);	
					break;
				case K_EXISTENTIAL:					
					writer.buildExistential(i,(NameID) node.data);	
					break;
				case K_DICTIONARY: {
					// binary node
					Pair<Integer, Integer> p = (Pair<Integer, Integer>) node.data;
					writer.buildDictionary(i,p.first(),p.second());										
					break;
				}
				case K_UNION: {
					int[] bounds = (int[]) node.data;			
					writer.buildUnion(i,bounds);
					break;
				}
				case K_TUPLE: {									
					int[] bounds = (int[]) node.data;								
					writer.buildTuple(i,bounds);					
					break;
				}				
				case K_FUNCTION: {				
					int[] bounds = (int[]) node.data;
					int[] params = new int[bounds.length-2];
					System.arraycopy(bounds, 2, params,0, params.length);
					writer.buildFunction(i,bounds[1],params);
					break;
				}
				case K_METHOD: {				
					int[] bounds = (int[]) node.data;
					int[] params = new int[bounds.length-2];
					System.arraycopy(bounds, 2, params,0, params.length);
					writer.buildMethod(i,bounds[0],bounds[1],params);
					break;
				}
				case K_RECORD: {
					// labeled nary node					
					Pair<String, Integer>[] fields = (Pair<String, Integer>[]) node.data;
					writer.buildRecord(i,fields);
					break;
				}
				default: 
					throw new IllegalArgumentException("Invalid type encountered");
				}
			}		
		}
	}
	
	
	// =============================================================
	// Type operations
	// =============================================================

		/**
	 * Determine whether type <code>t2</code> is a <i>coercive subtype</i> of
	 * type <code>t1</code> (written t1 :> t2). In other words, whether the set
	 * of all possible values described by the type <code>t2</code> is a subset
	 * of that described by <code>t1</code>.
	 */
	public static boolean isCoerciveSubtype(Type t1, Type t2) {				
		Node[] g1 = nodes(t1);
		Node[] g2 = nodes(t2);
		SubtypeInference inference = new CoerciveSubtypeOperator(g1,g2);		
		SubtypeRelation rel = inference.doInference();		
		return rel.isSubtype(0, 0); 
	}
	
	/**
	 * Determine whether type <code>t2</code> is a <i>subtype</i> of type
	 * <code>t1</code> (written t1 :> t2). In other words, whether the set of
	 * all possible values described by the type <code>t2</code> is a subset of
	 * that described by <code>t1</code>.
	 */
	public static boolean isSubtype(Type t1, Type t2) {				
		Node[] g1 = nodes(t1);
		Node[] g2 = nodes(t2);
		SubtypeInference inference = new DefaultSubtypeOperator(g1,g2);		
		SubtypeRelation rel = inference.doInference();		
		return rel.isSubtype(0, 0); 
	}

	/**
	 * Check whether two types are <i>isomorphic</i>. This is true if they are
	 * identical, or encode the same structure.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static boolean isomorphic(Type t1, Type t2) {
		return isSubtype(t1,t2) && isSubtype(t2,t1);
	}
	
	/**
	 * Compute the <i>least upper bound</i> of two types t1 and t2. The least upper
	 * bound is a type t3 where <code>t3 :> t1</code>, <code>t3 :> t2</code> and
	 * there does not exist a type t4, where <code>t3 :> t4</code>,
	 * <code>t4 :> t1</code>, <code>t4 :> t2</code>.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Type leastUpperBound(Type t1, Type t2) {
		return minimise(T_UNION(t1,t2)); // so easy
	}
	
	/**
	 * Compute the <i>greatest lower bound</i> of two types t1 and t2. The
	 * greatest lower bound is a type t3 where <code>t1 :> t3</code>,
	 * <code>t2 :> t3</code> and there does not exist a type t4, where
	 * <code>t1 :> t4</code>, <code>t2 :> t4</code> and <code>t4 :> t3</code>.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Type greatestLowerBound(Type t1, Type t2) {
		// BUG FIX: this algorithm still isn't implemented correctly.
		// -------> it can infinite loop as the recursion isn't terminated.
		if(isSubtype(t1,t2)) {			
			return t2;
		} else if(isSubtype(t2,t1)) {			
			return t1;
		} else {
			Node[] graph1, graph2;
			if(t1 instanceof Leaf) {
				graph1 = new Node[] { new Node(Node.leafKind((Type.Leaf) t1), null) };
			} else {
				graph1 = ((Compound)t1).nodes;
			}
			if(t2 instanceof Leaf) {
				graph2 = new Node[] { new Node(Node.leafKind((Type.Leaf) t2), null) };
			} else {
				graph2 = ((Compound)t2).nodes;
			}
			ArrayList<Node> newNodes = new ArrayList<Node>();
			intersect(0,graph1,0,graph2,newNodes, new HashMap());
			Type glb = construct(newNodes.toArray(new Node[newNodes.size()]));				
			return minimise(glb);
		}
	}

	/**
	 * Let <code>S</code> be determined by subtracting the set of values
	 * described by type <code>t2</code> from that described by <code>t1</code>.
	 * Then, this method returns the <i>least</i> type <code>t3</code> which
	 * covers <code>S</code> (that is, every value in <code>S</code> is in the
	 * set of values described by <code>t3</code>). Unfortunately, in some
	 * cases, <code>t3</code> may contain other (spurious) values not found in
	 * <code>S</code>.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Type leastDifference(Type t1, Type t2) {
		// BUG FIX: this algorithm still isn't implemented correctly.
		// -------> it can infinite loop as the recursion isn't terminated.
		if(isSubtype(t2,t1)) {			
			return T_VOID;
		} else {
			Node[] graph1, graph2;
			if(t1 instanceof Leaf) {
				graph1 = new Node[] { new Node(Node.leafKind((Type.Leaf) t1), null) };
			} else {
				graph1 = ((Compound)t1).nodes;
			}
			if(t2 instanceof Leaf) {
				graph2 = new Node[] { new Node(Node.leafKind((Type.Leaf) t2), null) };
			} else {
				graph2 = ((Compound)t2).nodes;
			}
			SubtypeRelation assumptions = new DefaultSubtypeOperator(graph1,graph2).doInference();			
			ArrayList<Node> newNodes = new ArrayList<Node>();
			difference(0,graph1,0,graph2,newNodes, new HashMap(),assumptions);
			Type ldiff = construct(newNodes.toArray(new Node[newNodes.size()]));
			return minimise(ldiff);
		}
	}

	/**
	 * The effective record type gives a subset of the visible fields which are
	 * guaranteed to be in the type. For example, consider this type:
	 * 
	 * <pre>
	 * {int op, int x} | {int op, [int] y}
	 * </pre>
	 * 
	 * Here, the field op is guaranteed to be present. Therefore, the effective
	 * record type is just <code>{int op}</code>.
	 * 
	 * @param t
	 * @return
	 */
	public static Record effectiveRecordType(Type t) {
		if (t instanceof Type.Record) {
			return (Type.Record) t;
		} else if (t instanceof Type.Union) {
			Union ut = (Type.Union) t;
			Record r = null;
			for (Type b : ut.bounds()) {
				if (!(b instanceof Record)) {
					return null;
				}
				Record br = (Record) b;
				if (r == null) {
					r = br;
				} else {
					HashMap<String, Type> rfields = r.fields();
					HashMap<String, Type> bfields = br.fields();
					HashMap<String, Type> nfields = new HashMap();
					for (Map.Entry<String, Type> e : rfields.entrySet()) {
						Type bt = bfields.get(e.getKey());
						if (bt != null) {
							nfields.put(e.getKey(),
									leastUpperBound(e.getValue(), bt));
						}
					}					
					r = T_RECORD(nfields);
				}
			}
			return r;
		}
		return null;
	}

	public static Set effectiveSetType(Type t) {
		if (t instanceof Type.Set) {
			return (Type.Set) t;
		} else if (t instanceof Type.Union) {			
			Union ut = (Type.Union) t;
			Set r = null;
			for (Type b : ut.bounds()) {
				if (!(b instanceof Set)) {
					return null;
				}
				Set br = (Set) b;
				if (r == null) {
					r = br;
				} else {
					r = T_SET(leastUpperBound(r.element(),br.element()));
				}
			}			
			return r;
		}
		return null;
	}
	
	public static List effectiveListType(Type t) {
		if (t instanceof Type.List) {
			return (Type.List) t;
		} else if (t instanceof Type.Union) {			
			Union ut = (Type.Union) t;
			List r = null;
			for (Type b : ut.bounds()) {
				if (!(b instanceof List)) {
					return null;
				}
				List br = (List) b;
				if (r == null) {
					r = br;
				} else {
					r = T_LIST(leastUpperBound(r.element(),br.element()));
				}
			}			
			return r;
		}
		return null;
	}
	
	public static Dictionary effectiveDictionaryType(Type t) {
		if (t instanceof Type.Dictionary) {
			return (Type.Dictionary) t;
		} else if (t instanceof Type.Union) {
			Union ut = (Type.Union) t;
			Dictionary r = null;
			for (Type b : ut.bounds()) {
				if (!(b instanceof Dictionary)) {
					return null;
				}
				Dictionary br = (Dictionary) b;
				if (r == null) {
					r = br;
				} else {
					r = T_DICTIONARY(leastUpperBound(r.key(), br.key()),
							leastUpperBound(r.value(), br.value()));
				}
			}
			return r;
		}
		return null;
	}
	
	/**
	 * <p>
	 * The following algorithm minimises a type. For example:
	 * </p>
	 * 
	 * <pre>
	 * define InnerList as null|{int data, OuterList next}
	 * define OuterList as null|{int data, InnerList next}
	 * </pre>
	 * <p>
	 * This type is minimised into the following (equivalent) form:
	 * </p>
	 * 
	 * <pre>
	 * define LinkedList as null|{int data, LinkedList next}
	 * </pre>
	 * <p>
	 * The minisation algorithm is based on the well-known algorithm for
	 * minimising a DFA (see e.g. <a
	 * href="http://en.wikipedia.org/wiki/DFA_minimization">[1]</a>).
	 * </p>
	 * <p>
	 * The algorithm operates by performing a subtype test of each node against
	 * all others. From this, we can identify nodes which are equivalent under
	 * the subtype operator. Using this information, the type is reconstructed
	 * such that for each equivalence class only a single node is created.
	 * </p>
	 * <p>
	 * <b>NOTE:</b> this algorithm does not put the type into a canonical form.
	 * Additional work is necessary to do this.
	 * </p>
	 * 
	 * @param type
	 * @return
	 */
	public static Type minimise(Type type) {		
		return Algorithms.minimise(type);
	}

	
	private static int intersect(int n1, Node[] graph1, int n2, Node[] graph2,
			ArrayList<Node> newNodes,
			HashMap<Pair<Integer, Integer>, Integer> allocations) {	
		Integer idx = allocations.get(new Pair(n1,n2));
		if(idx != null) {
			// this indicates an allocation has already been performed for this
			// pair.  
			return idx;
		}
		
		Node c1 = graph1[n1];
		Node c2 = graph2[n2];				
		int nid = newNodes.size(); // my node id
		newNodes.add(null); // reserve space for my node	
		allocations.put(new Pair(n1,n2), nid);
		Node node; // new node being created
		
		if(c1.kind == c2.kind) { 
			switch(c1.kind) {
			case K_VOID:
			case K_ANY:
			case K_META:
			case K_NULL:
			case K_BOOL:
			case K_BYTE:
			case K_CHAR:
			case K_INT:
			case K_RATIONAL:
			case K_STRING:
				node = c1;
				break;
			case K_EXISTENTIAL:
				NameID nid1 = (NameID) c1.data;
				NameID nid2 = (NameID) c2.data;				
				if(nid1.name().equals(nid2.name())) {
					node = c1;
				} else {
					node = new Node(K_VOID,null);
				}
				break;
			case K_SET:
			case K_LIST:
			case K_PROCESS: {
				// unary node
				int e1 = (Integer) c1.data;
				int e2 = (Integer) c2.data;
				int element = intersect(e1,graph1,e2,graph2,newNodes,allocations);
				node = new Node(c1.kind,element);
				break;
			}
			case K_DICTIONARY: {
				// binary node
				Pair<Integer, Integer> p1 = (Pair<Integer, Integer>) c1.data;
				Pair<Integer, Integer> p2 = (Pair<Integer, Integer>) c2.data;
				int key = intersect(p1.first(),graph2,p2.first(),graph2,newNodes,allocations);
				int value = intersect(p1.second(),graph2,p2.second(),graph2,newNodes,allocations);
				node = new Node(K_DICTIONARY,new Pair(key,value));
				break;
			}		
			case K_TUPLE:  {
				// nary nodes
				int[] elems1 = (int[]) c1.data;
				int[] elems2 = (int[]) c2.data;
				if(elems1.length != elems2.length) {
					// TODO: can we do better here?
					node = new Node(K_VOID,null);
				} else {
					int[] nelems = new int[elems1.length];
					for(int i=0;i!=nelems.length;++i) {
						nelems[i] = intersect(elems1[i],graph1,elems2[i],graph2,newNodes,allocations);
					}
					node = new Node(K_TUPLE,nelems);
				}
				break;
			}
			case K_METHOD:
			case K_FUNCTION:  {
				// nary nodes
				int[] elems1 = (int[]) c1.data;
				int[] elems2 = (int[]) c2.data;
				int e1 = elems1[0];
				int e2 = elems2[0];
				if(elems1.length != elems2.length){
					node = new Node(K_VOID,null);
				} else if ((e1 == -1 || e2 == -1) && e1 != e2) {
					node = new Node(K_VOID, null);
				} else {
					int[] nelems = new int[elems1.length];
					// TODO: need to check here whether or not this is the right
					// thing to do. My gut is telling me that covariant and
					// contravariance should be treated differently ...
					for (int i = 0; i != nelems.length; ++i) {
						nelems[i] = intersect(elems1[i], graph1, elems2[i],
								graph2, newNodes,allocations);
					}
					node = new Node(c1.kind, nelems);
				}
				break;
			}
			case K_RECORD: 
					// labeled nary nodes
					outer : {
						Pair<String, Integer>[] fields1 = (Pair<String, Integer>[]) c1.data;
						Pair<String, Integer>[] fields2 = (Pair<String, Integer>[]) c2.data;
						int old = newNodes.size();
						if (fields1.length != fields2.length) {
							node = new Node(K_VOID, null);
						} else {
							Pair<String, Integer>[] nfields = new Pair[fields1.length];
							for (int i = 0; i != nfields.length; ++i) {
								Pair<String, Integer> e1 = fields1[i];
								Pair<String, Integer> e2 = fields2[i];
								if (!e1.first().equals(e2.first())) {
									node = new Node(K_VOID, null);
									break outer;
								} else {
									int nidx = intersect(e1.second(), graph1,
											e2.second(), graph2, newNodes,
											allocations);

									if (newNodes.get(nidx).kind == K_VOID) {
										// A record with a field of void type
										// cannot exist --- it's just equivalent
										// to void.
										while (newNodes.size() != old) {
											newNodes.remove(newNodes.size() - 1);
										}
										node = new Node(K_VOID, null);
										break outer;
									}

									nfields[i] = new Pair<String, Integer>(
											e1.first(), nidx);
								}
							}
							node = new Node(K_RECORD, nfields);
						}						
					}	
				break;
			case K_UNION: {
				// This is the hardest (i.e. most expensive) case. Essentially, I
				// just check that for each bound in one node, there is an
				// equivalent bound in the other.
				int[] bounds1 = (int[]) c1.data;
				int[] nbounds = new int[bounds1.length];
								
				// check every bound in c1 is a subtype of some bound in c2.
				for (int i = 0; i != bounds1.length; ++i) {
					nbounds[i] = intersect(bounds1[i], graph1, n2, graph2,
							newNodes,allocations);
				}
				node = new Node(K_UNION,nbounds);
				break;
			}					
			default:
				throw new IllegalArgumentException("attempting to minimise open recurisve type");
			}		
		} else if(c1.kind == K_ANY) {			
			newNodes.remove(newNodes.size()-1);
			extractOnto(n2,graph2,newNodes);
			return nid;
		} else if(c2.kind == K_ANY) {			
			newNodes.remove(newNodes.size()-1);
			extractOnto(n1,graph1,newNodes);
			return nid;
		} else if (c1.kind == K_UNION){					
			int[] obounds = (int[]) c1.data;			
			int[] nbounds = new int[obounds.length];
							
			// check every bound in c1 is a subtype of some bound in c2.
			for (int i = 0; i != obounds.length; ++i) {
				nbounds[i] = intersect(obounds[i], graph1, n2, graph2,
						newNodes,allocations);
			}
			node = new Node(K_UNION,nbounds);
		} else if (c2.kind == K_UNION) {			
			int[] obounds = (int[]) c2.data;			
			int[] nbounds = new int[obounds.length];
							
			// check every bound in c1 is a subtype of some bound in c2.
			for (int i = 0; i != obounds.length; ++i) {
				nbounds[i] = intersect(n1,graph1,obounds[i], graph2,
						newNodes,allocations);
			}
			node = new Node(K_UNION,nbounds);
		} else {
			// default case --> go to void
			node = new Node(K_VOID,null);
		}
		// finally, create the new node!!!
		newNodes.set(nid, node);
		return nid;
	}
	
	private static int difference(int n1, Node[] graph1, int n2, Node[] graph2,
			ArrayList<Node> newNodes,
			HashMap<Pair<Integer, Integer>, Integer> allocations, SubtypeRelation matrix) {
		
		int nid = newNodes.size(); // my node id		
		if(matrix.isSupertype(n1,n2)) {
			newNodes.add(new Node(K_VOID,null));
			return nid; 
		}
		
		Integer idx = allocations.get(new Pair(n1,n2));
		if(idx != null) {
			// this indicates an allocation has already been performed for this
			// pair.  
			return idx;
		}
		
		Node c1 = graph1[n1];
		Node c2 = graph2[n2];				
		
		allocations.put(new Pair(n1,n2), nid);
		newNodes.add(null); // reserve space for my node	
		Node node; // new node being created
		
		if(c1.kind == c2.kind) { 
			switch(c1.kind) {
			case K_VOID:
			case K_ANY:
			case K_META:
			case K_NULL:
			case K_BOOL:
			case K_BYTE:
			case K_CHAR:
			case K_INT:
			case K_RATIONAL:
			case K_STRING:
				node = new Node(K_VOID,null);
				break;
			case K_EXISTENTIAL:
				NameID nid1 = (NameID) c1.data;
				NameID nid2 = (NameID) c2.data;				
				if(nid1.name().equals(nid2.name())) {
					node = new Node(K_VOID,null);					
				} else {
					node = c1;
				}
				break;
			case K_SET:
			case K_LIST:
			case K_PROCESS: {
				// unary node
				int e1 = (Integer) c1.data;
				int e2 = (Integer) c2.data;
				int element = difference(e1,graph1,e2,graph2,newNodes,allocations,matrix);
				node = new Node(c1.kind,element);
				break;
			}
			case K_DICTIONARY: {
				// binary node
				Pair<Integer, Integer> p1 = (Pair<Integer, Integer>) c1.data;
				Pair<Integer, Integer> p2 = (Pair<Integer, Integer>) c2.data;
				int key = difference(p1.first(),graph2,p2.first(),graph2,newNodes,allocations,matrix);
				int value = difference(p1.second(),graph2,p2.second(),graph2,newNodes,allocations,matrix);
				node = new Node(K_DICTIONARY,new Pair(key,value));
				break;
			}		
			case K_TUPLE:  {
				// nary nodes
				int[] elems1 = (int[]) c1.data;
				int[] elems2 = (int[]) c2.data;
				if(elems1.length != elems2.length) {
					node = c1;
				} else {
					int[] nelems = new int[elems1.length];
					for(int i=0;i!=nelems.length;++i) {
						nelems[i] = difference(elems1[i],graph1,elems2[i],graph2,newNodes,allocations,matrix);
					}
					node = new Node(K_TUPLE,nelems);
				}
				break;
			}
			case K_METHOD:
			case K_FUNCTION:  {
				// nary nodes
				int[] elems1 = (int[]) c1.data;
				int[] elems2 = (int[]) c2.data;
				int e1 = elems1[0];
				int e2 = elems2[0];
				if(elems1.length != elems2.length){
					node = c1;
				} else if ((e1 == -1 || e2 == -1) && e1 != e2) {
					node = c1;
				} else {
					int[] nelems = new int[elems1.length];
					// TODO: need to check here whether or not this is the right
					// thing to do. My gut is telling me that covariant and
					// contravariance should be treated differently ...
					for (int i = 0; i != nelems.length; ++i) {
						nelems[i] = difference(elems1[i], graph1, elems2[i],
								graph2, newNodes,allocations,matrix);
					}
					node = new Node(c1.kind, nelems);
				}
				break;
			}
			case K_RECORD:
				// labeled nary nodes
					outer : {
						Pair<String, Integer>[] fields1 = (Pair<String, Integer>[]) c1.data;
						Pair<String, Integer>[] fields2 = (Pair<String, Integer>[]) c2.data;
						int old = newNodes.size();
						if (fields1.length != fields2.length) {
							node = c1;
						} else {
							Pair<String, Integer>[] nfields = new Pair[fields1.length];
							boolean voidField = false;
							for (int i = 0; i != fields1.length; ++i) {
								Pair<String, Integer> e1 = fields1[i];
								Pair<String, Integer> e2 = fields2[i];
								if (!e1.first().equals(e2.first())) {
									node = c1;
									break outer;
								} else {
									int nidx = difference(e1.second(), graph1,
											e2.second(), graph2, newNodes,
											allocations, matrix);
									Node nnode = newNodes.get(nidx);
									if (nnode != null && nnode.kind == K_VOID) {
										voidField = true;
									}

									nfields[i] = new Pair<String, Integer>(
											e1.first(), nidx);
								}
							}
							if(voidField) {
								// A record with a field of void type
								// cannot exist --- it's just equivalent
								// to void.
								while (newNodes.size() != old) {
									newNodes.remove(newNodes.size() - 1);
								}
								node = new Node(K_VOID, null);
								break outer;
							}
							node = new Node(K_RECORD, nfields);
						}
					}
				break;
			case K_UNION: {
				// This is the hardest (i.e. most expensive) case. Essentially, I
				// just check that for each bound in one node, there is an
				// equivalent bound in the other.
				int[] bounds1 = (int[]) c1.data;
				int[] nbounds = new int[bounds1.length];
								
				// check every bound in c1 is a subtype of some bound in c2.
				for (int i = 0; i != bounds1.length; ++i) {
					nbounds[i] = difference(bounds1[i], graph1, n2, graph2,
							newNodes,allocations,matrix);
				}
				node = new Node(K_UNION,nbounds);
				break;
			}					
			default:
				throw new IllegalArgumentException("attempting to minimise open recurisve type");
			}		
		} else if(c1.kind == K_ANY) {			
			// TODO: try to do better
			node = new Node(K_ANY,null);
		} else if(c2.kind == K_ANY) {			
			node = new Node(K_VOID,null);
		} else if (c1.kind == K_UNION){					
			int[] obounds = (int[]) c1.data;			
			int[] nbounds = new int[obounds.length];
							
			for (int i = 0; i != obounds.length; ++i) {
				nbounds[i] = difference(obounds[i], graph1, n2, graph2,
						newNodes,allocations,matrix);
			}
			node = new Node(K_UNION,nbounds);
		} else if (c2.kind == K_UNION) {			
			int[] obounds = (int[]) c2.data;			
			int[] nbounds = new int[obounds.length];
							
			for (int i = 0; i != obounds.length; ++i) {
				nbounds[i] = difference(n1,graph1,obounds[i], graph2,
						newNodes,allocations,matrix);
			}
			// FIXME: this is broken. need intersection types.
			node = new Node(K_UNION,nbounds);
		} else {
			// default case --> go to no change
			node = c1;			
		}								
		
		if(node == c1) {
			while(newNodes.size() > nid) {
				newNodes.remove(newNodes.size()-1);
			}
						
			extractOnto(n1,graph1,newNodes);			
			return nid;
		} else {
			// finally, create the new node!!!
			newNodes.set(nid, node);
			return nid;
		}
	}
	
	// =============================================================
	// Primitive Types
	// =============================================================

	/**
	 * A leaf type represents a type which has no component types. For example,
	 * primitive types like <code>int</code> and <code>real</code> are leaf
	 * types.
	 * 
	 * @author djp
	 * 
	 */
	public static class Leaf extends Type {}

	/**
	 * A void type represents the type whose variables cannot exist! That is,
	 * they cannot hold any possible value. Void is used to represent the return
	 * type of a function which does not return anything. However, it is also
	 * used to represent the element type of an empty list of set. <b>NOTE:</b>
	 * the void type is a subtype of everything; that is, it is bottom in the
	 * type lattice.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Void extends Leaf {
		private Void() {}
		public boolean equals(Object o) {
			return this == o;
		}
		public int hashCode() {
			return 1;
		}
		public String toString() {
			return "void";
		}
	}

	/**
	 * The type any represents the type whose variables may hold any possible
	 * value. <b>NOTE:</b> the any type is top in the type lattice.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Any extends Leaf {
		private Any() {}
		public boolean equals(Object o) {
			return o == T_ANY;
		}
		public int hashCode() {
			return 1;
		}
		public String toString() {
			return "any";
		}
	}

	/**
	 * The null type is a special type which should be used to show the absence
	 * of something. It is distinct from void, since variables can hold the
	 * special <code>null</code> value (where as there is no special "void"
	 * value). With all of the problems surrounding <code>null</code> and
	 * <code>NullPointerException</code>s in languages like Java and C, it may
	 * seem that this type should be avoided. However, it remains a very useful
	 * abstraction to have around and, in Whiley, it is treated in a completely
	 * safe manner (unlike e.g. Java).
	 * 
	 * @author djp
	 * 
	 */
	public static final class Null extends Leaf {
		private Null() {}
		public boolean equals(Object o) {
			return this == o;
		}
		public int hashCode() {
			return 2;
		}
		public String toString() {
			return "null";
		}
	}

	/**
	 * The type mets represents the type of types. That is, values of this type
	 * are themselves types. (think reflection, where we have
	 * <code>class Class {}</code>).
	 * 
	 * @author djp
	 * 
	 */
	public static final class Meta extends Leaf {
		private Meta() {}
		public boolean equals(Object o) {
			return o == T_META;
		}
		public int hashCode() {
			return 1;
		}
		public String toString() {
			return "type";
		}
	}

	/**
	 * The existential type represents the an unknown type, defined at a given
	 * position.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Existential extends Compound{
		private Existential(NameID name) {
			super(new Node[] { new Node(K_EXISTENTIAL,name) });
		}
		public boolean equals(Object o) {
			if(o instanceof Existential) {
				Existential e = (Existential) o;
				return nodes[0].data.equals(nodes[0].data);
			}
			return false;
		}
		public NameID name() {
			return (NameID) nodes[0].data;
		}
		public int hashCode() {
			return nodes[0].data.hashCode();
		}
		public String toString() {
			return "?" + name();
		}
	}
	
	/**
	 * Represents the set of boolean values (i.e. true and false)
	 * @author djp
	 *
	 */
	public static final class Bool extends Leaf {
		private Bool() {}
		public boolean equals(Object o) {
			return o == T_BOOL;
		}
		public int hashCode() {
			return 3;
		}
		public String toString() {
			return "bool";
		}
	}

	/**
	 * Represents a unicode character.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Char extends Leaf {
		private Char() {}
		public boolean equals(Object o) {
			return o == T_CHAR;
		}
		public int hashCode() {
			return 4;
		}
		public String toString() {
			return "char";
		}	
	}
	
	/**
	 * Represents a unicode character.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Byte extends Leaf {
		private Byte() {}
		public boolean equals(Object o) {
			return o == T_BYTE;
		}
		public int hashCode() {
			return 4;
		}
		public String toString() {
			return "byte";
		}	
	}
	
	
	/**
	 * Represents the set of (unbound) integer values. Since integer types in
	 * Whiley are unbounded, there is no equivalent to Java's
	 * <code>MIN_VALUE</code> and <code>MAX_VALUE</code> for <code>int</code>
	 * types.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Int extends Leaf {
		private Int() {}
		public boolean equals(Object o) {
			return o == T_INT;
		}
		public int hashCode() {
			return 4;
		}
		public String toString() {
			return "int";
		}	
	}

	/**
	 * Represents the set of (unbound) rational numbers. 
	 * 
	 * @author djp
	 * 
	 */
	public static final class Real extends Leaf {
		private Real() {}
		public boolean equals(Object o) {
			return o == T_REAL;
		}
		public int hashCode() {
			return 5;
		}
		public String toString() {
			return "real";
		}
	}
	
	/**
	 * Represents a string of characters 
	 * 
	 * @author djp
	 * 
	 */
	public static final class Strung extends Leaf {
		private Strung() {}
		public boolean equals(Object o) {
			return o == T_STRING;
		}
		public int hashCode() {
			return 6;
		}
		public String toString() {
			return "string";
		}
	}
	
	// =============================================================
	// Compound Type
	// =============================================================

	/**
	 * A Compound data structure is essentially a graph encoding of a type. Each
	 * node in the graph corresponds to a component of the type. Recursive
	 * cycles in the graph are permitted. <b>NOTE:</b> care must be take to
	 * ensure that the root of the graph (namely node at index 0) matches the
	 * actual Compound class (i.e. if its kind is K_SET, then this is an
	 * instance of Set).
	 */
	public static class Compound extends Type {
		public final Node[] nodes;
		
		public Compound(Node[] nodes) {
			this.nodes = nodes;
		}

		/**
		 * Determine the hashCode of a type.
		 */
		public int hashCode() {
			int r = 0;
			for(Node c : nodes) {
				r = r + c.hashCode();
			}
			return r;
		}

		/**
		 * This method compares two compound types to test whether they are
		 * <i>identical</i>. Observe that it does not perform an
		 * <i>isomorphism</i> test. Thus, two distinct types which are
		 * structurally isomorphic will <b>not</b> be considered equal under
		 * this method. <b>NOTE:</b> to test whether two types are structurally
		 * isomorphic, using the <code>isomorphic(t1,t2)</code> method.
		 */
		public boolean equals(Object o) {
			if(o instanceof Compound) {
				Node[] cs = ((Compound) o).nodes;
				if(cs.length != nodes.length) {
					return false;
				}
				for(int i=0;i!=cs.length;++i) {
					if(!nodes[i].equals(cs[i])) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		
		/**
		 * The extract method basically performs a DFS from the root, extracts
		 * what it finds and minimises it.
		 * 
		 * @param root
		 *            --- the starting node to extract from.
		 * @return
		 */
		protected final Type extract(int root) {
			return construct(Type.extract(root,nodes));
		}
		
		public String toString() {
			return TypeToString.toString(nodes);
		}
	}

	private static final Node[] extract(int root, Node[] nodes) {
		// First, we perform the DFS.
		BitSet visited = new BitSet(nodes.length);
		// extracted maps new indices to old indices
		ArrayList<Integer> extracted = new ArrayList<Integer>();
		subgraph(root,visited,extracted,nodes);		
		// rextracted is the reverse of extracted
		int[] rextracted = new int[nodes.length];
		int i=0;
		for(int j : extracted) {
			rextracted[j]=i++;
		}
		Node[] newNodes = new Node[extracted.size()];
		i=0;
		for(int j : extracted) {
			newNodes[i++] = Node.remap(nodes[j],rextracted);  
		}
			
		return newNodes;
	}
	
	private static final void extractOnto(int root, Node[] nodes,
			ArrayList<Node> newNodes) {
		// First, we perform the DFS.
		BitSet visited = new BitSet(nodes.length);
		// extracted maps new indices to old indices
		ArrayList<Integer> extracted = new ArrayList<Integer>();
		subgraph(root, visited, extracted, nodes);
		// rextracted is the reverse of extracted
		int[] rextracted = new int[nodes.length];
		int i = newNodes.size();
		for (int j : extracted) {
			rextracted[j] = i++;
		}				
		for (int j : extracted) {
			newNodes.add(Node.remap(nodes[j], rextracted));
		}
	}
	
	/**
	 * The following method recursively extracts the subgraph rooted at
	 * <code>index</code> in the given graph using a depth-first search.
	 * Vertices in the subgraph are added to <code>extracted</code> in the order
	 * they are visited.
	 * 
	 * @param index
	 *            --- the node to extract the subgraph from.
	 * @param visited
	 *            --- the set of vertices already visited
	 * @param extracted
	 *            --- the list of vertices that make up the subgraph which is
	 *            built by this method.
	 * @param graph
	 *            --- the graph.
	 */
	private final static void subgraph(int index, BitSet visited,
			ArrayList<Integer> extracted, Node[] graph) {
		if(visited.get(index)) { return; } // node already visited}
		extracted.add(index);
		visited.set(index);
		Node node = graph[index];
		switch(node.kind) {
		case K_SET:
		case K_LIST:
		case K_PROCESS:
			// unary nodes
			subgraph((Integer) node.data,visited,extracted,graph);
			break;
		case K_DICTIONARY:
			// binary node
			Pair<Integer,Integer> p = (Pair<Integer,Integer>) node.data;
			subgraph(p.first(),visited,extracted,graph);
			subgraph(p.second(),visited,extracted,graph);
			break;
		case K_TUPLE:
		case K_UNION:
		case K_METHOD:
		case K_FUNCTION:
			// nary node
			int[] bounds = (int[]) node.data;
			for(int b : bounds) {
				if(b == -1) { continue; } // possible with K_FUNCTION				
				subgraph(b,visited,extracted,graph);				
			}
			break;
		case K_RECORD:
			// labeled nary node
			Pair<String,Integer>[] fields = (Pair<String,Integer>[]) node.data;
			for(Pair<String,Integer> f : fields) {
				subgraph(f.second(),visited,extracted,graph);
			}
			break;			
		}
	}

		
	// =============================================================
	// Compound Faces
	// =============================================================

	/*
	 * The compound faces are not technically necessary, as they simply provide
	 * interfaces to the underlying nodes of a compound type. However, they
	 * certainly make it more pleasant to use this library.
	 */

	/**
	 * A tuple type describes a compound type made up of two or more
	 * subcomponents. It is similar to a record, except that fields are
	 * effectively anonymous.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Tuple extends Compound  {
		private Tuple(Node[] nodes) {
			super(nodes);
		}		
		public java.util.List<Type> elements() {
			int[] values = (int[]) nodes[0].data;
			ArrayList<Type> elems = new ArrayList<Type>();
			for(Integer i : values) {
				elems.add(extract(i));
			}
			return elems;
		}		
	}	
	
	/**
	 * A set type describes set values whose elements are subtypes of the
	 * element type. For example, <code>{1,2,3}</code> is an instance of set
	 * type <code>{int}</code>; however, <code>{1.345}</code> is not.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Set extends Compound  {
		private Set(Node[] nodes) {
			super(nodes);
		}
		public Type element() {
			int elemIdx = (Integer) nodes[0].data;
			return extract(elemIdx);			
		}		
	}

	/**
	 * A list type describes list values whose elements are subtypes of the
	 * element type. For example, <code>[1,2,3]</code> is an instance of list
	 * type <code>[int]</code>; however, <code>[1.345]</code> is not.
	 * 
	 * @author djp
	 * 
	 */
	public static final class List extends Compound  {
		private List(Node[] nodes) {
			super(nodes);
		}
		public Type element() {			
			int elemIdx = (Integer) nodes[0].data;
			return extract(elemIdx);
		}		
	}

	/**
	 * A process represents a reference to an actor in Whiley.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Process extends Compound  {
		private Process(Node[] nodes) {
			super(nodes);
		}
		public Type element() {
			int elemIdx = (Integer) nodes[0].data;
			return extract(elemIdx);	
		}		
	}

	/**
	 * A dictionary represents a one-many mapping from variables of one type to
	 * variables of another type. For example, the dictionary type
	 * <code>int->real</code> represents a map from integers to real values. A
	 * valid instance of this type might be <code>{1->1.2,2->3}</code>.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Dictionary extends Compound  {
		private Dictionary(Node[] nodes) {
			super(nodes);
		}
		public Type key() {
			Pair<Integer,Integer> p = (Pair) nodes[0].data;
			return extract(p.first());
		}
		public Type value() {
			Pair<Integer,Integer> p = (Pair) nodes[0].data;
			return extract(p.second());			
		}
	}

	/**
	 * A record is made up of a number of fields, each of which has a unique
	 * name. Each field has a corresponding type. One can think of a record as a
	 * special kind of "fixed" dictionary (i.e. where we know exactly which
	 * entries we have).
	 * 
	 * @author djp
	 * 
	 */
	public static final class Record extends Compound  {
		private Record(Node[] nodes) {
			super(nodes);
		}

		/**
		 * Extract just the key set for this field. This is a cheaper operation
		 * than extracting the keys and their types (since types must be
		 * extracted).
		 * 
		 * @return
		 */
		public HashSet<String> keys() {
			Pair<String,Integer>[] fields = (Pair[]) nodes[0].data;
			HashSet<String> r = new HashSet<String>();
			for(Pair<String,Integer> f : fields) {
				r.add(f.first());
			}
			return r;
		}

		/**
		 * Return a mapping from field names to their types.
		 * 
		 * @return
		 */
		public HashMap<String,Type> fields() {
			Pair<String,Integer>[] fields = (Pair[]) nodes[0].data;
			HashMap<String,Type> r = new HashMap<String,Type>();
			for(Pair<String,Integer> f : fields) {
				r.put(f.first(),extract(f.second()));
			}
			return r;
		}
	}

	/**
	 * A union type represents a type whose variables may hold values from any
	 * of its "bounds". For example, the union type null|int indicates a
	 * variable can either hold an integer value, or null. <b>NOTE:</b>There
	 * must be at least two bounds for a union type to make sense.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Union extends Compound {
		private Union(Node[] nodes) {
			super(nodes);
		}

		/**
		 * Return the bounds of this union type.
		 * 
		 * @return
		 */
		public HashSet<Type> bounds() {			
			HashSet<Type> r = new HashSet<Type>();
			int[] fields = (int[]) nodes[0].data;
			for(int i : fields) {
				Type b = extract(i);					
				r.add(b);					
			}			
			return r;
		}
	}

	/**
	 * An intersection type represents a type which accepts values in the
	 * intersection of its bounds. <b>NOTE:</b>There must be at least two bounds
	 * for an intersection type to make sense.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Intersection extends Compound {
		private Intersection(Node[] nodes) {
			super(nodes);
		}

		/**
		 * Return the bounds of this union type.
		 * 
		 * @return
		 */
		public HashSet<Type> bounds() {			
			HashSet<Type> r = new HashSet<Type>();
			int[] fields = (int[]) nodes[0].data;
			for(int i : fields) {
				Type b = extract(i);					
				r.add(b);					
			}			
			return r;
		}
	}
	
	/**
	 * A difference type represents a type which accepts values in the
	 * difference between its bounds. 
	 * 
	 * @author djp
	 * 
	 */
	public static final class Difference extends Compound {
		private Difference(Node[] nodes) {
			super(nodes);
		}
		
		public Type left() {						
			int[] fields = (int[]) nodes[0].data;
			return extract(fields[0]);			
		}
		
		public Type right() {						
			int[] fields = (int[]) nodes[0].data;
			return extract(fields[1]);			
		}
	}
	
	/**
	 * A function type, consisting of a list of zero or more parameters and a
	 * return type.
	 * 
	 * @author djp
	 * 
	 */
	public static class Fun extends Compound  {
		Fun(Node[] nodes) {
			super(nodes);
		}

		/**
		 * Get the return type of this function type.
		 * 
		 * @return
		 */
		public Type ret() {						
			int[] fields = (int[]) nodes[0].data;			
			return extract(fields[1]);
		}		
		
		/**
		 * Get the parameter types of this function type.
		 * 
		 * @return
		 */
		public ArrayList<Type> params() {
			int[] fields = (int[]) nodes[0].data;
			ArrayList<Type> r = new ArrayList<Type>();
			for(int i=2;i<fields.length;++i) {
				r.add(extract(fields[i]));
			}
			return r;
		}
	}
	
	public static final class Meth extends Fun {
		Meth(Node[] nodes) {
			super(nodes);
		}
		/**
		 * Get the receiver type of this function type.
		 * 
		 * @return
		 */
		public Type.Process receiver() {
			int[] fields = (int[]) nodes[0].data;
			int r = fields[0];
			if(r == -1) { return null; }
			return (Type.Process) extract(r);
		}
	}
	
	// =============================================================
	// Components
	// =============================================================

	/**
	 * The construct methods constructs a Type from an array of Components.
	 * It carefully ensures the kind of the root node matches the class
	 * created (e.g. a kind K_SET results in a class Set).
	 * 
	 * @param nodes
	 * @return
	 */
	public final static Type construct(Node[] nodes) {
		Node root = nodes[0];
		switch(root.kind) {
		case K_VOID:
			return T_VOID;
		case K_ANY:
			return T_ANY;
		case K_META:
			return T_META;
		case K_NULL:
			return T_NULL;			
		case K_BOOL:
			return T_BOOL;
		case K_BYTE:
			return T_BYTE;
		case K_CHAR:
			return T_CHAR;
		case K_INT:
			return T_INT;
		case K_RATIONAL:
			return T_REAL;
		case K_STRING:
			return T_STRING;
		case K_TUPLE:
			return new Tuple(nodes);
		case K_SET:
			return new Set(nodes);
		case K_LIST:
			return new List(nodes);
		case K_EXISTENTIAL:
			if(root.data == null) {
				throw new RuntimeException("Problem");
			}
			return new Existential((NameID) root.data);
		case K_PROCESS:
			return new Process(nodes);
		case K_DICTIONARY:
			return new Dictionary(nodes);
		case K_RECORD:
			return new Record(nodes);
		case K_UNION:
			return new Union(nodes);
		case K_METHOD:
			return new Meth(nodes);
		case K_FUNCTION:
			return new Fun(nodes);		
		case K_LABEL:
			return T_LABEL((String)root.data);
		default:
			throw new IllegalArgumentException("invalid node kind: " + root.kind);
		}
	}
	
	private static final Node[] nodes(Type t) {
		if (t instanceof Leaf) {
			return new Node[]{new Node(Node.leafKind((Leaf) t), null)};
		} else {			
			// compound type
			return ((Compound)t).nodes;
		}
	}
	
	public static void main(String[] args) {				
		PrintBuilder printer = new PrintBuilder(System.out);	
		//Type t1 = contractive(); //linkedList(2);
		Type t1 = T_UNION(T_NULL,T_NULL);
		System.out.println("GOT: " + t1);
		System.out.println("MIN: " + minimise(t1));
		/*
		Type t2 = fromString("{int x,any y}");
		//Type t1 = T_REAL;
		//Type t2 = T_INT;
		System.out.println("Type: " + t1 + "\n------------------");
		build(printer,t1);		
		System.out.println("\nType: " + t2 + "\n------------------");
		build(printer,t2);		
		System.out.println("====================");
		System.out.println(isSubtype(t1,t2));
		System.out.println(isSubtype(t2,t1));
		Type glb = greatestLowerBound(t1,t2);
		System.out.println(glb);
		Type lub = leastUpperBound(t1,t2);
		System.out.println(lub);
		*/	
	}
	
	public static Type contractive() {
		Type lab = T_LABEL("Contractive");
		Type union = T_UNION(lab,lab);
		return T_RECURSIVE("Contractive", union);
	}
	
	public static Type linkedList(int n) {
		return T_RECURSIVE("X",innerLinkedList(n));
	}
	
	public static Type innerLinkedList(int n) {
		if(n == 0) {
			return T_LABEL("X");
		} else {
			Type leaf = T_PROCESS(innerLinkedList(n-1)); 
			HashMap<String,Type> fields = new HashMap<String,Type>();
			fields.put("next", T_UNION(T_NULL,leaf));
			fields.put("data", T_BOOL);
			Type.Record rec = T_RECORD(fields);
			return rec;
		}
	}
}
