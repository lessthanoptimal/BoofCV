/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.alg.geo;

import boofcv.alg.geo.f.FundamentalExtractEpipoles;
import boofcv.alg.geo.h.HomographyInducedStereo2Line;
import boofcv.alg.geo.h.HomographyInducedStereo3Pts;
import boofcv.alg.geo.h.HomographyInducedStereoLinePt;
import boofcv.alg.geo.trifocal.TrifocalExtractGeometries;
import boofcv.alg.geo.trifocal.TrifocalTransfer;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PairLineNorm;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.Tuple2;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.QRDecomposition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Contains commonly used operations used in 2-view and 3-view perspective geometry.
 * </p>
 *
 * <p>
 * LINES:  lines on the image place are represented in homogeneous or generic form as a 3D vector. If a point in
 * homogeneous coordinates is on a line and the dot product is computed the result will be zero.
 * </p>
 *
 * @author Peter Abeles
 */
public class MultiViewOps {

	/**
	 * <p>
	 * Creates a trifocal tensor from two camera matrices.
	 * </p>
	 *
	 * <p>
	 * IMPORTANT: It is assumed that the first camera has the following camera matrix P1 = [I|0],
	 * where I is an identify matrix.
	 * </p>
	 *
	 * @param P2 Camera matrix from view 1 to view 2
	 * @param P3 Camera matrix from view 1 to view 3
	 * @param ret Storage for trifocal tensor.  If null a new instance will be created.
	 * @return The trifocal tensor
	 */
	public static TrifocalTensor createTrifocal( DMatrixRMaj P2 , DMatrixRMaj P3 , TrifocalTensor ret ) {
		if( ret == null )
			ret = new TrifocalTensor();

		for( int col = 0; col < 3; col++ ) {
			DMatrixRMaj T = ret.getT(col);

			int index = 0;
			for( int i = 0; i < 3; i++ ) {
				double a_left = P2.get(i,col);
				double a_right = P2.get(i,3);

				for( int j = 0; j < 3; j++ ) {
					T.data[index++] = a_left*P3.get(j,3) - a_right*P3.get(j,col);
				}
			}
		}

		return ret;
	}

