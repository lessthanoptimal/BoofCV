from sage.all import *

from utilsymbolic import *

Xa=symVector(3,'xa')
Xb=symVector(3,'xb')
Xc=symVector(3,'xc')

# Set the z coordinate to 1
Xa[2] = 1
Xb[2] = 1
Xc[2] = 1


T1=symMatrix(3,3,'t1')
T2=symMatrix(3,3,'t2')
T3=symMatrix(3,3,'t3')

XCb = symCrossMat3x3(Xb)
XCc = symCrossMat3x3(Xc)


insideSum = multScalarMatrix(Xa[0],T1) + multScalarMatrix(Xa[1],T2) + multScalarMatrix(Xa[2],T3)
constraint3p = XCb*insideSum*XCc

swap=[
    ('xa0','p1_norm.x'),('xa1','p1_norm.y'),
    ('xb0','p2_norm.x'),('xb1','p2_norm.y'),
    ('xc0','p3_norm.x'),('xc1','p3_norm.y')]

def printCode( input ):
    for k in range(1,4):
        for row in range(0,3):
            for col in range(0,3):
                i = row*3+col
                key = 't'+str(k)+str(row)+str(col)
                data = 'A.data[index'+str(k)+' + '+str(i)+"] = "
                eq = extractVarEq(input,key)
                if eq:
                    print data,performSwap(extractVarEq(input,key),swap)+';'

for i in range(0,3):
    for j in range(0,3):
        print ''
        print '// constraint for ('+str(i)+","+str(j)+")"
        print 'index2 = index1+9;'
        print 'index3 = index1+18;'
        print ''
        printCode(constraint3p[i,j])
        print 'index1 += 27;'
