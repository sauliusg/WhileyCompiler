import println from whiley.lang.System

type wierd is [int] where some { x in $ | x > 0 }

function f([int] xs) => wierd
requires |xs| > 0:
    xs[0] = 1
    return xs

method main(System.Console sys) => void:
    rs = f([-1, -2])
    sys.out.println(Any.toString(rs))