	/**
	 * <p>
	 * Creates a trifocal tensor from two rigid body motions.  This is for the calibrated camera case.
	 * </p>
	 *
	 * <p>
	 * NOTE: View 1 is the world coordinate system, i.e. [I|0]
	 * </p>
	 *
	 * @param P2 Transform from view 1 to view 2.
	 * @param P3 Transform from view 1 to view 3.
	 * @param ret Storage for trifocal tensor.  If null a new instance will be created.
	 * @return The trifocal tensor
	 */
	public static TrifocalTensor createTrifocal( Se3_F64 P2 , Se3_F64 P3 , TrifocalTensor ret ) {
		if( ret == null )
			ret = new TrifocalTensor();

		DMatrixRMaj R2 = P2.getR();
		DMatrixRMaj R3 = P3.getR();
		Vector3D_F64 T2 = P2.getT();
		Vector3D_F64 T3 = P3.getT();

		for( int col = 0; col < 3; col++ ) {
			DMatrixRMaj T = ret.getT(col);

			int index = 0;
			for( int i = 0; i < 3; i++ ) {
				double a_left = R2.unsafe_get(i,col);
				double a_right = T2.getIndex(i);

				for( int j = 0; j < 3; j++ ) {
					T.data[index++] = a_left*T3.getIndex(j) - a_right*R3.unsafe_get(j,col);
				}
			}
		}

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with line-line-line correspondence:<br>
	 * (l2<sup>T</sup>*[T1,T2,T3]*L2)*[l1]<sub>x</sub> = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param l1 A line in the first view.
	 * @param l2 A line in the second view.
	 * @param l3 A line in the third view.
	 * @param ret Storage for output.  If null a new instance will be declared.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static Vector3D_F64 constraint(TrifocalTensor tensor,
										  Vector3D_F64 l1, Vector3D_F64 l2, Vector3D_F64 l3,
										  Vector3D_F64 ret)
	{
		if( ret == null )
			ret = new Vector3D_F64();

		double x = GeometryMath_F64.innerProd(l2, tensor.T1, l3);
		double y = GeometryMath_F64.innerProd(l2, tensor.T2, l3);
		double z = GeometryMath_F64.innerProd(l2, tensor.T3, l3);

		GeometryMath_F64.cross(new Vector3D_F64(x, y, z), l1, ret);

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with point-line-line correspondence:<br>
	 * (l2<sup>T</sup>*(sum p1<sup>i</sup>*T<sub>i</sub>)*l3 = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param l2 A line in the second view.
	 * @param l3 A line in the third view.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static double constraint(TrifocalTensor tensor,
									Point2D_F64 p1, Vector3D_F64 l2, Vector3D_F64 l3)
	{
		DMatrixRMaj sum = new DMatrixRMaj(3,3);

		CommonOps_DDRM.add(p1.x,tensor.T1,sum,sum);
		CommonOps_DDRM.add(p1.y,tensor.T2,sum,sum);
		CommonOps_DDRM.add(tensor.T3, sum, sum);

		return GeometryMath_F64.innerProd(l2,sum,l3);
	}

	/**
	 * <p>
	 * Trifocal tensor with point-line-point correspondence:<br>
	 * (l2<sup>T</sup>(sum p1<sup>i</sup>*T<sub>i</sub>)[p3]<sub>x</sub> = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param l2 A line in the second view.
	 * @param p3 A point in the third view.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static Vector3D_F64 constraint(TrifocalTensor tensor,
										  Point2D_F64 p1, Vector3D_F64 l2, Point2D_F64 p3,
										  Vector3D_F64 ret)
	{
		if( ret == null )
			ret = new Vector3D_F64();

		DMatrixRMaj sum = new DMatrixRMaj(3,3);

		CommonOps_DDRM.add(p1.x,tensor.T1,sum,sum);
		CommonOps_DDRM.add(p1.y,tensor.T2,sum,sum);
		CommonOps_DDRM.add(tensor.T3,sum,sum);

		Vector3D_F64 tempV = new Vector3D_F64();
		GeometryMath_F64.multTran(sum, l2, tempV);

		GeometryMath_F64.cross(tempV, new Vector3D_F64(p3.x, p3.y, 1), ret);

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with point-point-line correspondence:<br>
	 * [p2]<sub>x</sub>(sum p1<sup>i</sup>*T<sub>i</sub>)*l3 = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param p2 A point in the second view.
	 * @param l3 A line in the third view.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static Vector3D_F64 constraint(TrifocalTensor tensor,
										  Point2D_F64 p1, Point2D_F64 p2, Vector3D_F64 l3,
										  Vector3D_F64 ret)
	{
		if( ret == null )
			ret = new Vector3D_F64();

		DMatrixRMaj sum = new DMatrixRMaj(3,3);

		CommonOps_DDRM.add(p1.x,tensor.T1,sum,sum);
		CommonOps_DDRM.add(p1.y,tensor.T2,sum,sum);
		CommonOps_DDRM.add(tensor.T3,sum,sum);

		DMatrixRMaj cross2 = GeometryMath_F64.crossMatrix(p2.x,p2.y,1,null);

		DMatrixRMaj temp = new DMatrixRMaj(3,3);

		CommonOps_DDRM.mult(cross2,sum,temp);
		GeometryMath_F64.mult(temp, l3, ret);

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with point-point-point correspondence:<br>
	 * [p2]<sub>x</sub>(sum p1<sup>i</sup>*T<sub>i</sub>)[p3]<sub>x</sub> = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param p2 A point in the second view.
	 * @param p3 A point in the third view.
	 * @param ret Optional storage for output. 3x3 matrix.  Modified.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static DMatrixRMaj constraint(TrifocalTensor tensor,
											Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3,
											DMatrixRMaj ret)
	{
		if( ret == null )
			ret = new DMatrixRMaj(3,3);

		DMatrixRMaj sum = new DMatrixRMaj(3,3);

		CommonOps_DDRM.add(p1.x,tensor.T1,p1.y,tensor.T2,sum);
		CommonOps_DDRM.add(sum,tensor.T3,sum);

		DMatrixRMaj cross2 = GeometryMath_F64.crossMatrix(p2.x,p2.y,1,null);
		DMatrixRMaj cross3 = GeometryMath_F64.crossMatrix(p3.x,p3.y,1,null);

		DMatrixRMaj temp = new DMatrixRMaj(3,3);

		CommonOps_DDRM.mult(cross2,sum,temp);
		CommonOps_DDRM.mult(temp, cross3, ret);

		return ret;
	}

	/**
	 * <p>
	 * Applies the epipolar relationship constraint to an essential or fundamental matrix:<br>
	 * 0 = p2<sup>T</sup>*F*p1<br>
	 * Input points are in normalized image coordinates for an essential matrix and pixels for
	 * fundamental.
	 * </p>
	 *
	 * @param F 3x3 essential or fundamental matrix.
	 * @param p1 Point in view 1.
	 * @param p2 Point in view 2.
	 * @return  Constraint value.
	 */
	public static double constraint( DMatrixRMaj F , Point2D_F64 p1, Point2D_F64 p2 ) {
		return GeometryMath_F64.innerProd(p2,F,p1);
	}

	/**
	 * <p>
	 * Applies the homography constraints to two points:<br>
	 * z*p2 = H*p1<br>
	 * where z is a scale factor and (p1,p2) are point observations.  Note that since 2D points are inputted
	 * translation and normalization to homogeneous coordinates with z=1 is automatically handled.
	 * </p>
	 *
	 * @param H Input: 3x3 Homography matrix.
	 * @param p1 Input: Point in view 1.
	 * @param outputP2 Output: storage for point in view 2.
	 * @return Predicted point in view 2
	 */
	public static Point2D_F64 constraintHomography( DMatrixRMaj H , Point2D_F64 p1 , Point2D_F64 outputP2 ) {
		if( outputP2 == null )
			outputP2 = new Point2D_F64();

		GeometryMath_F64.mult(H,p1,outputP2);

		return outputP2;
	}


	/**
	 * Computes the homography induced from view 1 to 3 by a line in view 2.  The provided line in
	 * view 2 must contain the view 2 observation.
	 *
	 * p3 = H13*p1
	 *
	 * @param tensor Input: Trifocal tensor
	 * @param line2 Input: Line in view 2.  {@link LineGeneral2D_F64 General notation}.
	 * @param output Output: Optional storage for homography. 3x3 matrix
	 * @return Homography from view 1 to 3
	 */
	public static DMatrixRMaj inducedHomography13( TrifocalTensor tensor ,
													  Vector3D_F64 line2 ,
													  DMatrixRMaj output ) {
		if( output == null )
			output = new DMatrixRMaj(3,3);

		DMatrixRMaj T = tensor.T1;

		// H(:,0) = transpose(T1)*line
		output.data[0] = T.data[0]*line2.x + T.data[3]*line2.y + T.data[6]*line2.z;
		output.data[3] = T.data[1]*line2.x + T.data[4]*line2.y + T.data[7]*line2.z;
		output.data[6] = T.data[2]*line2.x + T.data[5]*line2.y + T.data[8]*line2.z;

		// H(:,1) = transpose(T2)*line
		T = tensor.T2;
		output.data[1] = T.data[0]*line2.x + T.data[3]*line2.y + T.data[6]*line2.z;
		output.data[4] = T.data[1]*line2.x + T.data[4]*line2.y + T.data[7]*line2.z;
		output.data[7] = T.data[2]*line2.x + T.data[5]*line2.y + T.data[8]*line2.z;

		// H(:,2) = transpose(T3)*line
		T = tensor.T3;
		output.data[2] = T.data[0]*line2.x + T.data[3]*line2.y + T.data[6]*line2.z;
		output.data[5] = T.data[1]*line2.x + T.data[4]*line2.y + T.data[7]*line2.z;
		output.data[8] = T.data[2]*line2.x + T.data[5]*line2.y + T.data[8]*line2.z;

//		Vector3D_F64 temp = new Vector3D_F64();
//
//		for( int i = 0; i < 3; i++ ) {
//			GeometryMath_F64.multTran(tensor.getT(i),line,temp);
//			output.unsafe_set(0,i,temp.x);
//			output.unsafe_set(1,i,temp.y);
//			output.unsafe_set(2,i,temp.z);
//		}

		return output;
	}

	/**
	 * Computes the homography induced from view 1 to 2 by a line in view 3.  The provided line in
	 * view 3 must contain the view 3 observation.
	 *
	 * p2 = H12*p1
	 *
	 * @param tensor Input: Trifocal tensor
	 * @param line3 Input: Line in view 3.  {@link LineGeneral2D_F64 General notation}.
	 * @param output Output: Optional storage for homography. 3x3 matrix
	 * @return Homography from view 1 to 2
	 */
	public static DMatrixRMaj inducedHomography12( TrifocalTensor tensor ,
													  Vector3D_F64 line3 ,
													  DMatrixRMaj output ) {
		if( output == null )
			output = new DMatrixRMaj(3,3);

		// H(:,0) = T1*line
		DMatrixRMaj T = tensor.T1;
		output.data[0] = T.data[0]*line3.x + T.data[1]*line3.y + T.data[2]*line3.z;
		output.data[3] = T.data[3]*line3.x + T.data[4]*line3.y + T.data[5]*line3.z;
		output.data[6] = T.data[6]*line3.x + T.data[7]*line3.y + T.data[8]*line3.z;

		// H(:,0) = T2*line
		T = tensor.T2;
		output.data[1] = T.data[0]*line3.x + T.data[1]*line3.y + T.data[2]*line3.z;
		output.data[4] = T.data[3]*line3.x + T.data[4]*line3.y + T.data[5]*line3.z;
		output.data[7] = T.data[6]*line3.x + T.data[7]*line3.y + T.data[8]*line3.z;

		// H(:,0) = T3*line
		T = tensor.T3;
		output.data[2] = T.data[0]*line3.x + T.data[1]*line3.y + T.data[2]*line3.z;
		output.data[5] = T.data[3]*line3.x + T.data[4]*line3.y + T.data[5]*line3.z;
		output.data[8] = T.data[6]*line3.x + T.data[7]*line3.y + T.data[8]*line3.z;

//		Vector3D_F64 temp = new Vector3D_F64();
//
//		for( int i = 0; i < 3; i++ ) {
//			GeometryMath_F64.mult(tensor.getT(i), line, temp);
//			output.unsafe_set(0,i,temp.x);
//			output.unsafe_set(1,i,temp.y);
//			output.unsafe_set(2,i,temp.z);
//		}

		return output;
	}

	/**
	 * Computes the homography induced from a planar surface when viewed from two views using correspondences
	 * of three points. Observations must be on the planar surface.
	 *
	 * @see boofcv.alg.geo.h.HomographyInducedStereo3Pts
	 *
	 * @param F Fundamental matrix
	 * @param p1 Associated point observation
	 * @param p2 Associated point observation
	 * @param p3 Associated point observation
	 * @return The homography from view 1 to view 2 or null if it fails
	 */
	public static DMatrixRMaj homographyStereo3Pts( DMatrixRMaj F , AssociatedPair p1, AssociatedPair p2, AssociatedPair p3) {
		HomographyInducedStereo3Pts alg = new HomographyInducedStereo3Pts();

		alg.setFundamental(F,null);
		if( !alg.process(p1,p2,p3) )
			return null;
		return alg.getHomography();
	}

	/**
	 * Computes the homography induced from a planar surface when viewed from two views using correspondences
	 * of a line and a point. Observations must be on the planar surface.
	 *
	 * @see HomographyInducedStereoLinePt
	 *
	 * @param F Fundamental matrix
	 * @param line Line on the plane
	 * @param point Point on the plane
	 * @return The homography from view 1 to view 2 or null if it fails
	 */
	public static DMatrixRMaj homographyStereoLinePt( DMatrixRMaj F , PairLineNorm line, AssociatedPair point) {
		HomographyInducedStereoLinePt alg = new HomographyInducedStereoLinePt();

		alg.setFundamental(F,null);
		alg.process(line,point);
		return alg.getHomography();
	}

	/**
	 * Computes the homography induced from a planar surface when viewed from two views using correspondences
	 * of two lines. Observations must be on the planar surface.
	 *
	 * @see HomographyInducedStereo2Line
	 *
	 * @param F Fundamental matrix
	 * @param line0 Line on the plane
	 * @param line1 Line on the plane
	 * @return The homography from view 1 to view 2 or null if it fails
	 */
	public static DMatrixRMaj homographyStereo2Lines( DMatrixRMaj F , PairLineNorm line0, PairLineNorm line1) {
		HomographyInducedStereo2Line alg = new HomographyInducedStereo2Line();

		alg.setFundamental(F,null);
		if( !alg.process(line0,line1) )
			return null;
		return alg.getHomography();
	}

	/**
	 * <p>
	 * Computes the epipoles of the first camera in the second and third images.  Epipoles are found
	 * in homogeneous coordinates and have a norm of 1.
	 * </p>
	 *
	 * <p>
	 * Properties:
	 * <ul>
	 *     <li> e2<sup>T</sup>*F12 = 0
	 *     <li> e3<sup>T</sup>*F13 = 0
	 * </ul>
	 * where F1i is a fundamental matrix from image 1 to i.
	 * </p>
	 *
	 * @see TrifocalExtractGeometries
	 *
	 * @param tensor Trifocal tensor.  Not Modified
	 * @param e2  Output: Epipole in image 2. Homogeneous coordinates. Modified
	 * @param e3  Output: Epipole in image 3. Homogeneous coordinates. Modified
	 */
	public static void extractEpipoles( TrifocalTensor tensor , Point3D_F64 e2 , Point3D_F64 e3 ) {
		TrifocalExtractGeometries e = new TrifocalExtractGeometries();
		e.setTensor(tensor);
		e.extractEpipoles(e2,e3);
	}

	/**
	 * <p>
	 * Extract the fundamental matrices between views 1 + 2 and views 1 + 3.  The returned Fundamental
	 * matrices will have the following properties: x<sub>i</sub><sup>T</sup>*Fi*x<sub>1</sub> = 0, where i is view 2 or 3.
	 * </p>
	 *
	 * <p>
	 * NOTE: The first camera is assumed to have the camera matrix of P1 = [I|0].  Thus observations in pixels for
	 * the first camera will not meet the epipolar constraint when applied to the returned fundamental matrices.
	 * </p>
	 *
	 * @see TrifocalExtractGeometries
	 *
	 * @param tensor Trifocal tensor.  Not modified.
	 * @param F21 Output: Fundamental matrix for views 1 and 2. Modified.
	 * @param F31 Output: Fundamental matrix for views 1 and 3. Modified.
	 */
	public static void extractFundamental( TrifocalTensor tensor , DMatrixRMaj F21 , DMatrixRMaj F31 ) {
		TrifocalExtractGeometries e = new TrifocalExtractGeometries();
		e.setTensor(tensor);
		e.extractFundmental(F21,F31);
	}

	/**
	 * <p>
	 * Extract the camera matrices up to a common projective transform.
	 * </p>
	 *
	 * <p>
	 * NOTE: The camera matrix for the first view is assumed to be P1 = [I|0].
	 * </p>
	 *
	 * @see TrifocalExtractGeometries
	 *
	 * @param tensor Trifocal tensor.  Not modified.
	 * @param P2 Output: 3x4 camera matrix for views 1 to 2. Modified.
	 * @param P3 Output: 3x4 camera matrix for views 1 to 3. Modified.
	 */
	public static void extractCameraMatrices( TrifocalTensor tensor , DMatrixRMaj P2 , DMatrixRMaj P3 ) {
		TrifocalExtractGeometries e = new TrifocalExtractGeometries();
		e.setTensor(tensor);
		e.extractCamera(P2,P3);
	}

	/**
	 * <p>
	 * Computes an essential matrix from a rotation and translation.  This motion
	 * is the motion from the first camera frame into the second camera frame.  The essential
	 * matrix 'E' is defined as:<br>
	 * E = hat(T)*R<br>
	 * where hat(T) is the skew symmetric cross product matrix for vector T.
	 * </p>
	 *
	 * @param R Rotation matrix.
	 * @param T Translation vector.
	 * @param E (Output) Storage for essential matrix. 3x3 matrix
	 * @return Essential matrix
	 */
	public static DMatrixRMaj createEssential(DMatrixRMaj R, Vector3D_F64 T, @Nullable DMatrixRMaj E)
	{
		if( E == null )
			E = new DMatrixRMaj(3,3);

		DMatrixRMaj T_hat = GeometryMath_F64.crossMatrix(T, null);
		CommonOps_DDRM.mult(T_hat, R, E);

		return E;
	}

	/**
	 * Computes a Fundamental matrix given an Essential matrix and the camera calibration matrix.
	 *
	 * F = (K<sup>-1</sup>)<sup>T</sup>*E*K<sup>-1</sup>
	 *
	 * @param E Essential matrix
	 * @param K Intrinsic camera calibration matrix
	 * @return Fundamental matrix
	 */
	public static DMatrixRMaj createFundamental(DMatrixRMaj E, DMatrixRMaj K) {
		DMatrixRMaj K_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K,K_inv);

		DMatrixRMaj F = new DMatrixRMaj(3,3);
		PerspectiveOps.multTranA(K_inv,E,K_inv,F);

		return F;
	}

	/**
	 * Computes a Fundamental matrix given an Essential matrix and the camera's intrinsic
	 * parameters.
	 *
	 * @see #createFundamental(DMatrixRMaj, DMatrixRMaj)
	 *
	 * @param E Essential matrix
	 * @param intrinsic Intrinsic camera calibration
	 * @return Fundamental matrix
	 */
	public static DMatrixRMaj createFundamental(DMatrixRMaj E, CameraPinhole intrinsic ) {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic,(DMatrixRMaj)null);
		return createFundamental(E,K);
	}

	/**
	 * Computes a Fundamental matrix given an Essential matrix and the camera calibration matrix.
	 *
	 * F = (K2<sup>-1</sup>)<sup>T</sup>*E*K1<sup>-1</sup>
	 *
	 * @param E Essential matrix
	 * @param K1 Intrinsic camera calibration matrix for camera 1
	 * @param K2 Intrinsic camera calibration matrix for camera 2
	 * @return Fundamental matrix
	 */
	public static DMatrixRMaj createFundamental(DMatrixRMaj E,
												   DMatrixRMaj K1,  DMatrixRMaj K2) {
		DMatrixRMaj K1_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K1,K1_inv);
		DMatrixRMaj K2_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(K2,K2_inv);

		DMatrixRMaj F = new DMatrixRMaj(3,3);
		DMatrixRMaj temp = new DMatrixRMaj(3,3);

		CommonOps_DDRM.multTransA(K2_inv,E,temp);
		CommonOps_DDRM.mult(temp,K1_inv,F);

		return F;
	}

