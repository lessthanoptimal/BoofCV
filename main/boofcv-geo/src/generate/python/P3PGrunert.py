# Turns out I didn't need to solve all these equations myself.  In the journal article the authors fully expanded
# and simplified the equations out themselves.  Keeping it around since, why not?

from sage.all import *

from utilsymbolic import *

# cos(alpha) = cos23  cos(beta) = cos13  cos(gamma) = cos12
(a,b,c,v,cos23,cos13,cos12)=var('a,b,c,v,cos23,cos13,cos12')

top = (-1 + (a*a - c*c)/(b*b))*v*v - 2*((a*a - c*c)/(b*b))*cos13*v + 1 + (a*a - c*c)/(b*b)
bottom = 2*(cos12 - v*cos23)
u = top/bottom

# f=u*u + ((b*b - a*a)/(b*b))*v*v - 2*u*v*cos23 + 2*a*a/(b*b)*v*cos13 - a*a/(b*b)
f = top*top + bottom*bottom*((b*b - a*a)/(b*b))*v*v - 2*bottom*top*v*cos23 + bottom*bottom*(2*a*a/(b*b)*v*cos13 - a*a/(b*b))

swap=[
  ('a*a*a*a','a_4'),('(b*b*b*b)','b_4'),('c*c*c*c','c_4'),
  ('a*a*a','a_3'),('(b*b*b)','b_3'),('c*c*c*','c_3*')]


print 'poly.c[0] = '+performSwap(extractNotVarEq(f,'v'),swap)+';'
print 'poly.c[1] = '+performSwap(extractVarEq(f,'v'),swap)+';'
print 'poly.c[2] = '+performSwap(extractVarEq(f,'v^2'),swap)+';'
print 'poly.c[3] = '+performSwap(extractVarEq(f,'v^3'),swap)+';'
print 'poly.c[4] = '+performSwap(extractVarEq(f,'v^4'),swap)+';'