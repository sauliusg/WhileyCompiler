import whiley.lang.*

function f({int} xs, {int} ys) -> bool:
    if xs ⊆ ys:
        return true
    else:
        return false

method main(System.Console sys) -> void:
    assume f({1, 2, 3}, {1, 2, 3}) == true
    assume f({1, 4}, {1, 2, 3}) == false
    assume f({1}, {1, 2, 3}) == true