	/**
	 * <p>
	 * Computes a homography matrix from a rotation, translation, plane normal and plane distance:<br>
	 * x[2] = H*x[1]<br>
	 * where x[1] is the point on the first camera and x[2] the location in the second camera.<br>
	 * H = R+(1/d)*T*N<sup>T</sup><br>
	 * Where [R,T] is the transform from camera 1 to camera 2.
	 * </p>
	 *
	 * @param R Rotation matrix from camera 1 to camera 2.
	 * @param T Translation vector from camera 1 to camera 2.
	 * @param d Distance &gt; 0 of closest point on plane to the origin of camera 1.
	 * @param N Normal of plane with respect to the first camera.
	 * @return Calibrated homography matrix
	 */
	public static DMatrixRMaj createHomography(DMatrixRMaj R, Vector3D_F64 T,
												  double d, Vector3D_F64 N)
	{
		DMatrixRMaj H = new DMatrixRMaj(3,3);

		GeometryMath_F64.outerProd(T,N,H);
		CommonOps_DDRM.divide(H,d);
		CommonOps_DDRM.addEquals(H, R);

		return H;
	}

	/**
	 * <p>
	 * Computes a homography matrix from a rotation, translation, plane normal, plane distance, and
	 * calibration matrix:<br>
	 * x[2] = H*x[1]<br>
	 * where x[1] is the point on the first camera and x[2] the location in the second camera.<br>
	 * H = K*(R+(1/d)*T*N<sup>T</sup>)*K<sup>-1</sup><br>
	 * Where [R,T] is the transform from camera 1 to camera, and K is the calibration matrix for both cameras.
	 * </p>
	 *
	 * @param R Rotation matrix from camera 1 to camera 2.
	 * @param T Translation vector from camera 1 to camera 2.
	 * @param d Distance &gt; 0 of closest point on plane to the origin of camera 1.
	 * @param N Normal of plane with respect to the first camera.
	 * @param K Intrinsic calibration matrix
	 * @return Uncalibrated homography matrix
	 */
	public static DMatrixRMaj createHomography(DMatrixRMaj R, Vector3D_F64 T,
											   double d, Vector3D_F64 N,
											   DMatrixRMaj K)
	{
		DMatrixRMaj temp = new DMatrixRMaj(3,3);
		DMatrixRMaj K_inv = new DMatrixRMaj(3,3);

		DMatrixRMaj H = createHomography(R, T, d, N);

		// apply calibration matrix to R
		CommonOps_DDRM.mult(K,H,temp);

		CommonOps_DDRM.invert(K,K_inv);
		CommonOps_DDRM.mult(temp,K_inv,H);

		return H;
	}

