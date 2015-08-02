

function reverse([int] ls) -> ([int] result)
ensures |result| == |ls|:
    int i = |ls|
    [int] r = [0; |ls|]
    while i > 0 where i <= |ls| && |r| == |ls| - i:
        int item = ls[|ls|-i]
        i = i - 1
        r[i] = item
    return r

public export method test() -> void:
    assume reverse([]) == []
    assume reverse([1]) == [1]
    assume reverse([1,2]) == [2,1]
    assume reverse([1,2,3]) == [3,2,1]
    assume reverse([1,2,3,4]) == [4,3,2,1]
    assume reverse([1, 2, 3, 4, 5]) == [5,4,3,2,1]

