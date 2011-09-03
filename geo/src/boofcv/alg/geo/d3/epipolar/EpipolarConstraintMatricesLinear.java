/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.geo.d3.epipolar;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;
import org.ejml.ops.SpecializedOps;
import org.ejml.simple.SimpleMatrix;

import java.util.List;


/**
 * <p>
 * Given a set of points this class computes matrices (essential/fundamental, homography) that define epipolar constraints of the form:<br>
 * <br>
 * x<sup>T</sup><sub>2</sub>*F*x<sub>1</sub> = 0<br>
 * where F is a matrix,  x is a pixel location in the right (2) or left (1) images or in the case
 * of a plane:<br>
 * hat{x}<sub>2</sub>H x<sub>1</sub>
 * where hat(x) is a skew symmetric cross product matrix. The solution to these problems are
 * computed using a linear least-squares approach.  The result is often used as an initial guess
 * for more accurate non-linear approaches.
 * </p>
 * 
 * <p>
 * The coordinates are normalized improve numerical conditioning.  This will only produce significantly different
 * results when points are in pixel coordinates.
 * </p>
 *
 * @author Peter Abeles
 */
public class EpipolarConstraintMatricesLinear {
    // contains the set of equations that are solved
    private DenseMatrix64F A = new DenseMatrix64F(1,9);
    private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(0,0,true,true,false);

    // either the fundamental or homography matrix
    private DenseMatrix64F M = new DenseMatrix64F(3,3);

    private DenseMatrix64F temp0 = new DenseMatrix64F(3,3);

    // matrix used to normalize results
    private DenseMatrix64F N1 = new DenseMatrix64F(3,3);
    private DenseMatrix64F N2 = new DenseMatrix64F(3,3);

    // if true it will normalize observations. Normalization will reduce numerical issues
    // and is needed when dealing with pixel coordinates due to the wide range of values from small to large.
    private boolean normalize = true;

    /**
     * Returns the fundamental or homography matrix depending on which operation was called.
     *
     * @return F or H matrix. This matrix is saved internally and reused.
     */
    public DenseMatrix64F getEpipolarMatrix() {
        return M;
    }

    /**
     * Sets points normalization on and off.  Turn normalization on if dealing with pixels
     * and off if with normalized image coordinates.  Note that having normalization on when
     * it is not needed will not adversely affect the solution, except make it more computationally
     * expensive to compute.
     *
     * @param normalize The new normalization value
     */
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    /**
     * <p>
     * Computes a fundamental matrix from a set of associated points. This formulation requires a minimum
     * of eight points.  If input points are poorly condition for linear operations (in pixel coordinates)
     * then make sure normalization is turned on.
     * </p>
     *
     * <p>
     * Follows the procedures outlined in "An Invitation to 3-D Vision" 2004 with some minor modifications.
     * </p>
     *
     * @param points List of correlated points in image coordinates from perspectives. Either in pixel or normalized image coordinates.
     * @return true if it thinks it succeeded and false if it knows it failed.
     */
    public boolean computeFundamental( List<AssociatedPair> points ) {
        if( points.size() < 8 )
            throw new IllegalArgumentException("Must be at least 8 points. Was only "+points.size());

        if( normalize ) {
            UtilEpipolar.computeNormalization(N1, N2, points);

            createA(points,A);
        } else {
            createA_nonorm(points,A);
        }

        if (computeMatrix(svd,A))
            return false;

        if( normalize )
            undoNormalizationF(M,N1,N2);

        // just happens that since F is in a row major format there is no need to copy any memory
        return enforceSmallZeroSingularValue();
    }

    /**
     * <p>
     * Computes a planar homography matrix up to a scale factor:<br>
     * H<sub>s</sub> = &alpha;H<br>
     * where H<sub>s</sub> is the returned homography matrix.
     * </p>
     * <p>
     * This follows the procedure described in "An Invitation to 3-D Vision" 2004, but with out normalization
     * since it was found to not be of much practical use.  If this matrix was to be decomposed further then
     * the normalization procedure would need to be followed.  A minimum number of four points are required.
     * </p>
     *
     * <p>
     * Primarily based on chapter 4 in, " Multiple View Geometry in Computer Vision"  2nd Ed. but uses normalization
     * from "An Invitation to 3-D Vision" 2004.
     * </p>
     *
     * @param points A set of points that are generated from a planar object.
     * @return true if the calculation was a success.
     */
    public boolean computeHomography( List<AssociatedPair> points ) {
        if( points.size() < 4 )
            throw new IllegalArgumentException("Must be at least 4 points.");

        if( normalize ) {
            UtilEpipolar.computeNormalization(N1, N2, points);

            createHomoA(points,A);
        } else {
            throw new RuntimeException("Unnnormalized homography hasn't been created yet");
        }

        // compute the homograph matrix up to a scale factor
        if (computeMatrix(svd,A))
            return false;

        if( normalize )
            undoNormalizationH(M,N1,N2);

        adjustHomographSign(points.get(0));

        return true;
    }