	/**
	 * <p>
	 * Extracts the epipoles from an essential or fundamental matrix.  The epipoles are extracted
	 * from the left and right null space of the provided matrix.  Note that the found epipoles are
	 * in homogeneous coordinates.  If the epipole is at infinity then z=0
	 * </p>
	 *
	 * <p>
	 * Left: e<sub>2</sub><sup>T</sup>*F = 0 <br>
	 * Right: F*e<sub>1</sub> = 0
	 * </p>
	 *
	 * @param F Input: Fundamental or Essential 3x3 matrix.  Not modified.
	 * @param e1 Output: Right epipole in homogeneous coordinates. Can be null. Modified.
	 * @param e2 Output: Left epipole in homogeneous coordinates. Can be null. Modified.
	 */
	public static void extractEpipoles( DMatrixRMaj F , Point3D_F64 e1 , Point3D_F64 e2 ) {
		FundamentalExtractEpipoles alg = new FundamentalExtractEpipoles();
		alg.process(F,e1,e2);
	}

	/**
	 * <p>
	 * Given a fundamental matrix a pair of camera matrices P and P1' are extracted. The camera matrices
	 * are 3 by 4 and used to project a 3D homogenous point onto the image plane. These camera matrices will only
	 * be known up to a projective transform, thus there are multiple solutions, The canonical camera
	 * matrix is defined as: <br>
	 * <pre>
	 * P=[I|0] and P'= [M|-M*t] = [[e']*F + e'*v^t | lambda*e']
	 * </pre>
	 * where e' is the epipole F<sup>T</sup>e' = 0, [e'] is the cross product matrix for the enclosed vector,
	 * v is an arbitrary 3-vector and lambda is a non-zero scalar.
	 * </p>
	 *
	 * <p>
	 *     NOTE: Additional information is needed to upgrade this projective transform into a metric transform.
	 * </p>
	 * <p>
	 * Page 256 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
	 * </p>
	 *
	 * @see #extractEpipoles
	 *
	 * @param F (Input) A fundamental matrix
	 * @param e2 (Input) Left epipole of fundamental matrix, F<sup>T</sup>*e2 = 0.
	 * @param v (Input) Arbitrary 3-vector.  Just pick some value, say (0,0,0).
	 * @param lambda (Input) A non zero scalar.  Try one.
	 * @return The canonical camera (projection) matrix P' (3 by 4) Known up to a projective transform.
	 */
	public static DMatrixRMaj fundamentalToProjective(DMatrixRMaj F , Point3D_F64 e2, Vector3D_F64 v , double lambda ) {

		DMatrixRMaj crossMatrix = new DMatrixRMaj(3,3);
		GeometryMath_F64.crossMatrix(e2, crossMatrix);

		DMatrixRMaj outer = new DMatrixRMaj(3,3);
		GeometryMath_F64.outerProd(e2,v,outer);

		DMatrixRMaj KR = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(crossMatrix, F, KR);
		CommonOps_DDRM.add(KR, outer, KR);

		DMatrixRMaj P = new DMatrixRMaj(3,4);
		CommonOps_DDRM.insert(KR,P,0,0);

		P.set(0,3,lambda*e2.x);
		P.set(1,3,lambda*e2.y);
		P.set(2,3,lambda*e2.z);

		return P;
	}

