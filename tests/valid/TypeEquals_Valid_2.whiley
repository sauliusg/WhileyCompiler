import whiley.lang.*

type expr is {int op, expr lhs} | {[int] err}

function f(expr e) -> int:
    if e is {[int] err}:
        return |e.err|
    else:
        return -1

method main(System.Console sys) -> void:
    int x = f({err: "Hello World"})
    assume x == 11
    x = f({op: 1, lhs: {err: "Gotcha"}})
    assume x == -1
