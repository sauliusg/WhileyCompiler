import whiley.lang.*

type ir1nat is (int x) where x > 0

type pir1nat is (ir1nat x) where x > 1

function f(int x) -> int:
    if x > 2:
        int y = x
        return y
    return 0

method main(System.Console sys) -> void:
    assume f(1) == 0
    assume f(2) == 0
    assume f(3) == 3