	/**
	 * <p>
	 * Given a fundamental matrix a pair of camera matrices P0 and P1 can be extracted. Same
	 * {@link #fundamentalToProjective(DMatrixRMaj, Point3D_F64, Vector3D_F64, double)} but with the suggested values
	 * for all variables filled in for you.
	 * </p>
	 * @param F (Input) Fundamental Matrix
	 * @return The canonical camera (projection) matrix P' (3 by 4) Known up to a projective transform.
	 */
	public static DMatrixRMaj fundamentalToProjective(DMatrixRMaj F ) {
		Point3D_F64 e2 = new Point3D_F64();
		MultiViewOps.extractEpipoles(F, null, e2);
		return MultiViewOps.fundamentalToProjective(F, e2, new Vector3D_F64(0, 0, 0), 1);
	}

	/**
	 * <p>
	 * Decomposes a camera matrix P=A*[R|T], where A is an upper triangular camera calibration
	 * matrix, R is a rotation matrix, and T is a translation vector.
	 *
	 * <ul>
	 * <li> NOTE: There are multiple valid solutions to this problem and only one solution is returned.
	 * <li> NOTE: The camera center will be on the plane at infinity.
	 * </ul>
	 * </p>
	 *
	 * @param P Input: Camera matrix, 3 by 4
	 * @param K Output: Camera calibration matrix, 3 by 3.
	 * @param pose Output: The rotation and translation.
	 */
	public static void decomposeCameraMatrix(DMatrixRMaj P, DMatrixRMaj K, Se3_F64 pose) {
		DMatrixRMaj KR = new DMatrixRMaj(3,3);
		CommonOps_DDRM.extract(P, 0, 3, 0, 3, KR, 0, 0);

		QRDecomposition<DMatrixRMaj> qr = DecompositionFactory_DDRM.qr(3, 3);

		if( !CommonOps_DDRM.invert(KR) )
			throw new RuntimeException("Inverse failed!  Bad input?");

		if( !qr.decompose(KR) )
			throw new RuntimeException("QR decomposition failed!  Bad input?");

		DMatrixRMaj U = qr.getQ(null,false);
		DMatrixRMaj B = qr.getR(null, false);

		if( !CommonOps_DDRM.invert(U,pose.getR()) )
			throw new RuntimeException("Inverse failed!  Bad input?");

		Point3D_F64 KT = new Point3D_F64(P.get(0,3),P.get(1,3),P.get(2,3));
		GeometryMath_F64.mult(B, KT, pose.getT());

		if( !CommonOps_DDRM.invert(B,K) )
			throw new RuntimeException("Inverse failed!  Bad input?");

		CommonOps_DDRM.scale(1.0/K.get(2,2),K);
	}

