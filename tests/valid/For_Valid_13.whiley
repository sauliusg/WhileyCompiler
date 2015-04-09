import whiley.lang.*

type listsetdict is [int] | {int} | {int=>int}

function f(listsetdict ls) -> int:
    int r = 0
    for l in ls:
        r = r + 1
    return r

method main(System.Console sys) -> void:
    {int} ls = {1, 2, 3, 4, 5}
    assume f(ls) == 5
    [int] xs = [1, 2, 3, 4, 5, 6, 7, 8]
    assume f(xs) == 8
    {int=>int} ms = {10=>20, 30=>40}
    assume f(ms) == 2
