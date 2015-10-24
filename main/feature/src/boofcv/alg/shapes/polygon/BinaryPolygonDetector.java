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

package boofcv.alg.shapes.polygon;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.shapes.edge.PolygonEdgeScore;
import boofcv.alg.shapes.polyline.MinimizeEnergyPrune;
import boofcv.alg.shapes.polyline.RefinePolyLineCorner;
import boofcv.alg.shapes.polyline.SplitMergeLineFitLoop;
import boofcv.struct.ConnectRule;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Area2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects convex polygons with the specified number of sides in an image.  Shapes are assumed to be black shapes
 * against a white background, allowing for thresholding to be used.  Subpixel refinement is done using the
 * provided implementation of {@link RefineBinaryPolygon}.
 * </p>
 *
 * Processing Steps:
 * <ol>
 * <li>First the input a gray scale image and a binarized version of it.</li>
 * <li>The contours of black blobs are found.</li>
 * <li>From the contours polygons are fitted and refined to pixel accuracy.</li>
 * <li>(Optional) Sub-pixel refinement of the polygon's edges and/or corners.</li>
 * </ol>
 *
 * <p>
 * The returned polygons will encompass the entire black polygon.  Here is a simple example in 1D. If all pixels are
 * white, but pixels ranging from 5 to 10, inclusive, then the returned boundaries would be 5.0 to 11.0.  This
 * means that coordinates 5.0 &le; x < 11.0 are all black.  11.0 is included, but note that the entire pixel 11 is white.
 * </p>
 *
 * @author Peter Abeles
 */
public class BinaryPolygonDetector<T extends ImageSingleBand> {

	// minimum size of a shape's contour as a fraction of the image width
	private double minContourFraction;
	private int minimumContour; // this is image.width*minContourFraction
	private double minimumArea; // computed from minimumContour

	// does the polygon have to be convex
	private boolean convex;

	private LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	private ImageSInt32 labeled = new ImageSInt32(1,1);

	// finds the initial polygon around a target candidate
	private SplitMergeLineFitLoop fitPolygon;

	// removes extra corners
	GrowQueue_I32 pruned = new GrowQueue_I32();
	MinimizeEnergyPrune pruner;

	// Improve the selection of corner pixels in the contour
	private RefinePolyLineCorner improveContour = new RefinePolyLineCorner(true,20);

	// Refines the estimate of the polygon's lines using a subpixel technique
	private RefineBinaryPolygon<T> refinePolygon;

	// List of all squares that it finds
	private FastQueue<Polygon2D_F64> found;

	// type of input image
	private Class<T> inputType;

	// number of lines allowed in the polygon
	private int minSides,maxSides;

	// work space for initial polygon
	private Polygon2D_F64 workPoly = new Polygon2D_F64();

	// should the order of the polygon be on clockwise order on output?
	private boolean outputClockwise;

	// storage for the contours associated with a found target.  used for debugging
	private List<Contour> foundContours = new ArrayList<Contour>();

	// transforms which can be used to handle lens distortion
	protected PixelTransform_F32 toUndistorted, toDistorted;

	boolean verbose = false;

	// used to remove false positives
	PolygonEdgeScore differenceScore;
	// should it check the edge score before?  With a chessboard pattern the initial guess is known to be very poor
	// so it should only check the edge after.  Otherwise its good to filter before optimization.
	boolean checkEdgeBefore = true;

	// helper used to customize low level behaviors internally
	private PolygonHelper helper;