    /**
     * Undo the normalization done to the input matrices for a Fundamental matrix.
     * <br>
     * M = N<sub>2</sub><sup>T</sup>*M*N<sub>1</sub>
     *
     * @param M Either the homography or fundamental matrix computed from normalized points.
     * @param N1 normalization matrix.
     * @param N2 normalization matrix.
     */
    private void undoNormalizationF(DenseMatrix64F M, DenseMatrix64F N1, DenseMatrix64F N2) {
        SimpleMatrix a = SimpleMatrix.wrap(M);
        SimpleMatrix b = SimpleMatrix.wrap(N1);
        SimpleMatrix c = SimpleMatrix.wrap(N2);

        SimpleMatrix result = c.transpose().mult(a).mult(b);

        M.set(result.getMatrix());
    }

    /**
     * Undoes normalization for a homography matrix.
     */
    private void undoNormalizationH(DenseMatrix64F M, DenseMatrix64F N1, DenseMatrix64F N2) {
        SimpleMatrix a = SimpleMatrix.wrap(M);
        SimpleMatrix b = SimpleMatrix.wrap(N1);
        SimpleMatrix c = SimpleMatrix.wrap(N2);

        SimpleMatrix result = c.invert().mult(a).mult(b);

        M.set(result.getMatrix());
    }

    /**
     * Since the sign of the homography is ambiguous a point is required to make sure the correct
     * one was selected.
     *
     * @param p test point, used to determine the sign of the matrix.
     */
    private void adjustHomographSign( AssociatedPair p ) {
        // now figure out the sign
        DenseMatrix64F x1 = new DenseMatrix64F(3,1,true,p.currLoc.x,p.currLoc.y,1);
        DenseMatrix64F x2 = new DenseMatrix64F(3,1,true,p.keyLoc.x,p.keyLoc.y,1);

        double val = VectorVectorMult.innerProdA(x2,M,x1);
        if( val < 0 )
            CommonOps.scale(-1,M);
    }

    /**
     * Computes the epipolar constraint matrix by performing a SVD on A.  The column of V corresponding
     * to the smallest singular value of A is the solution we are looking for.
     */
    private boolean computeMatrix( SingularValueDecomposition<DenseMatrix64F> svd , DenseMatrix64F A ) {
        if( !svd.decompose(A) )
            return true;

        if( A.numRows > 8 )
            SingularOps.nullSpace(svd,M);
        else {
            DenseMatrix64F V = svd.getV(false);
            SpecializedOps.subvector(V,0,8,V.numCols,false,0,M);
        }

        return false;
    }

    /**
     * Reorganizes the epipolar constraint equation (x<sup>T</sup><sub>2</sub>*F*x<sub>1</sub> = 0) such that it
     * is formulated as a standard linear system of the form Ax=0.  Where A contains the pixel locations and x is
     * the reformatted fundamental matrix.
     *
     * @param points Set of associated points in left and right images.
     * @param A Matrix where the reformatted points are written to.
     */
    private void createA(List<AssociatedPair> points, DenseMatrix64F A ) {
        A.reshape(points.size(),9, false);
        A.zero();

        Point2D_F64 f_norm = new Point2D_F64();
        Point2D_F64 s_norm = new Point2D_F64();

        final int size = points.size();
        for( int i = 0; i < size; i++ ) {
            AssociatedPair p = points.get(i);

            Point2D_F64 f = p.keyLoc;
            Point2D_F64 s = p.currLoc;

            // normalize the points
            normalize(f,f_norm,N1);
            normalize(s,s_norm,N2);

            // perform the Kronecker product with the two points being in
            // homogeneous coordinates (z=1)
            A.set(i,0,s_norm.x*f_norm.x);
            A.set(i,1,s_norm.x*f_norm.y);
            A.set(i,2,s_norm.x);
            A.set(i,3,s_norm.y*f_norm.x);
            A.set(i,4,s_norm.y*f_norm.y);
            A.set(i,5,s_norm.y);
            A.set(i,6,f_norm.x);
            A.set(i,7,f_norm.y);
            A.set(i,8,1);
        }
    }

