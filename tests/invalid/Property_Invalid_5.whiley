type nat is (int x) where x >= 0

property absent(int[] items, int item, int end)
where all { i in 0..end | items[i] != item }

function indexOf(int[] items, int item) -> (int r)
ensures (r >= 0) ==> (items[r] == item)
ensures (r < 0) ==> absent(items,item,|items|)
    //
    nat i = 0
    //
    while i < |items| where absent(items,item,i):
        if items[i] == 0:
            return i
        i = i + 1
    //
    return -1

public export method test():
    int[] items = [4,3,1,5,4]
    assume indexOf(items,0) == -1
    assume indexOf(items,1) == 2
    assume indexOf(items,4) == 0
    