	/**
	 * Configures the detector.
	 *
	 * @param minSides minimum number of sides
	 * @param maxSides maximum number of sides
	 * @param contourToPolygon Fits a crude polygon to the shape's binary contour
	 * @param differenceScore Used to remove false positives by computing the difference along the polygon's edges.
	 *                        If null then this test is skipped.
	 * @param refinePolygon (Optional) Refines the polygon's lines.  Set to null to skip step
	 * @param minContourFraction Size of minimum contour as a fraction of the input image's width.  Try 0.23
	 * @param minimumSplitFraction Minimum number of pixels allowed to split a polygon as a fraction of image width.
	 * @param outputClockwise If true then the order of the output polygons will be in clockwise order
	 * @param inputType Type of input image it's processing
	 */
	public BinaryPolygonDetector(int minSides, int maxSides,
								 SplitMergeLineFitLoop contourToPolygon,
								 PolygonEdgeScore differenceScore,
								 RefineBinaryPolygon<T> refinePolygon,
								 double minContourFraction,
								 boolean outputClockwise,
								 boolean convex,
								 double splitPenalty,
								 Class<T> inputType) {

		setNumberOfSides(minSides,maxSides);
		this.refinePolygon = refinePolygon;
		this.differenceScore = differenceScore;
		this.inputType = inputType;
		this.minContourFraction = minContourFraction;
		this.fitPolygon = contourToPolygon;
		this.outputClockwise = outputClockwise;
		this.convex = convex;

		pruner = new MinimizeEnergyPrune(splitPenalty);

		workPoly = new Polygon2D_F64(1);
		found = new FastQueue<Polygon2D_F64>(Polygon2D_F64.class,true);
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates.  The undistorted image is never explicitly created.</p>
	 *
	 * <p>
	 * WARNING: The undistorted image must have the same bounds as the distorted input image.  This is because
	 * several of the bounds checks use the image shape.  This are simplified greatly by this assumption.
	 * </p>
	 *
	 * @param width Input image width.  Used in sanity check only.
	 * @param height Input image height.  Used in sanity check only.
	 * @param toUndistorted Transform from undistorted to distorted image.
	 * @param toDistorted Transform from distorted to undistorted image.
	 */
	public void setLensDistortion( int width , int height ,
								   PixelTransform_F32 toUndistorted , PixelTransform_F32 toDistorted ) {

		this.toUndistorted = toUndistorted;
		this.toDistorted = toDistorted;

		// sanity check since I think many people will screw this up.
		RectangleLength2D_F32 rect = DistortImageOps.boundBox_F32(width, height, toUndistorted);
		float x1 = rect.x0 + rect.width;
		float y1 = rect.y0 + rect.height;

		float tol = 1e-4f;
		if( rect.getX() < -tol || rect.getY() < -tol || x1 > width+tol || y1 > height+tol ) {
			throw new IllegalArgumentException("You failed the idiot test! RTFM! The undistorted image "+
					"must be contained by the same bounds as the input distorted image");
		}

		if( refinePolygon != null ) {
			refinePolygon.setLensDistortion(width, height, toUndistorted, toDistorted);
		}

		if( differenceScore != null ) {
			differenceScore.setTransform(toDistorted);
		}
	}

	/**
	 * Examines the undistorted gray scake input image for squares.
	 *
	 * @param gray Input image
	 */
	public void process(T gray, ImageUInt8 binary) {
		if( verbose ) System.out.println("ENTER  BinaryPolygonDetector.process()");
		InputSanityCheck.checkSameShape(binary, gray);

		if( labeled.width != gray.width || labeled.height == gray.width )
			configure(gray.width,gray.height);

		found.reset();
		foundContours.clear();

		if( differenceScore != null ) {
			differenceScore.setImage(gray);
		}

		findCandidateShapes(gray, binary);
		if( verbose ) System.out.println("EXIT  BinaryPolygonDetector.process()");
	}

	/**
	 * Specifies the image's intrinsic parameters and target size
	 *
	 * @param width Width of the input image
	 * @param height Height of the input image
	 */
	private void configure( int width , int height ) {

		// resize storage images
		labeled.reshape(width, height);

		// adjust size based parameters based on image size
		this.minimumContour = (int)(width*minContourFraction);
		this.minimumArea = Math.pow(this.minimumContour /4.0,2);
	}

	/**
	 * Finds blobs in the binary image.  Then looks for blobs that meet size and shape requirements.  See code
	 * below for the requirements.  Those that remain are considered to be target candidates.
	 */
	private void findCandidateShapes( T gray , ImageUInt8 binary ) {

		int maxSidesConsider = (int)Math.ceil(maxSides*1.5);

		// stop fitting the polygon if it clearly has way too many sides
		fitPolygon.setAbortSplits(2*maxSides);

		// find binary blobs
		contourFinder.process(binary, labeled);

		// find blobs where all 4 edges are lines
		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			if( c.external.size() >= minimumContour) {
//				System.out.println("----- candidate "+c.external.size());

				// ignore shapes which touch the image border
				if( touchesBorder(c.external)) {
					if( verbose ) System.out.println("rejected polygon, touched border");
					continue;
				}

				// remove lens distortion
				if( toUndistorted != null ) {
					removeDistortionFromContour(c.external);
				}

				if( !fitPolygon.process(c.external) ) {
					if( verbose ) System.out.println("rejected polygon initial fit failed. contour size = "+c.external.size());
					continue;
				}

				GrowQueue_I32 splits = fitPolygon.getSplits();
				if( splits.size() > maxSidesConsider ) {
					if( verbose ) System.out.println("Way too many corners, "+splits.size()+". Aborting before improve. Contour size "+c.external.size());
					continue;
				}

				// Perform a local search and improve the corner placements
				if( !improveContour.fit(c.external,splits) ) {
					if( verbose ) System.out.println("rejected improve contour. contour size = "+c.external.size());
					continue;
				}

				// reduce the number of corners based on an energy model
				pruner.fit(c.external,splits,pruned);
				splits = pruned;

				// only accept polygons with the expected number of sides
				if (!expectedNumberOfSides(splits)) {
//					System.out.println("First point "+c.external.get(0));
					if( verbose ) System.out.println("rejected number of sides. "+splits.size()+"  contour "+c.external.size());
					continue;
				}

				if( helper != null ) {
					if( !helper.filterPolygon(c.external,splits) ) {
						if( verbose ) System.out.println("rejected by helper");
						continue;
					}
				}

				// convert the format of the initial crude polygon
				workPoly.vertexes.resize(splits.size());
				for (int j = 0; j < splits.size(); j++) {
					Point2D_I32 p = c.external.get( splits.get(j));
					workPoly.get(j).set(p.x,p.y);
				}

				if( helper != null ) {
					helper.adjustBeforeOptimize(workPoly);
				}

				// Filter out polygons which are not convex if requested by the user
				if( convex && !UtilPolygons2D_F64.isConvex(workPoly)) {
					if( verbose ) System.out.println("Rejected not convex");
					continue;
				}

				// make sure it's big enough
				double area = Area2D_F64.polygonSimple(workPoly);

				if( area < minimumArea ) {
					if( verbose ) System.out.println("Rejected area");
					continue;
				}

				// Test the edge quality and prune before performing an expensive optimization
				if( checkEdgeBefore && differenceScore != null && !differenceScore.validate(workPoly)) {
					if( verbose ) System.out.println("Rejected edge score, after: "+differenceScore.getAverageEdgeIntensity());
					continue;
				}

				Polygon2D_F64 refined = found.grow();
				refined.vertexes.resize(splits.size);

				boolean success;
				if( refinePolygon != null ) {
					refinePolygon.setImage(gray);
					success = refinePolygon.refine(workPoly,c.external,splits,refined);
					if( verbose && !success ) System.out.println("Rejected after refinePolygon");
				} else {
					refined.set(workPoly);
					success = true;
				}

				// test it again with the full threshold
				if( !checkEdgeBefore && differenceScore != null && !differenceScore.validate(refined)) {
					if( verbose ) System.out.println("Rejected edge score, after: "+differenceScore.getAverageEdgeIntensity());
					continue;
				}

				if( outputClockwise == refined.isCCW() )
					refined.flip();

				// refine the polygon and add it to the found list
				if( success ) {
//					System.out.println("SUCCESS!!!\n");
					c.id = found.size();
					foundContours.add(c);
				} else {
					found.removeTail();
				}
			}
		}
	}

