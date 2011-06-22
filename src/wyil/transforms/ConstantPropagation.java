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

package wyil.transforms;

import static wyil.util.SyntaxError.syntaxError;

import java.math.BigInteger;
import java.util.*;

import wyil.ModuleLoader;
import wyil.lang.*;
import wyil.lang.Block.Entry;
import wyil.lang.Code.*;
import wyil.lang.Code.SubList;
import wyil.transforms.TypePropagation.Env;
import wyil.util.*;
import wyil.util.dfa.ForwardFlowAnalysis;
import wyjc.runtime.BigRational;

public class ConstantPropagation extends ForwardFlowAnalysis<ConstantPropagation.Env> {	
	private static final HashMap<Integer,Rewrite> rewrites = new HashMap<Integer,Rewrite>();
	
	public ConstantPropagation(ModuleLoader loader) {
		super(loader);
	}
	
	public Module.TypeDef transform(Module.TypeDef type) {		
		return type;		
	}
	
	public Env initialStore() {				
		Env environment = new Env();		
		int nvars = methodCase.locals().size();
		
		for (int i=0; i != nvars; ++i) {			
			environment.add(null);			
		}				
					
		return environment;				
	}
	
	public Module.Case propagate(Module.Case mcase) {		
		methodCase = mcase;
		stores = new HashMap<String,Env>();
		rewrites.clear();
		
		Env environment = initialStore();		
		propagate(0,mcase.body().size(), environment);	
		
		// At this point, we apply the inserts
		Block body = mcase.body();
		Block nbody = new Block();		
		for(int i=0;i!=body.size();++i) {
			Rewrite rewrite = rewrites.get(i);			
			if(rewrite != null) {				
				for(int j=0;j!=rewrite.stackArgs;++j) {
					 nbody.remove(nbody.size()-1);
				}				
				nbody.add(rewrite.rewrite);				
			} else {				
				nbody.add(body.get(i));
			}
		}
		
		return new Module.Case(nbody,mcase.locals(),mcase.attributes());
	}
	
	/*
	
	protected Block unrollFor(Code.ForAll fall, Block body) {		
		Block blk = new Block();
		Collection<Value> values;
		if(fall.source instanceof Value.List) {
			Value.List l = (Value.List) fall.source;
			values = l.values;
		} else {
			Value.Set s = (Value.Set) fall.source;
			values = s.values;
		}
		HashMap<String,CExpr> binding = new HashMap<String,CExpr>();
		String var = fall.variable.name();
		for(Value v : values) {
			// first, relabel to avoid conflicts
			Block tmp = Block.relabel(body);
			// second, substitute value
			binding.put(var, v);			
			tmp = Block.substitute(binding,tmp);			
			// finally,add to the target blk
			blk.addAll(tmp);
		}
		return blk;
	}		
	
	*/
	

