import whiley.lang.*

function f({int} xs) -> {int}:
    return xs

function g({int} ys) -> {int}:
    return f(ys & {1, 2})

method main(System.Console sys) -> void:
    assume g({}) == {}
    assume g({2, 3, 4, 5, 6}) == {2}
    assume g({2, 6}) == {2}