	/**
	 * True if the number of sides found matches what it is looking for
	 */
	private boolean expectedNumberOfSides(GrowQueue_I32 splits) {
		return splits.size() >= minSides && splits.size() <= maxSides;
	}

	/**
	 * Removes lens distortion from the found contour
	 */
	private void removeDistortionFromContour(List<Point2D_I32> contour) {
		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			toUndistorted.compute(p.x,p.y);
			// round to minimize error
			p.x = Math.round(toUndistorted.distX);
			p.y = Math.round(toUndistorted.distY);
		}
	}


	/**
	 * Checks to see if some part of the contour touches the image border.  Most likely cropped
	 */
	protected final boolean touchesBorder( List<Point2D_I32> contour ) {
		int endX = labeled.width-1;
		int endY = labeled.height-1;

		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			if( p.x == 0 || p.y == 0 || p.x == endX || p.y == endY )
			{
				return true;
			}
		}

		return false;
	}

	public void setHelper(PolygonHelper helper) {
		this.helper = helper;
	}

	public boolean isConvex() {
		return convex;
	}

	public void setConvex(boolean convex) {
		this.convex = convex;
	}

	public ImageSInt32 getLabeled() {
		return labeled;
	}

	public boolean isOutputClockwise() {
		return outputClockwise;
	}

	public FastQueue<Polygon2D_F64> getFoundPolygons() {
		return found;
	}

	public List<Contour> getUsedContours(){return foundContours;}

	public List<Contour> getAllContours(){return contourFinder.getContours().toList();}

	public Class<T> getInputType() {
		return inputType;
	}

	public void setNumberOfSides( int min , int max ) {
		if( min < 3 )
			throw new IllegalArgumentException("The min must be >= 3");
		if( max < min )
			throw new IllegalArgumentException("The max must be >= the min");

		this.minSides = min;
		this.maxSides = max;
	}

	public int getMinimumSides() {
		return minSides;
	}

	public int getMaximumSides() {
		return maxSides;
	}


	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isCheckEdgeBefore() {
		return checkEdgeBefore;
	}

	public RefineBinaryPolygon<T> getRefinePolygon() {
		return refinePolygon;
	}

	public void setRefinePolygon(RefineBinaryPolygon<T> refinePolygon) {
		this.refinePolygon = refinePolygon;
	}

	/**
	 * If set to true it will prune using polygons using their edge intensity before sub-pixel optimization.
	 * This should only be set to false if the initial edge is known to be off by a bit, like with a chessboard.
	 * @param checkEdgeBefore true for checking before and false for after.
	 */
	public void setCheckEdgeBefore(boolean checkEdgeBefore) {
		this.checkEdgeBefore = checkEdgeBefore;
	}
}
