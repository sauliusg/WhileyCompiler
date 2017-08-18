package wyil.util.interpreter;

import java.util.Map;

import wybs.util.AbstractCompilationUnit.Identifier;

public abstract class LValue {
	abstract public RValue read(Map<Identifier, RValue> frame);
	abstract public void write(Map<Identifier, RValue> frame, RValue rhs);

	public static final class Variable extends LValue {
		private final Identifier name;

		public Variable(Identifier name) {
			this.name = name;
		}

		@Override
		public RValue read(Map<Identifier, RValue> frame) {
			return frame.get(name);
		}

		@Override
		public void write(Map<Identifier, RValue> frame, RValue rhs) {
			frame.put(name, rhs);
		}
	}

	public static class Array extends LValue {
		private final LValue src;
		private final RValue.Int index;

		public Array(LValue src, RValue.Int index) {
			this.src = src;
			this.index = index;
		}

		@Override
		public RValue read(Map<Identifier, RValue> frame) {
			RValue.Array src = Interpreter.checkType(this.src.read(frame), null, RValue.Array.class);
			return src.read(index);
		}

		@Override
		public void write(Map<Identifier, RValue> frame, RValue value) {
			RValue.Array arr = Interpreter.checkType(this.src.read(frame), null, RValue.Array.class);
			src.write(frame, arr.write(index, value));
		}
	}

	public static class Record extends LValue {
		private final LValue src;
		private final Identifier field;

		public Record(LValue src, Identifier field) {
			this.src = src;
			this.field = field;
		}

		@Override
		public RValue read(Map<Identifier, RValue> frame) {
			RValue.Record src = Interpreter.checkType(this.src.read(frame), null, RValue.Record.class);
			return src.read(field);
		}

		@Override
		public void write(Map<Identifier, RValue> frame, RValue value) {
			RValue.Record rec = Interpreter.checkType(this.src.read(frame), null, RValue.Record.class);
			src.write(frame, rec.write(field, value));
		}
	}

	public static class Dereference extends LValue {
		private final LValue src;

		public Dereference(LValue src) {
			this.src = src;
		}

		@Override
		public RValue read(Map<Identifier, RValue> frame) {
			RValue.Cell cell = Interpreter.checkType(src.read(frame), null, RValue.Cell.class);
			return cell.read();
		}

		@Override
		public void write(Map<Identifier, RValue> frame, RValue rhs) {
			RValue.Cell cell = Interpreter.checkType(src.read(frame), null, RValue.Cell.class);
			cell.write(rhs);
		}
	}
}