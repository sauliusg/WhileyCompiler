
type urf1nat is (int n) where n > 0

type turf1nat is (int x) where x > 10

type wurf1nat is urf1nat | turf1nat

function f(wurf1nat x) -> int:
    return x

function g(int x) -> int:
    return f((urf1nat) x)
