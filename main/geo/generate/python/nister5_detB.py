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

# Expands the determinant of the B matrix to find the coefficients of a 10th degree polynomial
#
# Works with Sage Version 5.2

from sage.all import *

from utilsymbolic import *


def printCoef( equation , power ):
  # Make sure negative symbols are not stripped and split into multiplicative blocks
  s = str(equation).replace('- ','-').split(' ')

  # Only print coeffients of the appropriate power and strip of z
  if power > 1:
    tail = '^'+str(power)
    s = [w[:-len(tail)-2] for w in s if w.endswith(tail)]
  elif power == 1:
    s = [w[:-2] for w in s if w.endswith('z') ]
  else:
    s = [w for w in s if len(w) > 1 and w.find('z') == -1]
  
  ret = s[0]
  for w in s[1:]:
    if w[0] == '-':
      ret += ' - '+w[1:]
    else:
      ret += ' + '+w
  return simplifyExpanded(ret)
  
  

z = var('z')

K = symVector(13,'K')
L = symVector(13,'L')
M = symVector(13,'M')

z3 = matrix(4,1,[z**3,z**2,z,1])
z4 = matrix(5,1,[z**4,z**3,z**2,z,1])

# all the numpy()[0,0] stuff is to strip off some added formatting
# that was screwing up B.det().  Seems like a bug/bad design to me...
B=matrix(SR,3,3)
B[0,0] = (K[0:4].T*z3).numpy()[0,0]
B[0,1] = (K[4:8].T*z3).numpy()[0,0]
B[0,2] = (K[8:13].T*z4).numpy()[0,0]
B[1,0] = (L[0:4].T*z3).numpy()[0,0]
B[1,1] = (L[4:8].T*z3).numpy()[0,0]
B[1,2] = (L[8:13].T*z4).numpy()[0,0]
B[2,0] = (M[0:4].T*z3).numpy()[0,0]
B[2,1] = (M[4:8].T*z3).numpy()[0,0]
B[2,2] = (M[8:13].T*z4).numpy()[0,0]

p = B.det().expand()

f = open('poly.txt','w')
for degree in range(0,11):
  f.write('coefs[%d] = %s;\n'%(degree,printCoef(p,degree)))
f.close()

