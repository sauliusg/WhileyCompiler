// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyrl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import wyautl.core.*;
import wyautl.rw.Activation;
import wyautl.rw.RewriteRule;
import wyrl.core.Pattern;

public abstract class AbstractRewriteRule implements RewriteRule {

	/**
	 * The pattern that this rewrite rule will match against.
	 */
	private final Pattern pattern;

	/**
	 * The schema describes the automata that this rule will operate over.
	 */
	private final Schema schema;
	
	public AbstractRewriteRule(Pattern pattern, Schema schema) {
		this.schema = schema;
		this.pattern = pattern;
	}

	public final List<Activation> probe(Automaton automaton, int root) {
		// NOTE: +1 to include the root variable.
		Bindings bindings = new Bindings(pattern.nDeclarations() + 1);
		bindings.push(root);

		// First, we check whether or not this pattern accepts the given
		// automaton state. In the case that it does, we build an appropriate
		// activation record.
		if (accepts(pattern, automaton, root, bindings)) {
			return bindings.getActivations();
		} else {
			return null;
		}
	}

	/**
	 * Determine whether a pattern accepts a given automaton state and,
	 * furthermore, compute the binding(s) between variables declared in the
	 * pattern and states in the automaton. Multiple bindings can be returned
	 * because non-deterministic matching of collections (e.g. sets or bags)
	 * give rise to multiple possible ways a given state can be accepted.
	 * 
	 * @param pattern
	 *            The pattern being checked for accepting the given automaton
	 *            state.
	 * @param automaton
	 *            The automaton whose state(s) are being checked for acceptance.
	 * @param root
	 *            The state which is being checked for acceptance.
	 * @param bindings
	 *            The currently computed bindings.
	 * @return
	 */
	private final boolean accepts(Pattern pattern, Automaton automaton, int root,
			Bindings bindings) {
		if (pattern instanceof Pattern.Leaf) {
			Pattern.Leaf leaf = (Pattern.Leaf) pattern;
			return Runtime.accepts(leaf.type, automaton, automaton.get(root),
					schema);			
		} else if (pattern instanceof Pattern.Term) {
			return accepts((Pattern.Term) pattern, automaton, root, bindings);
		} else if (pattern instanceof Pattern.Set) {
			return accepts((Pattern.Set) pattern, automaton, root, bindings);
		} else if (pattern instanceof Pattern.Bag) {
			return accepts((Pattern.Bag) pattern, automaton, root, bindings);
		} else {
			return accepts((Pattern.List) pattern, automaton, root, bindings);
		}
	}

	private final boolean accepts(Pattern.Term p, Automaton automaton, int root,
			Bindings bindings) {
		Automaton.State state = automaton.get(root);
		if (state instanceof Automaton.Term) {
			Automaton.Term t = (Automaton.Term) state;
			String actualName = schema.get(t.kind).name;
			// Check term names match
			if (!p.name.equals(actualName)) {
				return false;
			}
			// Check contents matches
			if (p.data == null) {
				return t.contents == Automaton.K_VOID;
			} else if (accepts(p.data, automaton, t.contents, bindings)) {
				if(p.variable != null) {
					// At this point, we need to store the root of the match as this
					// will feed into the activation record.
					bindings.push(t.contents);
				}
				return true;
			}
		}
		return false;
	}

	private final boolean accepts(Pattern.BagOrSet p, Automaton automaton,
			int root, Bindings bindings) {
		Automaton.State state = automaton.get(root);
		
		if (p instanceof Pattern.Set && !(state instanceof Automaton.Set)) {
			return false;
		} else if (p instanceof Pattern.Bag && !(state instanceof Automaton.Bag)) {
			return false;
		} 
		
		Automaton.Collection c = (Automaton.Collection) state;
		Pair<Pattern,String>[] p_elements = p.elements;
		int minSize = p.unbounded ? p_elements.length - 1 : p_elements.length;
		
		// First, check size of collection		
		if(!p.unbounded && c.size() != minSize || c.size() < minSize) {
			return false;
		}
		
		BitSet matched = new BitSet(automaton.nStates());
		return nonDeterministicAccept(c,automaton,p,0,matched,bindings);
	}