	/**
	 * Decomposes an essential matrix into the rigid body motion which it was constructed from.  Due to ambiguities
	 * there are four possible solutions.  See {@link DecomposeEssential} for the details.  The correct solution can
	 * be found using triangulation and the positive depth constraint, e.g. the objects must be in front of the camera
	 * to be seen.  Also note that the scale of the translation is lost, even with perfect data.
	 *
	 * @see DecomposeEssential
	 *
	 * @param E An essential matrix.
	 * @return Four possible motions
	 */
	public static List<Se3_F64> decomposeEssential( DMatrixRMaj E ) {
		DecomposeEssential d = new DecomposeEssential();

		d.decompose(E);

		return d.getSolutions();
	}

	/**
	 * Decomposes a homography matrix that's in Euclidean space (computed from features in normalized image coordinates).
	 * The homography is defined as H = (R + (1/d)*T*N<sup>T</sup>), where R is a 3x3 rotation matrix,
	 * d is the distance of the plane, N is the plane's normal (unit vector), T is the translation vector.  If
	 * the homography is from view 'a' to 'b' then transform (R,T) will be from reference 'a' to 'b'.  Note that the
	 * returned 'T' is divided by 'd'.
	 *
	 * @see DecomposeHomography
	 *
	 * @param H Homography in Euclidean space
	 * @return The set of four possible solutions. First param: motion (R,T).  Second param: plane normal vector.
	 */
	public static List<Tuple2<Se3_F64,Vector3D_F64>> decomposeHomography( DMatrixRMaj H ) {
		DecomposeHomography d = new DecomposeHomography();

		d.decompose(H);

		List<Vector3D_F64> solutionsN = d.getSolutionsN();
		List<Se3_F64> solutionsSe = d.getSolutionsSE();

		List<Tuple2<Se3_F64,Vector3D_F64>> ret = new ArrayList<>();
		for( int i = 0; i < 4; i++ ) {
			ret.add(new Tuple2<>(solutionsSe.get(i), solutionsN.get(i)));
		}


		return ret;
	}

