import whiley.lang.*

type wierd is ([int] xs) where some { x in xs | x > 0 }

function f([int] xs) -> wierd
requires |xs| > 0:
    xs[0] = 1
    return xs

method main(System.Console sys) -> void:
    assume f([-1, -2]) == [1,-2]
