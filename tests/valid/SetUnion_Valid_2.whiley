import whiley.lang.*

function append([int] input) -> {int}:
    {int} rs = {}
    for i in 0 .. |input|:
        rs = {(int) input[i]} + rs
    return rs

method main(System.Console sys) -> void:
    {int} xs = append("abcdefghijklmnopqrstuvwxyz")
    assume xs == {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122}

