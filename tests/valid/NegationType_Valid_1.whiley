function f(int[]|int x) -> !null:
    if x is int:
        return 1
    else:
        return x

public export method test() :
    assume f(1) == 1
    assume f([1, 2, 3]) == [1,2,3]
