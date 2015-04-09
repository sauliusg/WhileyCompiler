import whiley.lang.*

type bop is {int y, int x}

type expr is int | bop

function f(expr e) -> int:
    if e is bop:
        return e.x + e.y
    else:
        return e

method main(System.Console sys) -> void:
    int x = f(1)
    assume x == 1
    x = f({y: 10, x: 4})
    assume x == 14
