import whiley.lang.*

type i8 is (int x) where x >= 0 && x <= 255
type bytes is {i8 b1, i8 b2}

function f(i8 b) -> bytes:
    return {b1: b, b2: 2}

method main(System.Console sys) -> void:
    i8 b = 1
    bytes bs = f(b)
    assume bs == {b1: 1, b2: 2}
    bs = {b1: b, b2: b}
    assume bs == {b1: 1, b2: 1}
