import println from whiley.lang.System

type sr5nat is int

method main(System.Console sys) => void:
    x = {f: 1}
    x.f = 2
    sys.out.println(Any.toString(x))