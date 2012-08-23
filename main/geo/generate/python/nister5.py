# Expands out symbolic equations for solving the linear system and polynomials in Nister's paper
# The script should be run using sage by typing "sage nister5.py"
#
# Works with Sage Version 5.2

from sage.all import *

def symMatrix( numRows , numCols , letter ):
  A = matrix(SR,numRows,numCols)
  for i in range(0,numRows):
    for j in range(0,numCols):
      A[i,j] = var('%s%d%d' % (letter,i,j) )

  return A

def expandPower( expression ):
  l = expression.split('*')

  # handle special case with a minus sign in front
  if l[0][0] == '-':
    l[0] = l[0][1:]
    expression = '-'
  else:
    expression = ''

  for s in l:
    if len(s) >= 3 and s[-2] == '^':
       var = s[:-2]
       expanded = var
       for i in range(int(s[-1])-1): expanded += '*'+var
       expression += expanded + '*'
    else:
       expression += s + '*'
  return expression[:-1]
          
def printEq( expression , key ):
  chars = set('xyz')
  # expand out and convert into a string
  expression = str(expression.expand())
  # Make sure negative symbols are not stripped and split into multiplicative blocks
  s = expression.replace('- ','-').split(' ')
  # Find blocks multiplied by the key and remove the key from the string
  if len(key) == 0:
    var = [w for w in s if len(w) != 1 and not any( c in chars for c in w )]
  else:
    var = [w[:-(1+len(key))] for w in s if (w.endswith(key) and not any(c in chars for c in w[:-len(key)])) ]
 
  # Expand out power
  var = [expandPower(w) for w in var] 
 
  # construct a string which can be compiled
  ret = var[0]
  for w in var[1:]:
    if w[0] == '-':
      ret += ' - '+w[1:]
    else:
      ret += ' + '+w
  return ret; 
      
def printData( var , eqs , keys ):
  f = open('%s.txt'%var,'w')
  for row,eq in enumerate(eqs):
    index = len(keys)*row
    for k in keys:
      f.write('%s.data[%d] = %s;\n'%(var,index,printEq(eq,k)))
      index += 1
  f.close()

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

# print out machine code for the linear system 
printData('A',eqs,keysA)
printData('B',eqs,keysB)

print 'Done'
