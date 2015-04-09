import whiley.lang.*

function f(real x) -> real:
    return x + 1

function g(function(int)->real func) -> real:
    return func(1)

method main(System.Console sys) -> void:
    assume g(&f) == 2.0