	/**
	 * <p>Computes symmetric Euclidean error for each observation and puts it into the storage. If the homography
	 * projects the point into the plane at infinity (z=0) then it is skipped</p>
	 *
	 * error[i] = (H*x1 - x2')**2 + (inv(H)*x2 - x1')**2<br>
	 *
	 * @param observations (Input) observations
	 * @param H (Input) Homography
	 * @param H_inv (Input) Inverse of homography. if null it will be computed internally
	 * @param storage (Output) storage for found errors
	 */
	public static void errorsHomographySymm(List<AssociatedPair> observations ,
											DMatrixRMaj H ,
											@Nullable DMatrixRMaj H_inv ,
											GrowQueue_F64 storage )
	{
		storage.reset();
		if( H_inv == null )
			H_inv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(H,H_inv);

		Point3D_F64 tmp = new Point3D_F64();

		for (int i = 0; i < observations.size(); i++) {
			AssociatedPair p = observations.get(i);

			double dx,dy;
			double error = 0;

			GeometryMath_F64.mult(H,p.p1,tmp);
			if( Math.abs(tmp.z) <= UtilEjml.EPS )
				continue;
			dx = p.p2.x - tmp.x/tmp.z;
			dy = p.p2.y - tmp.y/tmp.z;
			error += dx*dx + dy*dy;

			GeometryMath_F64.mult(H_inv,p.p2,tmp);
			if( Math.abs(tmp.z) <= UtilEjml.EPS )
				continue;
			dx = p.p1.x - tmp.x/tmp.z;
			dy = p.p1.y - tmp.y/tmp.z;
			error += dx*dx + dy*dy;

			storage.add(error);
		}
	}

