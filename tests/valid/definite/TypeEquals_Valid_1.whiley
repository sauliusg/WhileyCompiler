define bop as {int x, int y} where x > 0
define expr as int|bop

void f(expr e):
    if e ~= int:
        out->println("GOT INT")
    else:
        out->println("GOT BOB")

void System::main([string] args):
    e = 1
    f(e)
    e = {x:1,y:2}
    f(e)
 