	/**
	 * Implements the non-deterministic matching required for set and bag
	 * patterns. For example, consider:
	 * 
	 * <pre>
	 * reduce And{BExpr b, Not(BExpr) nb, BExpr... xs}}:
	 *     => ...
	 * </pre>
	 * 
	 * <p>
	 * Here, we are non-deterministically choosing <code>b</code> and
	 * <code>nb</code> from the children of the root state. In this case, we
	 * have two match elements (namely, for <code>b</code> and <code>nb</code>),
	 * along with a generic match for the rest. For each element, we consider
	 * every possible child which has not already been matched (i.e. so that
	 * <code>b<code> and <code>nb</code> don't match the same state).
	 * </p>
	 * <p>
	 * This method recursively matches the elements of such a pattern. At each
	 * invocation, a single element is matched by considering all remaining
	 * possibilities. As soon as we fail to make a match, the method returns
	 * immediately; otherwise it proceeds to try and (recursively) match all
	 * remaining elements.
	 * </p>
	 * 
	 * @param children
	 *            The child states being matched against.
     * @param automaton
	 *            The automaton which is being matched against.
	 * @param pattern
	 *            The pattern we're attempting to match.
	 * @param elementIndex
	 *            The current element from the pattern elements array that we
	 *            are matching. Initially, this starts at zero and is
	 *            incremented at each level of the match.
	 * @param matched
	 *            A bitset indicating those states which are already matched and
	 *            should not be further considered.
	 * @param bindings
	 *            The set of current bindings being explored.
	 * @return
	 */
	private final boolean nonDeterministicAccept(Automaton.Collection children,
			Automaton automaton, Pattern.BagOrSet pattern, int elementIndex,
			BitSet matched, Bindings bindings) {
		Pair<Pattern, String>[] elements = pattern.elements;
		int resetPoint = bindings.size();
		
		if(elementIndex != elements.length) {
			// At least one pattern element remains to be matched, so
			// attempt to match it.
			boolean found = false;
			Pair<Pattern, String> pItem = elements[elementIndex];
			Pattern pItem_first = pItem.first();
			String pItem_second = pItem.second();

			for (int i = 0; i != children.size(); ++i) {
				if (matched.get(i)) {
					continue;
				}
				int aItem = children.get(i);
				if (accepts(pItem_first, automaton, aItem, bindings)) {
					matched.set(i, true);
					if (nonDeterministicAccept(children, automaton, pattern,
							elementIndex + 1, matched, bindings)) {
						found = true;
						if (pItem_second != null) {
							bindings.push(aItem);
						}
					}
					matched.set(i, false);
				}
			}

			return found;
		} else  {
			// Matching elements complete complete. Now attempt to match
			// remainder (where appropriate).
			if (pattern.unbounded) {
				int minSize = elements.length - 1;
				Pair<Pattern, String> pItem = elements[elements.length];
				Pattern pItem_first = pItem.first();
				String pItem_second = pItem.second();
				// First, we check whether or not this will succeed.
				for (int j = 0; j != children.size(); ++j) {
					if (matched.get(j)) {
						continue;
					}
					int aItem = children.get(j);
					if (!accepts(pItem_first, automaton, aItem, bindings)) {
						return false;
					}
				}
				// Second, we construct a match array (if one is required).
				if (pItem_second != null) {
					// In this case, we have a named variable into which we need
					// to store the matched elements.
					int[] nChildren = new int[children.size() - minSize];
					for (int i = 0, j = 0; i != children.size(); ++i) {
						if (matched.get(i)) {
							continue;
						}
						nChildren[j++] = children.get(i);
					}
					if (pattern instanceof Pattern.Set) {
						assign(count++, new Automaton.Set(nChildren), bindings);
					} else {
						assign(count++, new Automaton.Bag(nChildren), bindings);
					}
				}
			}
			// If we're get here, then we've completed the match.
			bindings.add(Arrays.copyOf(state, state.length));
			return true;
		} 
	}
	
	private final boolean accepts(Pattern.List p, Automaton automaton,
			int root, Bindings bindings) {
		int startCount = count;
		Automaton.Collection c = (Automaton.Collection) automaton.get(root);
		Pair<Pattern, String>[] p_elements = p.elements;
		int minSize = p.unbounded ? p_elements.length - 1 : p_elements.length;

		// First, check size of collection
		if (!p.unbounded && c.size() != minSize || c.size() < minSize) {
			return false;
		}

		// Second, we need to try and match the elements.
		for (int i = 0; i != minSize; ++i) {
			Pair<Pattern, String> pItem = p_elements[i];
			Pattern pItem_first = pItem.first();
			String pItem_second = pItem.second();
			int aItem = c.get(i);
			if (!accepts(pItem_first, automaton, aItem, bindings)) {
				count = startCount; // reset
				return false;
			} else if (pItem_second != null) {
				assign(count++, aItem, bindings);
			}
		}

		// Third, in the case of an unbounded match we check the remainder.
		if (p.unbounded) {
			Pair<Pattern, String> pItem = p_elements[minSize];
			Pattern pItem_first = pItem.first();
			String pItem_second = pItem.second();
			for (int i = minSize; i != c.size(); ++i) {
				int aItem = c.get(i);
				if (!accepts(pItem_first, automaton, aItem, bindings)) {
					count = startCount; // reset
					return false;
				}
			}
			if (pItem_second != null) {
				int[] children = new int[c.size() - minSize];
				for (int i = minSize; i != c.size(); ++i) {
					children[i] = c.get(i);
				}
				assign(count++, new Automaton.List(children), bindings);
			}
		}

		return true;
	}
	
	public static final void assign(int state, Object value, ArrayList<Object[]> states) {
		
	}
	
	private final class Bindings {
		private final ArrayList<Object[]> bindings;
		private int count;
		
		public Bindings(int nVariables) {
			Object[] initialState = new Object[nVariables];
			this.bindings = new ArrayList<Object[]>();		
			this.bindings.add(initialState);
		}
		
		public final void push(Object value) {
			for (int i = 0; i != bindings.size(); ++i) {
				Object[] binding = bindings.get(i);
				binding[count] = value;
			}
			count++;
		}
		
		public final int size() {
			return count;
		}
		
		public final List<Activation> getActivations() {
			ArrayList<Activation> tmp = new ArrayList<Activation>();
			for (int i = 0; i != tmp.size(); ++i) {
				Object[] binding = bindings.get(i);
				tmp.add(new Activation(AbstractRewriteRule.this, null, binding));
			}
			return tmp;
		}
	}
}
