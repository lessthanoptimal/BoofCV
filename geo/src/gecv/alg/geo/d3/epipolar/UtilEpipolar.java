/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.geo.d3.epipolar;

import gecv.alg.geo.AssociatedPair;
import jgrl.geometry.GeometryMath;
import jgrl.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class UtilEpipolar {
    /**
     * Computes a normalization matrix.  After applying this transform to each point in an image
     * they will be conditioned such that their values are evenly spread.  Making the result much more
     * stable.
     *
     * TODO describe statistical properties of normalized points.
     *
     * @param N1 normalization matrix for first set of points. Modified.
     * @param N2 normalization matrix for second set of points. Modified.
     * @param points List of observed points that are to be normalized. Not modified.
     */
    public static void computeNormalization( DenseMatrix64F N1, DenseMatrix64F N2,
                                             List<AssociatedPair> points)
    {
        double meanX1 = 0;
        double meanY1 = 0;
        double meanX2 = 0;
        double meanY2 = 0;

        for( AssociatedPair p : points ) {
            meanX1 += p.keyLoc.x;
            meanY1 += p.keyLoc.y;
            meanX2 += p.currLoc.x;
            meanY2 += p.currLoc.y;
        }

        meanX1 /= points.size();
        meanY1 /= points.size();
        meanX2 /= points.size();
        meanY2 /= points.size();

        double stdX1 = 0;
        double stdY1 = 0;
        double stdX2 = 0;
        double stdY2 = 0;

        for( AssociatedPair p : points ) {
            double dx = p.keyLoc.x - meanX1;
            double dy = p.keyLoc.y - meanY1;
            stdX1 += dx*dx;
            stdY1 += dy*dy;

            dx = p.currLoc.x - meanX2;
            dy = p.currLoc.y - meanY2;
            stdX2 += dx*dx;
            stdY2 += dy*dy;
        }

        stdX1 = Math.sqrt(stdX1/points.size());
        stdY1 = Math.sqrt(stdY1/points.size());
        stdX2 = Math.sqrt(stdX2/points.size());
        stdY2 = Math.sqrt(stdY2/points.size());

        N1.set(0,0,1.0/stdX1);
        N1.set(1,1,1.0/stdY1);
        N1.set(0,2,-meanX1/stdX1);
        N1.set(1,2,-meanY1/stdY1);
        N1.set(2,2,1.0);

        N2.set(0,0,1.0/stdX2);
        N2.set(1,1,1.0/stdY2);
        N2.set(0,2,-meanX2/stdX2);
        N2.set(1,2,-meanY2/stdY2);
        N2.set(2,2,1.0);
    }

    public static void pixelToNormalized( DenseMatrix64F K_inv , Point2D_F64 pixel , Point2D_F64 norm ) {
        GeometryMath.mult(K_inv,pixel,norm);
    }

    /**
     * Converts the pairs from pixel to normalized image coordinates.
     *
     * @param K calibration matrix. Not modified.
     * @param pixels location in pixels. Not modified.
     * @param norms Computed location in normalized image coordinates. Modified.
     */
    public static void pixelToNormalized( DenseMatrix64F K , List<AssociatedPair> pixels , List<AssociatedPair> norms ) {
        // if it did not come with predeclared data, declare the data now
        while( norms.size() < pixels.size() ) {
            norms.add( new AssociatedPair() );
        }

        DenseMatrix64F K_inv = new DenseMatrix64F(3,3);

        CommonOps.invert(K,K_inv);

        double k00 = K_inv.get(0,0);
        double k01 = K_inv.get(0,1);
        double k02 = K_inv.get(0,2);
        double k11 = K_inv.get(1,1);
        double k12 = K_inv.get(1,2);
        double k22 = K_inv.get(2,2);

        for( int i = 0; i < pixels.size(); i++ ) {
            AssociatedPair pixelPair = pixels.get(i);
            AssociatedPair normPair = norms.get(i);

            Point2D_F64 p = pixelPair.keyLoc;
            Point2D_F64 n = normPair.keyLoc;

            n.x = (p.x*k00 + p.y*k01 + k02)/k22;
            n.y = (p.y*k11 + k12)/k22;

            p = pixelPair.currLoc;
            n = normPair.currLoc;

            n.x = (p.x*k00 + p.y*k01 + k02)/k22;
            n.y = (p.y*k11 + k12)/k22;

//            System.out.println("normed "+n.x+" "+n.y);
        }
    }

}
