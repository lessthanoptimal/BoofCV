# One type-o was found in this section of the paper.  Validating other equations in an attempt to hunt down problems

from sage.all import *

from utilsymbolic import *

(A,B,C,D,E,F)=var('A,B,C,D,E,F')

(l,cos23,cos12,cos13)=var('l,cos23,cos12,cos13')

(a,b,c)=var('a,b,c')

# Validating equation (14)
eq = C*(A*F - D**2) + B*(2*D*E - B*F) - A*E**2

eq = eq.substitute(A = 1 + l)
eq = eq.substitute(B = -cos23)
eq = eq.substitute(C = 1 - a**2/b**2 - l*c**2/b**2)
eq = eq.substitute(D = -l*cos12)
eq = eq.substitute(E = (a**2/b**2 + l*c**2/b**2)*cos13)
eq = eq.substitute(F = -a**2/b**2 + l*(1-c**2/b**2))
eq *= b**4

print 'double J = '+extractNotVarEq(eq,'l')+';'
print 'double I = '+extractVarEq(eq,'l')+';'
print 'double H = '+extractVarEq(eq,'l^2')+';'
print 'double G = '+extractVarEq(eq,'l^3')+';'