	public Env propagate(int index, Entry entry, Env environment) {						
		Code code = entry.code;			
		
		// reset the rewrites for this code, in case it changes
		rewrites.remove(index);
		
		environment = (Env) environment.clone();
		
		if(code instanceof Assert) {
			infer((Assert)code,entry,environment);
		} else if(code instanceof BinOp) {
			infer(index,(BinOp)code,entry,environment);
		} else if(code instanceof Convert) {
			infer(index,(Convert)code,entry,environment);
		} else if(code instanceof Const) {
			infer((Const)code,entry,environment);
		} else if(code instanceof Debug) {
			infer((Debug)code,entry,environment);
		} else if(code instanceof DictLoad) {
			infer(index,(DictLoad)code,entry,environment);
		} else if(code instanceof ExternJvm) {
			// skip
		} else if(code instanceof Fail) {
			// skip
		} else if(code instanceof FieldLoad) {
			infer(index,(FieldLoad)code,entry,environment);			
		} else if(code instanceof IndirectInvoke) {
			infer((IndirectInvoke)code,entry,environment);
		} else if(code instanceof IndirectSend) {
			infer((IndirectSend)code,entry,environment);
		} else if(code instanceof Invoke) {
			infer((Invoke)code,entry,environment);
		} else if(code instanceof Label) {
			// skip			
		} else if(code instanceof ListAppend) {
			infer(index,(ListAppend)code,entry,environment);
		} else if(code instanceof ListLength) {
			infer(index,(ListLength)code,entry,environment);
		} else if(code instanceof SubList) {
			infer(index,(SubList)code,entry,environment);
		} else if(code instanceof ListLoad) {
			infer(index,(ListLoad)code,entry,environment);
		} else if(code instanceof Load) {
			infer(index,(Load)code,entry,environment);
		} else if(code instanceof MultiStore) {
			infer((MultiStore)code,entry,environment);
		} else if(code instanceof NewDict) {
			infer(index,(NewDict)code,entry,environment);
		} else if(code instanceof NewList) {
			infer(index,(NewList)code,entry,environment);
		} else if(code instanceof NewRecord) {
			infer(index,(NewRecord)code,entry,environment);
		} else if(code instanceof NewSet) {
			infer(index,(NewSet)code,entry,environment);
		} else if(code instanceof Return) {
			infer((Return)code,entry,environment);
		} else if(code instanceof Send) {
			infer((Send)code,entry,environment);
		} else if(code instanceof Store) {
			infer((Store)code,entry,environment);
		} else if(code instanceof SetOp) {
			infer(index,(SetOp)code,entry,environment);
		} else if(code instanceof StringAppend) {
			infer(index,(StringAppend)code,entry,environment);
		} else if(code instanceof StringLength) {
			infer(index,(StringLength)code,entry,environment);
		} else if(code instanceof StringLoad) {
			infer(index,(StringLoad)code,entry,environment);
		} else if(code instanceof SubString) {
			infer(index,(SubString)code,entry,environment);
		} else if(code instanceof Skip) {
			// skip			
		} else if(code instanceof UnOp) {
			infer(index,(UnOp)code,entry,environment);
		} else {
			syntaxError("unknown wyil code encountered: " + code,filename,entry);
			return null;
		}	
		
		return environment;
	}
	
	public void infer(Code.Assert code, Block.Entry entry,
			Env environment) {
		
	}
	
