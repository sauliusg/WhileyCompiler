import whiley.lang.*

function add(int x, int y) -> (int r)
requires x >= 0 && y >= 0
ensures r > 0:
    //
    if x == y:
        return 1
    else:
        return x + y

method main(System.Console sys) -> void:
    assume add(1,2) == 3
    assume add(1,1) == 1
