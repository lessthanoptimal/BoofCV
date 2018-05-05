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

package boofcv.alg.fiducial.square;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.filter.binary.BinaryContourHelper;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.RefineEpipolar;
import boofcv.alg.distort.*;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.*;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.ConvertFloatType;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.ops.ConvertDMatrixStruct;

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
 * Must call {@link #configure} before it can process an image.
 * </p>
 *
 * <p>
 * Target orientation. Corner 0 = (-r,r), 1 = (r,r) , 2 = (r,-r) , 3 = (-r,-r).
 * </p>
 *
 * @author Peter Abeles
 */
// TODO create unit test for bright object
public abstract class BaseDetectFiducialSquare<T extends ImageGray<T>> {

	// Storage for the found fiducials
	private FastQueue<FoundFiducial> found = new FastQueue<>(FoundFiducial.class, true);

	// converts input image into a binary image
	InputToBinary<T> inputToBinary;
	// Detects the squares
	DetectPolygonBinaryGrayRefine<T> squareDetector;

	// Helps adjust the binary image for input into the contour finding algorithm
	BinaryContourHelper contourHelper;

	// image with lens and perspective distortion removed from it
	GrayF32 square;

	// Used to compute/remove perspective distortion
	private Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomographyDLT(true);
	private RefineEpipolar refineHomography = FactoryMultiView.refineHomography(1e-4,100, EpipolarError.SAMPSON);
	private DMatrixRMaj H = new DMatrixRMaj(3,3);
	private DMatrixRMaj H_refined = new DMatrixRMaj(3,3);
	private Homography2D_F64 H_fixed = new Homography2D_F64();
	private List<AssociatedPair> pairsRemovePerspective = new ArrayList<>();
	private ImageDistort<T,GrayF32> removePerspective;
	private PointTransformHomography_F32 transformHomography = new PointTransformHomography_F32();

	private Point2Transform2_F64 undistToDist = new DoNothing2Transform2_F64();

	// How wide the border is relative to the fiducial's total width
	protected double borderWidthFraction;
	// the minimum fraction of border pixels which must be black for it to be considered a fiducial
	private double minimumBorderBlackFraction;

	// Storage for results of fiducial reading
	private Result result = new Result();

	// type of input image
	private Class<T> inputType;

	// Smallest allowed aspect ratio between the smallest and largest side in a polygon
	private double thresholdSideRatio = 0.05;

	// verbose debugging output
	protected boolean verbose = false;

	/**
	 * Configures the detector.
	 * @param inputToBinary Converts input image into a binary image
	 * @param squareDetector Detects the quadrilaterals in the image
	 * @param binaryCopy If true a copy is created of the binary image and it's not modified.
	 * @param borderWidthFraction Fraction of the fiducial's width that the border occupies. 0.25 is recommended.
	 * @param minimumBorderBlackFraction Minimum fraction of pixels inside the border which must be black.  Try 0.65
	 * @param squarePixels  Number of pixels wide the undistorted square image of the fiducial's interior is.
	 *                      This will include the black border.
	 * @param inputType Type of input image it's processing
	 */
	protected BaseDetectFiducialSquare(InputToBinary<T> inputToBinary,
									   DetectPolygonBinaryGrayRefine<T> squareDetector,
									   boolean binaryCopy,
									   double borderWidthFraction, double minimumBorderBlackFraction,
									   int squarePixels,
									   Class<T> inputType) {

		squareDetector.getDetector().setOutputClockwise(false);
		squareDetector.getDetector().setConvex(true);
		squareDetector.getDetector().setNumberOfSides(4,4);

		if( borderWidthFraction <= 0 || borderWidthFraction >= 0.5 )
			throw new RuntimeException("Border width fraction must be 0 < x < 0.5");

		this.borderWidthFraction = borderWidthFraction;
		this.minimumBorderBlackFraction = minimumBorderBlackFraction;

		this.inputToBinary = inputToBinary;
		this.squareDetector = squareDetector;
		this.inputType = inputType;
		this.square = new GrayF32(squarePixels,squarePixels);

		for (int i = 0; i < 4; i++) {
			pairsRemovePerspective.add(new AssociatedPair());
		}

		// this combines two separate sources of distortion together so that it can be removed in the final image which
		// is sent to fiducial decoder
		InterpolatePixelS<T> interp = FactoryInterpolation.nearestNeighborPixelS(inputType);
		interp.setBorder(FactoryImageBorder.single(inputType, BorderType.EXTENDED));
		removePerspective = FactoryDistort.distortSB(false, interp, GrayF32.class);

		// if no camera parameters is specified default to this
		removePerspective.setModel(new PointToPixelTransform_F32(transformHomography));

		BinaryContourFinder contourFinder = squareDetector.getDetector().getContourFinder();
		contourHelper = new BinaryContourHelper(contourFinder,binaryCopy);
	}

	/**
	 * Specifies the image's intrinsic parameters and target size
	 *
	 * @param distortion Lens distortion
	 * @param width Image width
	 * @param height Image height
	 * @param cache If there's lens distortion should it cache the transforms?  Speeds it up by about 12%.  Ignored
	 *              if no lens distortion
	 */
	public void configure(LensDistortionNarrowFOV distortion, int width , int height , boolean cache ) {
		Point2Transform2_F32 pointSquareToInput;
		Point2Transform2_F32 pointDistToUndist = distortion.undistort_F32(true,true);
		Point2Transform2_F32 pointUndistToDist = distortion.distort_F32(true,true);
		PixelTransform2_F32 distToUndist = new PointToPixelTransform_F32(pointDistToUndist);
		PixelTransform2_F32 undistToDist = new PointToPixelTransform_F32(pointUndistToDist);

		if( cache ) {
			distToUndist = new PixelTransformCached_F32(width, height, distToUndist);
			undistToDist = new PixelTransformCached_F32(width, height, undistToDist);
		}

		squareDetector.setLensDistortion(width, height,distToUndist,undistToDist);

		pointSquareToInput = new SequencePoint2Transform2_F32(transformHomography,pointUndistToDist);

		// provide intrinsic camera parameters
		PixelTransform2_F32 squareToInput= new PointToPixelTransform_F32(pointSquareToInput);
		removePerspective.setModel(squareToInput);

		this.undistToDist = distortion.distort_F64(true,true);
	}

	private Polygon2D_F64 interpolationHack = new Polygon2D_F64(4);

	List<Polygon2D_F64> candidates = new ArrayList<>();
	List<DetectPolygonFromContour.Info> candidatesInfo = new ArrayList<>();

	/**
	 * Examines the input image to detect fiducials inside of it
	 *
	 * @param gray Undistorted input image
	 */
	public void process( T gray ) {
		configureContourDetector(gray);

		contourHelper.reshape(gray.width,gray.height);

		inputToBinary.process(gray,contourHelper.withoutPadding());
		squareDetector.process(gray,contourHelper.padded());
		squareDetector.refineAll();
		// These are in undistorted pixels
		squareDetector.getPolygons(candidates,candidatesInfo);

		found.reset();

		if( verbose ) System.out.println("---------- Got Polygons! "+candidates.size());

		for (int i = 0; i < candidates.size(); i++) {
			// compute the homography from the input image to an undistorted square image
			// If lens distortion has been specified this polygon will be in undistorted pixels
			Polygon2D_F64 p = candidates.get(i);
//			System.out.println(i+"  processing...  "+p.areaSimple()+" at "+p.get(0));

			// sanity check before processing
			if( !checkSideSize(p) ) {
				if( verbose ) System.out.println("  rejected side aspect ratio or size");
				continue;
			}

			// REMOVE EVENTUALLY  This is a hack around how interpolation is performed
			// Using a surface integral instead would remove the need for this.  Basically by having it start
			// interpolating from the lower extent it samples inside the image more
			// A good unit test to see if this hack is no longer needed is to rotate the order of the polygon and
			// see if it returns the same undistorted image each time
			double best=Double.MAX_VALUE;
			for (int j = 0; j < 4; j++) {
				double found = p.get(0).normSq();
				if( found < best ) {
					best = found;
					interpolationHack.set(p);
				}
				UtilPolygons2D_F64.shiftDown(p);
			}

			p.set(interpolationHack);

			// remember, visual clockwise isn't the same as math clockwise, hence
			// counter clockwise visual to the clockwise quad
			pairsRemovePerspective.get(0).set(0, 0, p.get(0).x, p.get(0).y);
			pairsRemovePerspective.get(1).set( square.width ,      0        , p.get(1).x , p.get(1).y );
			pairsRemovePerspective.get(2).set( square.width , square.height , p.get(2).x , p.get(2).y );
			pairsRemovePerspective.get(3).set( 0            , square.height , p.get(3).x , p.get(3).y );

			if( !computeHomography.process(pairsRemovePerspective,H) ) {
				if( verbose ) System.out.println("  rejected initial homography");
				continue;
			}

			// refine homography estimate
			if( !refineHomography.fitModel(pairsRemovePerspective,H,H_refined) ) {
				if( verbose ) System.out.println("  rejected refine homography");
				continue;
			}

			// pass the found homography onto the image transform
			ConvertDMatrixStruct.convert(H_refined,H_fixed);
			ConvertFloatType.convert(H_fixed, transformHomography.getModel());

			// TODO Improve how perspective is removed
			// The current method introduces artifacts.  If the "square" is larger
			// than the detected region and bilinear interpolation is used then pixels outside will// influence the
			// value of pixels inside and shift things over.  this is all bad

			// remove the perspective distortion and process it
			removePerspective.apply(gray, square);

			DetectPolygonFromContour.Info info = candidatesInfo.get(i);

			// see if the black border is actually black
			if( minimumBorderBlackFraction > 0 ) {
				double pixelThreshold = (info.edgeInside + info.edgeOutside) / 2;
				double foundFraction = computeFractionBoundary((float) pixelThreshold);
				if( foundFraction < minimumBorderBlackFraction ) {
					if( verbose ) System.out.println("  rejected black border fraction "+foundFraction);
					continue;
				}
			}
			if( processSquare(square,result,info.edgeInside,info.edgeOutside)) {
				prepareForOutput(p,result);

				if( verbose ) System.out.println("  accepted!");
			} else {
				if( verbose ) System.out.println("  rejected process square");
			}
		}
	}

	/**
	 * Sanity check the polygon based on the size of its sides to see if it could be a fiducial that can
	 * be decoded
	 */
	private boolean checkSideSize( Polygon2D_F64 p ) {
		double max=0,min=Double.MAX_VALUE;

		for (int i = 0; i < p.size(); i++) {
			double l = p.getSideLength(i);
			max = Math.max(max,l);
			min = Math.min(min,l);
		}

		// See if a side is too small to decode
		if( min < 10 )
			return false;

		// see if it's under extreme perspective distortion and unlikely to be readable
		return !(min / max < thresholdSideRatio);
	}

	/**
	 * Configures the contour detector based on the image size. Setting a maximum contour and turning off recording
	 * of inner contours and improve speed and reduce the memory foot print significantly.
	 */
	private void configureContourDetector(T gray) {
		// determine the maximum possible size of a square based on image size
		int maxContourSize = Math.min(gray.width,gray.height)*4;
		BinaryContourFinder contourFinder = squareDetector.getDetector().getContourFinder();
		contourFinder.setMaxContour(maxContourSize);
		contourFinder.setSaveInnerContour(false);
	}

	/**
	 * Computes the fraction of pixels inside the image border which are black
	 * @param pixelThreshold Pixel's less than this value are considered black
	 * @return fraction of border that's black
	 */
	protected double computeFractionBoundary( float pixelThreshold ) {
		// TODO ignore outer pixels from this computation.  Will require 8 regions (4 corners + top/bottom + left/right)
		final int w = square.width;
		int radius = (int) (w * borderWidthFraction);

		int innerWidth = w-2*radius;
		int total = w*w - innerWidth*innerWidth;
		int count = 0;
		for (int y = 0; y < radius; y++) {
			int indexTop = y*w;
			int indexBottom = (w - radius + y)*w;

			for (int x = 0; x < w; x++) {
				if( square.data[indexTop++] < pixelThreshold )
					count++;
				if( square.data[indexBottom++] < pixelThreshold )
					count++;
			}
		}

		for (int y = radius; y < w-radius; y++) {
			int indexLeft = y*w;
			int indexRight = y*w + w - radius;

			for (int x = 0; x < radius; x++) {
				if( square.data[indexLeft++] < pixelThreshold )
					count++;
				if( square.data[indexRight++] < pixelThreshold )
					count++;
			}
		}

		return count/(double)total;
	}

	/**
	 * Takes the found quadrilateral and the computed 3D information and prepares it for output
	 */
	private void prepareForOutput(Polygon2D_F64 imageShape, Result result) {
		// the rotation estimate, apply in counter clockwise direction
		// since result.rotation is a clockwise rotation in the visual sense, which
		// is CCW on the grid
		int rotationCCW = (4-result.rotation)%4;
		for (int j = 0; j < rotationCCW; j++) {
			UtilPolygons2D_F64.shiftUp(imageShape);
		}

		// save the results for output
		FoundFiducial f = found.grow();
		f.id = result.which;

		for (int i = 0; i < 4; i++) {
			Point2D_F64 a = imageShape.get(i);
			undistToDist.compute(a.x, a.y, f.distortedPixels.get(i));
		}
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
	 * @param edgeInside Average pixel value along edge inside
	 * @param edgeOutside Average pixel value along edge outside
	 * @return true if the square matches a known target.
	 */
	protected abstract boolean processSquare(GrayF32 square , Result result , double edgeInside , double edgeOutside  );

	/**
	 * Used to toggle on/off verbose debugging information
	 * @param verbose true for verbose output
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public DetectPolygonBinaryGrayRefine<T> getSquareDetector() {
		return squareDetector;
	}

	public GrayU8 getBinary() {
		return contourHelper.withoutPadding();
	}

	public Class<T> getInputType() {
		return inputType;
	}

	public double getBorderWidthFraction() {
		return borderWidthFraction;
	}

	public double getThresholdSideRatio() {
		return thresholdSideRatio;
	}

	public void setThresholdSideRatio(double thresholdSideRatio) {
		this.thresholdSideRatio = thresholdSideRatio;
	}

	public static class Result {
		int which;
		// length of one of the sides in world units
		double lengthSide;
		// amount of clockwise rotation.  Each value = +90 degrees
		// Just to make things confusion, the rotation is done in the visual clockwise, which
		// is a counter-clockwise rotation when you look at the actual coordinates
		int rotation;
	}
}