	public void infer(int index, Code.BinOp code, Block.Entry entry,
			Env environment) {
		Value rhs = environment.pop();
		Value lhs = environment.pop();
		Value result = null;
		
		if(lhs instanceof Value.Number && rhs instanceof Value.Number) {
			Value.Number lnum = (Value.Number) lhs;
			Value.Number rnum = (Value.Number) rhs;
			
			switch (code.bop) {
			case ADD: {
				result = lnum.add(rnum);
				break;
			}
			case SUB: {
				result = lnum.subtract(rnum);
				break;
			}
			case MUL: {
				result = lnum.multiply(rnum);
				break;
			}
			case DIV: {								
				result = lnum.divide(rnum);				
				break;
			}	
			case REM: {				
				result = lnum.intRemainder(rnum);				
				break;
			}	
			}		
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,2));
		} 
		
		environment.push(result);
	}
	
	public void infer(int index, Code.Convert code, Block.Entry entry,
			Env environment) {
		Value val = environment.pop();
		
		if (val instanceof Value.Number && code.from == Type.T_INT
				&& code.to == Type.T_REAL) {			
			entry = new Block.Entry(Code.Const(val),entry.attributes());
			rewrites.put(index, new Rewrite(entry,1));					
		} else {			
			// need to apply other conversions here
			val = null;
		}
		
		environment.push(val);
	}
	
	public void infer(Code.Const code, Block.Entry entry,
			Env environment) {
		environment.push(code.constant);		
	}
	
	public void infer(Code.Debug code, Block.Entry entry,
			Env environment) {
		environment.pop();
	}
	
	public void infer(int index, Code.DictLoad code, Block.Entry entry,
			Env environment) {
		Value key = environment.pop();
		Value src = environment.pop();
		Value result = null;
		
		if (key instanceof Value && src instanceof Value.Dictionary) {			
			Value.Dictionary dict = (Value.Dictionary) src;			
			if(dict.values.containsKey(key)) {				
				result = dict.values.get(key);
				entry = new Block.Entry(Code.Const(result),entry.attributes());
				rewrites.put(index, new Rewrite(entry,2));				
			}			
		} 
		
		environment.push(result);		
	}
	
	public void infer(int index, Code.FieldLoad code, Block.Entry entry,
			Env environment) {
		Value src = environment.pop();
		Value result = null;
		if(src instanceof Value.Record) {
			Value.Record rec = (Value.Record) src;			
			result = rec.values.get(code.field);			
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,1));
		} 
		
		environment.push(result);		
	}
	
	public void infer(Code.IndirectInvoke code, Block.Entry entry,
			Env environment) {
		
		// TODO: in principle we can do better here in the case that the target
		// is a constant. This seems pretty unlikely though ...
		
		for(int i=0;i!=code.type.params().size();++i) {
			environment.pop();
		}
		
		environment.pop(); // target
		
		if(code.type.ret() != Type.T_VOID && code.retval) {
			environment.push(null);
		}
	}
	
	public void infer(Code.IndirectSend code, Block.Entry entry,
			Env environment) {
		// FIXME: need to do something here
	}
	
	public void infer(Code.Invoke code, Block.Entry entry,
			Env environment) {
		
		// TODO: in the case of a function call (rather than an internal message
		// send), we could potentially evaluate the function in question to give
		// a constant value.
		
		for(int i=0;i!=code.type.params().size();++i) {
			environment.pop();
		}
		
		if(code.type.ret() != Type.T_VOID && code.retval) {
			environment.push(null);
		}
	}
	
	public void infer(int index, Code.ListAppend code, Block.Entry entry,
			Env environment) {
		Value rhs = environment.pop();
		Value lhs = environment.pop();
		Value result = null;
		if(code.dir == OpDir.UNIFORM && lhs instanceof Value.List && rhs instanceof Value.List) {
			Value.List left = (Value.List) lhs;
			Value.List right = (Value.List) rhs;
			ArrayList<Value> values = new ArrayList<Value>(left.values);
			values.addAll(right.values);
			result = Value.V_LIST(values);
		} else if(code.dir == OpDir.LEFT && lhs instanceof Value.List && rhs instanceof Value) {
			Value.List left = (Value.List) lhs;
			Value right = (Value) rhs;
			ArrayList<Value> values = new ArrayList<Value>(left.values);
			values.add(right);
			result = Value.V_LIST(values);
		} else if(code.dir == OpDir.RIGHT && lhs instanceof Value && rhs instanceof Value.List) {
			Value left = (Value) lhs;
			Value.List right = (Value.List) rhs;
			ArrayList<Value> values = new ArrayList<Value>();
			values.add(left);
			values.addAll(right.values);
			result = Value.V_LIST(values);
		} 
		
		if(result != null) {
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,2));
		}
		environment.push(result);
	}
	
	public void infer(int index, Code.ListLength code, Block.Entry entry,
			Env environment) {
		Value val = environment.pop();
		Value result = null;
		
		if(val instanceof Value.List) {
			Value.List list = (Value.List) val;
			result = Value.V_NUMBER(BigInteger.valueOf(list.values.size()));
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,1));
		} 
		
		environment.push(result);
	}
	
	public void infer(int index, Code.SubList code, Block.Entry entry,
			Env environment) {
		Value end = environment.pop();
		Value start = environment.pop();
		Value list = environment.pop();
		Value result = null;
		if (list instanceof Value.List && start instanceof Value.Number
				&& end instanceof Value.Number) {
			Value.Number en = (Value.Number) end;
			Value.Number st = (Value.Number) start;
			if (en.value.isInteger() && st.value.isInteger()) {
				Value.List li = (Value.List) list;
				int eni = en.value.intValue();
				int sti = st.value.intValue();
				if (BigRational.valueOf(eni).equals(en.value)
						&& eni >= 0 && eni <= li.values.size()
						&& BigRational.valueOf(sti).equals(st.value)
						&& sti >= 0 && sti <= li.values.size()) {
					ArrayList<Value> nvals = new ArrayList<Value>();
					for (int i = sti; i < eni; ++i) {
						nvals.add(li.values.get(i));
					}
					result = Value.V_LIST(nvals);
					entry = new Block.Entry(Code.Const(result),entry.attributes());
					rewrites.put(index, new Rewrite(entry,3));
				}
			}
		} 
		environment.push(result);
	}
	
	public void infer(int index, Code.ListLoad code, Block.Entry entry,
			Env environment) {
		Value idx = environment.pop();
		Value src = environment.pop();
		Value result = null;
		if (idx instanceof Value.Number && src instanceof Value.List) {
			Value.Number num = (Value.Number) idx;
			Value.List list = (Value.List) src;
			if(num.value.isInteger()) {
				int i = num.value.intValue();
				if (BigRational.valueOf(i).equals(num.value) && i >= 0
						&& i < list.values.size()) {
					result = list.values.get(i);
					entry = new Block.Entry(Code.Const(result),entry.attributes());
					rewrites.put(index, new Rewrite(entry,2));
				}
			}			
		} 
		
		environment.push(result);		
	}
	
	public void infer(int index, Code.Load code, Block.Entry entry,
			Env environment) {
		
		Value val = environment.get(code.slot);
		if(val instanceof Value) {
			// register rewrite
			entry = new Block.Entry(Code.Const(val), entry.attributes());					
			rewrites.put(index, new Rewrite(entry,0));
		} 
		
		environment.push(val);
	}
	
	public void infer(Code.MultiStore code, Block.Entry entry,
			Env environment) {
		
		// TO DO: I could definite do more here
		
		int npop = code.level - code.fields.size();
		for(int i=0;i!=npop;++i) {
			environment.pop();
		}
		
		environment.set(code.slot,null);
	}
	
	public void infer(int index, Code.NewDict code, Block.Entry entry,
			Env environment) {
		HashMap<Value,Value> values = new HashMap<Value,Value>();
		boolean isValue = true;
		for(int i=0;i!=code.nargs;++i) {
			Value val = environment.pop();
			Value key = environment.pop();
			if(key instanceof Value && val instanceof Value) {
				values.put(key, val);
			} else {
				isValue=false;
			}
		}
		Value result = null;
		if(isValue) {
			result = Value.V_DICTIONARY(values);			
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,code.nargs));			
		}
		environment.push(result);		
	}
	
	public void infer(int index, Code.NewRecord code, Block.Entry entry,
			Env environment) {
		HashMap<String, Value> values = new HashMap<String, Value>();
		ArrayList<String> keys = new ArrayList<String>(code.type.keys());
		Collections.sort(keys);
		Collections.reverse(keys);
		boolean isValue = true;
		for (String key : keys) {
			Value val = environment.pop();
			if (val instanceof Value) {
				values.put(key, val);
			} else {
				isValue = false;
			}
		}

		Value result = null;
		if (isValue) {
			result = Value.V_RECORD(values);
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,keys.size()));
		}
		environment.push(result);
	}
	
	public void infer(int index, Code.NewList code, Block.Entry entry,
			Env environment) {
		ArrayList<Value> values = new ArrayList<Value>();		

		boolean isValue = true;
		for (int i=0;i!=code.nargs;++i) {
			Value val = environment.pop();
			if (val instanceof Value) {
				values.add(val);
			} else {
				isValue = false;
			}
		}		
		
		Value result = null;
		if (isValue) {
			Collections.reverse(values);
			result = Value.V_LIST(values);
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,code.nargs));
		}
		environment.push(result);
	}
	
	public void infer(int index, Code.NewSet code, Block.Entry entry,
			Env environment) {
		HashSet<Value> values = new HashSet<Value>();		

		boolean isValue = true;
		for (int i=0;i!=code.nargs;++i) {
			Value val = environment.pop();
			if (val instanceof Value) {
				values.add(val);
			} else {
				isValue = false;
			}
		}		
		
		Value result = null;
		if (isValue) {			
			result = Value.V_SET(values);
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,code.nargs));
		}
		environment.push(result);
	}
	
	public void infer(Code.Return code, Block.Entry entry,
			Env environment) {
		if(code.type != Type.T_VOID) {
			environment.pop();
		}
	}
	
	public void infer(Code.Send code, Block.Entry entry,
			Env environment) {

		for(int i=0;i!=code.type.params().size();++i) {
			environment.pop();
		}
		
		environment.pop(); // receiver
		
		if (code.type.ret() != Type.T_VOID && code.synchronous && code.retval) {
			environment.push(null);
		}
	}
	
	public void infer(Code.Store code, Block.Entry entry,
			Env environment) {
		environment.set(code.slot, environment.pop());
	}
	
	public void infer(int index, Code.SetOp code, Block.Entry entry,
			Env environment) {						
		Value result = null;
		int nops = 2;
		
		switch (code.sop) {
		case UNION: {
			Value rhs = environment.pop();
			Value lhs = environment.pop();
			if (code.dir == OpDir.UNIFORM && lhs instanceof Value.Set
					&& rhs instanceof Value.Set) {
				Value.Set lv = (Value.Set) lhs;
				Value.Set rv = (Value.Set) rhs;
				result = lv.union(rv);
			} else if(code.dir == OpDir.LEFT && lhs instanceof Value.Set
					&& rhs instanceof Value) {
				Value.Set lv = (Value.Set) lhs;
				Value rv = (Value) rhs;
				result = lv.add(rv);
			} else if(code.dir == OpDir.RIGHT && lhs instanceof Value
					&& rhs instanceof Value.Set) {
				Value lv = (Value) lhs;
				Value.Set rv = (Value.Set) rhs;
				result = rv.add(lv);
			} 
			break;
		}
		case DIFFERENCE: {
			Value rhs = environment.pop();
			Value lhs = environment.pop();
			if (code.dir == OpDir.UNIFORM && lhs instanceof Value.Set
					&& rhs instanceof Value.Set) {
				Value.Set lv = (Value.Set) lhs;
				Value.Set rv = (Value.Set) rhs;
				result = lv.difference(rv);
			} else if(code.dir == OpDir.LEFT && lhs instanceof Value.Set
					&& rhs instanceof Value) {
				Value.Set lv = (Value.Set) lhs;
				Value rv = (Value) rhs;
				result = lv.remove(rv);
			} else if(code.dir == OpDir.RIGHT && lhs instanceof Value
					&& rhs instanceof Value.Set) {
				Value lv = (Value) lhs;
				Value.Set rv = (Value.Set) rhs;
				result = rv.remove(lv);
			} 
			break;
		}
		case INTERSECT: {
			Value rhs = environment.pop();
			Value lhs = environment.pop();
			if (code.dir == OpDir.UNIFORM && lhs instanceof Value.Set
					&& rhs instanceof Value.Set) {
				Value.Set lv = (Value.Set) lhs;
				Value.Set rv = (Value.Set) rhs;
				result = lv.intersect(rv);
			} else if(code.dir == OpDir.LEFT && lhs instanceof Value.Set
					&& rhs instanceof Value) {
				Value.Set lv = (Value.Set) lhs;
				Value rv = (Value) rhs;
				if(lv.values.contains(rv)) {
					HashSet<Value> nset = new HashSet<Value>();
					nset.add(rv);
					result = Value.V_SET(nset);
				} else {
					result = Value.V_SET(Collections.EMPTY_SET);
				}
			} else if(code.dir == OpDir.RIGHT && lhs instanceof Value
					&& rhs instanceof Value.Set) {
				Value lv = (Value) lhs;
				Value.Set rv = (Value.Set) rhs;
				if(rv.values.contains(lv)) {
					HashSet<Value> nset = new HashSet<Value>();
					nset.add(lv);
					result = Value.V_SET(nset);
				} else {
					result = Value.V_SET(Collections.EMPTY_SET);
				}
			} 
			break;
		}
		case LENGTHOF:
		{
			Value val = environment.pop();
			
			if(val instanceof Value.Set) {
				Value.Set set = (Value.Set) val;
				result = Value.V_NUMBER(BigInteger.valueOf(set.values.size()));
				nops = 1;
			} 
		}
		}
		
		if(result != null) {
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,nops));
		}
		
		environment.push(result);
	}
	
	public void infer(int index, Code.StringAppend code, Block.Entry entry,
			Env environment) {
		Value rhs = environment.pop();
		Value lhs = environment.pop();
		Value result = null;
		if(code.dir == OpDir.UNIFORM && lhs instanceof Value.Strung && rhs instanceof Value.Strung) {
			Value.Strung left = (Value.Strung) lhs;
			Value.Strung right = (Value.Strung) rhs;
			result = Value.V_STRING(left.value + right.value);
		} else if(code.dir == OpDir.LEFT && lhs instanceof Value.Strung && rhs instanceof Value) {
			// TODO: need to add Value.Char
		} else if(code.dir == OpDir.RIGHT && lhs instanceof Value && rhs instanceof Value.Strung) {
			// TODO: need to add Value.Char					
		} 
		
		if(result != null) {
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,2));
		}
		environment.push(result);	
	}
	
	public void infer(int index, Code.StringLoad code, Block.Entry entry,
			Env environment) {
		Value source = environment.pop();
		Value idx = environment.pop();
		Value result = null;
		
		if(source instanceof Value.Strung && idx instanceof Value.Number) {
			Value.Strung str = (Value.Strung) source;
			Value.Number num = (Value.Number) idx;			
			if (num.value.isInteger()) {
				int i = num.value.intValue();
				if(i >=0 && i < str.value.length()) {
					// TO DO: need to actually push a character here
				}
			}
		}
		
		environment.push(result);
	}
	
	public void infer(int index, Code.StringLength code, Block.Entry entry,
			Env environment) {
		Value val = environment.pop();
		Value result = null;
		
		if(val instanceof Value.Strung) {
			Value.Strung str = (Value.Strung) val;
			result = Value.V_NUMBER(BigInteger.valueOf(str.value.length()));
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,1));
		} 				
	}
	
	public void infer(int index, Code.SubString code, Block.Entry entry,
			Env environment) {
		Value end = environment.pop();
		Value start = environment.pop();
		Value src = environment.pop();
		Value result = null;
		if (src instanceof Value.Strung && start instanceof Value.Number
				&& end instanceof Value.Number) {
			Value.Number en = (Value.Number) end;
			Value.Number st = (Value.Number) start;
			if (en.value.isInteger() && st.value.isInteger()) {
				Value.Strung str = (Value.Strung) src;
				int eni = en.value.intValue();
				int sti = st.value.intValue();
				if (BigRational.valueOf(eni).equals(en.value)
						&& eni >= 0 && eni <= str.value.length()
						&& BigRational.valueOf(sti).equals(st.value)
						&& sti >= 0 && sti <= str.value.length()) {
					String nval = "";							
					for (int i = sti; i < eni; ++i) {
						nval += str.value.charAt(i);
					}
					result = Value.V_STRING(nval);
					entry = new Block.Entry(Code.Const(result),entry.attributes());
					rewrites.put(index, new Rewrite(entry,3));
				}
			}
		} 
		environment.push(result);
	}
	
	public void infer(int index, Code.UnOp code, Block.Entry entry,
			Env environment) {
		Value val = environment.pop();
		Value result = null;
		switch(code.uop) {
			case NEG:
			{
				if(val instanceof Value.Number) {
					Value.Number num = (Value.Number) val;
					result = Value.V_NUMBER(num.value.negate());
				} 
			}
			break;			
		}
		
		if(result != null) {
			entry = new Block.Entry(Code.Const(result),entry.attributes());
			rewrites.put(index, new Rewrite(entry,1));
		}
		
		environment.push(result);
	}
	
	public Pair<Env, Env> propagate(int index,
			Code.IfGoto igoto, Entry stmt, Env environment) {
		
		Value rhs = environment.pop();
		Value lhs = environment.pop();
		
		// TODO: could do more here to eliminate conditionals which must either
		// be taken or untaken.
		
		return new Pair(environment, environment);
	}
	
	public Pair<Env, Env> propagate(int index,
			Code.IfType code, Entry stmt, Env environment) {
		
		if(code.slot < 0) {			
			Value lhs = environment.pop();			
		} 
		
		return new Pair(environment, environment);
	}
	
	public List<Env> propagate(int index, Code.Switch sw,
			Entry stmt, Env environment) {
		
		Value val = environment.pop();
		
		ArrayList<Env> stores = new ArrayList();
		for (int i = 0; i != sw.branches.size(); ++i) {
			stores.add(environment);
		}
		
		return stores;
	}
		
	public Env propagate(int start, int end, Code.Loop loop,
			Entry stmt, Env environment) {
		
		environment = new Env(environment);
		
		if(loop instanceof Code.ForAll) {
			Code.ForAll fall = (Code.ForAll) loop; 
			environment.pop();		
			
			// TO DO: could unroll loop if src collection is a value.
			
			environment.set(fall.slot,null);
		} 
		
		// Now, kill every variable which is modified in the loop. This is a
		// safety precaution, and it's possible we could do better here in some
		// circumstances (e.g. by unrolling the loop).
		
		for(int slot : loop.modifies) {
			environment.set(slot,null);
		}
		
		Env oldEnv = null;
		Env newEnv = null;
		
		do {			
			// iterate until a fixed point reached
			oldEnv = newEnv != null ? newEnv : environment;
			newEnv = propagate(start+1,end, oldEnv);
			
		} while (!newEnv.equals(oldEnv));

		return join(environment,newEnv);		
	}
	
	public Env join(Env env1, Env env2) {
		if (env2 == null) {
			return env1;
		} else if (env1 == null) {
			return env2;
		}
		Env env = new Env();
		for (int i = 0; i != Math.min(env1.size(), env2.size()); ++i) {
			Value mt = env1.get(i);
			Value ot = env2.get(i);
			if (ot instanceof Value && mt instanceof Value && ot.equals(mt)) {
				env.add(mt);
			} else {
				env.add(null);
			}			
		}

		return env;
	}	
	
	public static class Env extends ArrayList<Value> {
		public Env() {
		}
		public Env(Collection<Value> v) {
			super(v);
		}
		public void push(Value t) {
			add(t);
		}
		public Value top() {
			return get(size()-1);
		}
		public Value pop() {
			return remove(size()-1);			
		}
		public Env clone() {
			return new Env(this);
		}
	}
	

	private static class Rewrite {		
		public final Block.Entry rewrite;
		public final int stackArgs;		
		
		public Rewrite(Block.Entry rewrite, int stackArgs) {
			this.rewrite = rewrite;
			this.stackArgs = stackArgs;
		}
	}
}
