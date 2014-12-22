function abs(int x) => (int r)
ensures r >= 0:
    //
    if x < 0:
        x = -x
    //
    return x

method main(System.Console console):
    console.out.println("abs(1) = " ++ Int.toString(abs(1)))
    console.out.println("abs(-1) = " ++ Int.toString(abs(-1)))
