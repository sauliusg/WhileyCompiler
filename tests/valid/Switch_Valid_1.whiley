import whiley.lang.*

type nat is (int n) where n >= 0

function f(int x) -> nat:
    switch x:
        case 1:
            return x - 1
        case -1:
            return x + 1
    return 1

method main(System.Console sys) -> void:
    assume f(2) == 1
    assume f(1) == 0
    assume f(0) == 1
    assume f(-1) == 0
    assume f(-2) == 1
