/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.abst.geo.RefineEpipolar;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.Zhang99DecomposeHomography;
import boofcv.alg.geo.h.HomographyLinear4;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.homography.UtilHomography;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Base class for square fiducial detectors.  Searches for quadrilaterals inside the image with a black border
 * and inner contours.  It then removes perspective and lens distortion from the candidate quadrilateral and
 * rendered onto a new image.  The just mentioned image is then passed on to the class which extends this one.
 * After being processed by the extending class, the corners are rotated to match and the 3D pose of the
 * target found.  Lens distortion is removed sparsely for performance reasons.
 * </p>
 *
 * <p>
 * Must call {@link #configure(boofcv.struct.calib.IntrinsicParameters)} before it can process an image.
 * </p>
 *
 * <p>
 * Target orientation. Corner 0 = (r,r), 1 = (r,-r) , 2 = (-r,-r) , 3 = (-r,r).
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class BaseDetectFiducialSquare<T extends ImageSingleBand> {

	// Storage for the found fiducials
	private FastQueue<FoundFiducial> found = new FastQueue<FoundFiducial>(FoundFiducial.class,true);

	// precomputed lookup table for distorted to undistorted pixel locations
//	private Point2D_I32 tableDistPixel[];

	private BinaryPolygonConvexDetector<T> squareDetector;

	// image with lens and perspective distortion removed from it
	private ImageFloat32 square;

	// Used to compute/remove distortion from perspective
	private HomographyLinear4 computeHomography = new HomographyLinear4(true);
	private RefineEpipolar refineHomography = FactoryMultiView.refineHomography(1e-4,100, EpipolarError.SAMPSON);
	private DenseMatrix64F H = new DenseMatrix64F(3,3);
	private DenseMatrix64F H_refined = new DenseMatrix64F(3,3);
	private List<AssociatedPair> pairsRemovePerspective = new ArrayList<AssociatedPair>();
	private ImageDistort<T,ImageFloat32> removePerspective;
	private PointTransformHomography_F32 transformHomography = new PointTransformHomography_F32();

	// Storage for results of fiducial reading
	private Result result = new Result();

	// type of input image
	private Class<T> inputType;

	private boolean verbose = false;

	// ----- Used to estimate 3D pose of calibration target

	// p1 is set to the corner locations in 2D target frame. [0] is top right (upper extreme)
	// and the points are added in clock-wise direction.  The center of the target is
	// the center of the coordinate system
	List<AssociatedPair> pairsPose = new ArrayList<AssociatedPair>();
	Zhang99DecomposeHomography homographyToPose = new Zhang99DecomposeHomography();

	/**
	 * Configures the detector.
	 *
	 * @param squareDetector Detects the quadrilaterals in the image
	 * @param squarePixels  Number of pixels wide the image that stores the target's detector interior is.
	 * @param inputType Type of input image it's processing
	 */
	protected BaseDetectFiducialSquare(BinaryPolygonConvexDetector<T> squareDetector,
									   int squarePixels,
									   Class<T> inputType) {

		if( squareDetector.getPolyNumberOfLines() != 4 )
			throw new IllegalArgumentException("quadDetector not configured to detect quadrilaterals");
		if( squareDetector.isOutputClockwise() )
			throw new IllegalArgumentException("output polygons needs to be counter-clockwise");

		this.squareDetector = squareDetector;
		this.inputType = inputType;
		this.square = new ImageFloat32(squarePixels,squarePixels);

		for (int i = 0; i < 4; i++) {
			pairsRemovePerspective.add(new AssociatedPair());
			pairsPose.add( new AssociatedPair());
		}

		// this combines two separate sources of distortion together so that it can be removed in the final image which
		// is sent to fiducial decoder
		removePerspective = FactoryDistort.distort(false,FactoryInterpolation.bilinearPixelS(inputType),
				FactoryImageBorder.general(inputType, BorderType.EXTENDED),ImageFloat32.class);
	}

	/**
	 * Specifies the image's intrinsic parameters and target size
	 *
	 * @param intrinsic Intrinsic parameters for the distortion free input image
	 */
	public void configure( IntrinsicParameters intrinsic) {

		if( intrinsic.isDistorted() )
			throw new IllegalArgumentException("Detector assumes distortion has been removed already");

		// add corner points in target frame.  Used to compute homography.  Target's center is at its origin
		// see comment in class JavaDoc above.  Note that the target's length is one below.  The scale factor
		// will be provided later one
		pairsPose.get(0).p1.set( 0.5,  0.5);
		pairsPose.get(1).p1.set( 0.5, -0.5);
		pairsPose.get(2).p1.set(-0.5, -0.5);
		pairsPose.get(3).p1.set(-0.5,  0.5);

		// Setup homography to camera pose estimator
		DenseMatrix64F K = new DenseMatrix64F(3,3);
		PerspectiveOps.calibrationMatrix(intrinsic, K);
		homographyToPose.setCalibrationMatrix(K);

		// provide intrinsic camera parameters
		PixelTransform_F32 squareToInput= new PointToPixelTransform_F32(transformHomography);
		removePerspective.setModel(squareToInput);
	}

	/**
	 * Examines the input image to detect fiducials inside of it
	 *
	 * @param gray Undistorted input image
	 */
	public boolean process( T gray ) {

		squareDetector.process(gray);
		FastQueue<Polygon2D_F64> candidates = squareDetector.getFound();

		found.reset();

		if( verbose ) System.out.println("---------- Got Polygons! "+candidates.size);
		// undistort the squares
		Quadrilateral_F64 q = new Quadrilateral_F64(); // todo predeclare
		for (int i = 0; i < candidates.size; i++) {
			// compute the homography from the input image to an undistorted square image
			Polygon2D_F64 p = candidates.get(i);
			UtilPolygons2D_F64.convert(p,q);

			pairsRemovePerspective.get(0).set( 0              ,    0            , q.a.x , q.a.y);
			pairsRemovePerspective.get(1).set( square.width-1 ,    0            , q.b.x , q.b.y );
			pairsRemovePerspective.get(2).set( square.width-1 , square.height-1 , q.c.x , q.c.y );
			pairsRemovePerspective.get(3).set( 0              , square.height-1 , q.d.x , q.d.y );

			if( !computeHomography.process(pairsRemovePerspective,H) ) {
				if( verbose ) System.out.println("rejected initial homography");
				continue;
			}

			// refine homography estimate
			if( !refineHomography.fitModel(pairsRemovePerspective,H,H_refined) ) {
				if( verbose ) System.out.println("rejected refine homography");
				continue;
			}

			// pass the found homography onto the image transform
			UtilHomography.convert(H_refined,transformHomography.getModel());
			// remove the perspective distortion and process it
			removePerspective.apply(gray, square);
			if( processSquare(square,result)) {
				FoundFiducial f = found.grow();
				f.index = result.which;
				f.location.set(q);

				// account for the rotation
				for (int j = 0; j < result.rotation; j++) {
					rotateClockWise(q);
				}

				// estimate position
				computeTargetToWorld(q, result.lengthSide, f.targetToSensor);
				if( verbose ) System.out.println("accepted!");
			} else {
				if( verbose ) System.out.println("rejected process square");
			}
		}
		return true;
	}

	/**
	 * Rotates the corners on the quad
	 */
	private void rotateClockWise( Quadrilateral_F64 quad ) {
		Point2D_F64 a = quad.a;
		Point2D_F64 b = quad.b;
		Point2D_F64 c = quad.c;
		Point2D_F64 d = quad.d;

		quad.a = d;
		quad.b = a;
		quad.c = b;
		quad.d = c;
	}

	/**
	 * Given observed location of corners, compute the transform from target to world frame.
	 * See code comments for correct ordering of corners in quad.
	 *
	 * @param quad (Input) Observed location of corner points in the specified order.
	 * @param lengthSide (Input) Length of a side on the square
	 * @param targetToWorld (output) transform from target to world frame.
	 */
	public void computeTargetToWorld( Quadrilateral_F64 quad , double lengthSide , Se3_F64 targetToWorld )
	{
		pairsPose.get(0).p2.set(quad.a);
		pairsPose.get(1).p2.set(quad.b);
		pairsPose.get(2).p2.set(quad.c);
		pairsPose.get(3).p2.set(quad.d);

		if( !computeHomography.process(pairsPose,H_refined) )
			throw new RuntimeException("Compute homography failed!");

		targetToWorld.set(homographyToPose.decompose(H_refined));
		GeometryMath_F64.scale(targetToWorld.getT(),lengthSide);
	}

	/**
	 * Returns list of found fiducials
	 */
	public FastQueue<FoundFiducial> getFound() {
		return found;
	}

	/**
	 * Processes the detected square and matches it to a known fiducial.  Black border
	 * is included.
	 *
	 * @param square Image of the undistorted square
	 * @param result Which target and its orientation was found
	 * @return true if the square matches a known target.
	 */
	protected abstract boolean processSquare( ImageFloat32 square , Result result );

	public BinaryPolygonConvexDetector getSquareDetector() {
		return squareDetector;
	}

	public Class<T> getInputType() {
		return inputType;
	}

	public static class Result {
		int which;
		// length of one of the sides in world units
		double lengthSide;
		// amount of clock-wise rotation.  Each value = +90 degrees
		int rotation;
	}
}