	/**
	 * Transfers a point from the first view to the third view using a plane induced by
	 * a line in the second view.
	 * @param x1 (Input) point (pixel) in first view
	 * @param l2 (Input) line in second view
	 * @param T (Input) Trifocal tensor
	 * @param x3 (Output) Induced point (pixel) in third view. Homogenous coordinates.
	 * @return induced point.
	 */
	public static Point3D_F64 transfer13(  TrifocalTensor T , Point2D_F64 x1 , Vector3D_F64 l2 ,
										  @Nullable Point3D_F64 x3 )
	{
		if( x3 == null )
			x3 = new Point3D_F64();

		GeometryMath_F64.multTran(T.T1,l2,x3);
		// storage solution here to avoid the need to declare a temporary variable
		double xx = x3.x * x1.x;
		double yy = x3.y * x1.x;
		double zz = x3.z * x1.x;
		GeometryMath_F64.multTran(T.T2,l2,x3);
		xx += x3.x * x1.y;
		yy += x3.y * x1.y;
		zz += x3.z * x1.y;
		GeometryMath_F64.multTran(T.T3,l2,x3);
		x3.x = xx + x3.x;
		x3.y = yy + x3.y;
		x3.z = zz + x3.z;

		return x3;
		// Commented out code is closer to tensor notation. The above was derived from it
//		for (int i = 0; i < 3; i++) {
//			DMatrixRMaj t = T.T1;
//
//			double vx;
//			switch( i ) {
//				case 0: vx = x.x; break;
//				case 1: vx = x.y; break;
//				case 2: vx = 1; break;
//				default: throw new RuntimeException("Egads");
//			}

//			sumX += vx*(l.x*t.get(0,0)+l.y*t.get(1,0)+l.z*t.get(2,0));
//			sumY += vx*(l.x*t.get(0,1)+l.y*t.get(1,1)+l.z*t.get(2,1));
//			sumZ += vx*(l.x*t.get(0,2)+l.y*t.get(1,2)+l.z*t.get(2,2));
//		}
	}

	/**
	 * Transfers a point from the first view to the third view using the observed location in the second view
	 * @param x1 (Input) point (pixel) in first view
	 * @param x2 (Input) point (pixel) in second view
	 * @param T (Input) Trifocal tensor
	 * @param x3 (Output) Induced point (pixel) in third view. Homogenous coordinates.
	 * @return induced point.
	 */
	public static Point3D_F64 transfer13(  TrifocalTensor T , Point2D_F64 x1 , Point2D_F64 x2 ,
										   @Nullable Point3D_F64 x3 )
	{
		if( x3 == null )
			x3 = new Point3D_F64();

		TrifocalTransfer transfer = new TrifocalTransfer();
		transfer.setTrifocal(T);
		transfer.transfer13(x1.x,x1.y,x2.x,x2.y,x3);

		return x3;
	}

	/**
	 * Transfers a point from the first view to the second view using a plane induced by
	 * a line in the third view.
	 * @param x1 (Input) point (pixel) in first view
	 * @param l3 (Input) line in third view
	 * @param T (Input) Trifocal tensor
	 * @param x2 (Output) Induced point (pixel) in second view. Homogenous coordinates.
	 * @return induced point.
	 */
	public static Point3D_F64 transfer12(  TrifocalTensor T , Point2D_F64 x1 , Vector3D_F64 l3 ,
										   @Nullable Point3D_F64 x2 ) {
		if (x2 == null)
			x2 = new Point3D_F64();

		GeometryMath_F64.mult(T.T1, l3, x2);
		// storage solution here to avoid the need to declare a temporary variable
		double xx = x2.x * x1.x;
		double yy = x2.y * x1.x;
		double zz = x2.z * x1.x;
		GeometryMath_F64.mult(T.T2, l3, x2);
		xx += x2.x * x1.y;
		yy += x2.y * x1.y;
		zz += x2.z * x1.y;
		GeometryMath_F64.mult(T.T3, l3, x2);
		x2.x = xx + x2.x;
		x2.y = yy + x2.y;
		x2.z = zz + x2.z;

		return x2;
	}

	/**
	 * Transfers a point from the first view to the third view using the observed location in the second view
	 * @param x1 (Input) point (pixel) in first view
	 * @param x3 (Input) point (pixel) in third view
	 * @param T (Input) Trifocal tensor
	 * @param x2 (Output) Induced point (pixel) in second view. Homogenous coordinates.
	 * @return induced point.
	 */
	public static Point3D_F64 transfer12(  TrifocalTensor T , Point2D_F64 x1 , Point2D_F64 x3 ,
										   @Nullable Point3D_F64 x2 )
	{
		if( x2 == null )
			x2 = new Point3D_F64();

		TrifocalTransfer transfer = new TrifocalTransfer();
		transfer.setTrifocal(T);
		transfer.transfer12(x1.x,x1.y,x3.x,x3.y,x2);

		return x2;
	}
}
