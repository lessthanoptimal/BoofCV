# Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
#
# This file is part of BoofCV (http://boofcv.org).
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Expands out symbolic equations for solving the linear system and polynomials in Nister's paper
# The script should be run using sage by typing "sage nister5.py"
#
# Works with Sage Version 5.2

from numpy.core.fromnumeric import var
from numpy.linalg.linalg import det
from sage.all import *

from utilsymbolic import *

x,y,z = var('x','y','z')

X = symMatrix( 3, 3 , 'X')
Y = symMatrix( 3, 3 , 'Y')
Z = symMatrix( 3, 3 , 'Z')
W = symMatrix( 3, 3 , 'W')

E = x*X + y*Y + z*Z + 1*W
EE = E*E.T

eq1=det(E)
eq2=EE*E-0.5*EE.trace()*E

eqs = (eq1,eq2[0,0],eq2[0,1],eq2[0,2],eq2[1,0],eq2[1,1],eq2[1,2],eq2[2,0],eq2[2,1],eq2[2,2])

keysA = ('x^3','y^3','x^2*y','x*y^2','x^2*z','x^2','y^2*z','y^2','x*y*z','x*y')
keysB = ('x*z^2','x*z','x','y*z^2','y*z','y','z^3','z^2','z','')

# WARNING: The code has been modified since it was originally written and can't handle and empty key any more
#          Write a new function which can handle that case.  The offending function is extractVarEq() which was
#          made more generic

# print out machine code for the linear system
printData('A',eqs,keysA)
printData('B',eqs,keysB)

print 'Done'
