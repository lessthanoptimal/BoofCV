import numpy as np
from pyboof import gateway
from pyboof.common import *

def real_ejml_to_nparray( ejml ):
    num_rows = ejml.getNumRows()
    num_cols = ejml.getNumCols()

    M = np.zeros((num_rows,num_cols))
    for i in xrange(num_rows):
        for j in xrange(num_cols):
            M[i,j] = ejml.unsafe_get(i,j)
    return M

def real_nparray_to_ejml( array ):
    num_rows = array.shape[0]
    num_cols = array.shape[1]

    M = gateway.jvm.org.ejml.data.DenseMatrix64F(num_rows,num_cols)
    for i in xrange(num_rows):
        for j in xrange(num_cols):
            M.unsafe_set(i,j,array[i,j])
    return M

class Se3_F64(JavaWrapper):
    def __init__(self, java_Se3F64=None):
        if java_Se3F64 is None:
            self.set_java_object(gateway.jvm.georegression.struct.se.Se3_F64())
        else:
            self.set_java_object(java_Se3F64)

    def get_rotation(self):
        return real_ejml_to_nparray(self.java_obj.getR())

    def get_translation(self):
        T = self.java_obj.getT()
        return (T.getX(),T.getY(),T.getZ())