    /**
     * Same as {@link #createA}
     * @param points
     * @param A
     */
    private void createA_nonorm(List<AssociatedPair> points, DenseMatrix64F A ) {
        A.reshape(points.size(),9, false);
        A.zero();

        final int size = points.size();
        for( int i = 0; i < size; i++ ) {
            AssociatedPair p = points.get(i);

            Point2D_F64 f = p.keyLoc;
            Point2D_F64 s = p.currLoc;

            // perform the Kronecker product with the two points being in
            // homogeneous coordinates (z=1)
            A.set(i,0,s.x*f.x);
            A.set(i,1,s.x*f.y);
            A.set(i,2,s.x);
            A.set(i,3,s.y*f.x);
            A.set(i,4,s.y*f.y);
            A.set(i,5,s.y);
            A.set(i,6,f.x);
            A.set(i,7,f.y);
            A.set(i,8,1);
        }
    }

    /**
     * Compute the 'A' matrix for a homography.
     *
     * @param points
     * @param A
     */
    private void createHomoA(List<AssociatedPair> points, DenseMatrix64F A ) {
        A.reshape(points.size()*2,9, false);
        A.zero();

        Point2D_F64 f_norm = new Point2D_F64();
        Point2D_F64 s_norm = new Point2D_F64();

        final int size = points.size();
        for( int i = 0; i < size; i++ ) {
            AssociatedPair p = points.get(i);

            // the first image
            Point2D_F64 f = p.keyLoc;
            // the second image
            Point2D_F64 s = p.currLoc;

            // normalize the points
            normalize(f,f_norm,N1);
            normalize(s,s_norm,N2);
//            f_norm = f;
//            s_norm = s;


            A.set(i*2   , 3 , -f_norm.x);
            A.set(i*2   , 4 , -f_norm.y);
            A.set(i*2   , 5 , -1);
            A.set(i*2   , 6 , s_norm.y*f_norm.x);
            A.set(i*2   , 7 , s_norm.y*f_norm.y);
            A.set(i*2   , 8 , s_norm.y);
            A.set(i*2+1 , 0 , f_norm.x);
            A.set(i*2+1 , 1 , f_norm.y);
            A.set(i*2+1 , 2 , 1);
            A.set(i*2+1 , 6 , -s_norm.x*f_norm.x);
            A.set(i*2+1 , 7 , -s_norm.x*f_norm.y);
            A.set(i*2+1 , 8 , -s_norm.x);

        }
//        A.print();
    }


    /**
     * Makes sure the smallest SVD in F is equal to zero.  This is done by finding the smallest SVD
     * and setting it to zero then recomputing F.
     *
     * @return true if svd returned true.
     */
    private boolean enforceSmallZeroSingularValue() {
        int indexSmallest;
        if( !svd.decompose(M) ) {
            return false;
        }

        indexSmallest = findSmallestSingularValue(svd);

        DenseMatrix64F V = svd.getV(false);
        DenseMatrix64F U = svd.getU(false);
        DenseMatrix64F D = svd.getW(null);

        // force the smallest SVD to be zero
        D.set(indexSmallest,indexSmallest,0);

        // recompute F
        CommonOps.mult(U,D,temp0);
        CommonOps.multTransB(temp0,V, M);

        return true;
    }

    private static int findSmallestSingularValue(SingularValueDecomposition svd ) {
        double smallestVal = Double.MAX_VALUE;
        int indexSmallest = -1;

        double s[] = svd.getSingularValues();
        int N = svd.numberOfSingularValues();
        for( int i = 0; i < N; i++ ) {
            if( s[i] < smallestVal ) {
                smallestVal = s[i];
                indexSmallest = i;
            }
        }
        return indexSmallest;
    }

    /**
     * Given the normalization matrix it will normalize the point
     */
    private void normalize( Point2D_F64 orig , Point2D_F64 normed , DenseMatrix64F N ) {
        
        normed.x = orig.x * N.get(0,0) + N.get(0,2);
        normed.y = orig.y * N.get(1,1) + N.get(1,2);

//        normed.x = orig.x;
//        normed.y = orig.y;
    }

}
