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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.shapes.SplitMergeLineFitLoop;
import boofcv.alg.shapes.corner.SubpixelSparseCornerFit;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Area2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Detects convex polygons with the specified number of sides in an image.  Shapes are assumed to be black shapes
 * against a white background, allowing for thresholding to be used.
 * </p>
 *
 * Processing Steps:
 * <ol>
 * <li>First the input gray scale image is converted into a binary image.</li>
 * <li>The contours of black blobs are found.</li>
 * <li>From the contours a polygons are fitted.</li>
 * <li>From the polygons quadrilaterals are fitted.</li>
 * <li>Then a sub-pixel algorithm aligns the quadrilateral to its edge</li>
 * </ol>
 * The last step assumes that the lines of the shape are straight.  This is a reasonable assumption
 * when lens distortion has been removed.  The other steps are fairly tolerant to distortion.
 *
 * TODO discuss subpixel
 *
 * <p>
 * NOTE: A binary image is not processed as input because the gray-scale image is used in the sub-pixel line/corner
 * refinement.
 * </p>
 *
 * @author Peter Abeles
 */
public class BinaryPolygonConvexDetector<T extends ImageSingleBand> {

	// Converts the input image into a binary one
	private InputToBinary<T> thresholder;

	// private maximum distance a point can deviate from the edge of the contour and not
	// cause a new line to formed as a fraction of the contour's size
	private double splitDistanceFraction;

	// minimum size of a shape's contour as a fraction of the image width
	private double minContourFraction;
	private int minimumContour; // this is image.width*minContourFraction
	private double minimumArea; // computed from minimumContour

	// Storage for the binary image
	private ImageUInt8 binary = new ImageUInt8(1,1);

	private LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	private ImageSInt32 labeled = new ImageSInt32(1,1);

	// finds the initial polygon around a target candidate
	private SplitMergeLineFitLoop fitPolygon;

	// Improve the selection of corner pixels in the contour
	private RefinePolygonContourLoop improveContour = new RefinePolygonContourLoop(20);

	// Refines the estimate of the polygon's lines using a subpixel technique
	private RefinePolygonLineToImage<T> refineLine;
	// Refines the estimate of the polygon's corners using a subpixel technique
	private SubpixelSparseCornerFit<T> refineCorner;

	// List of all squares that it finds
	private FastQueue<Polygon2D_F64> found;

	// type of input image
	private Class<T> inputType;

	// number of lines in the polygon
	private int polyNumberOfLines;

	// work space for initial polygon
	private Polygon2D_F64 workPoly = new Polygon2D_F64();

	// should the order of the polygon be on clockwise order on output?
	private boolean outputClockwise;

	// storage for the contours associated with a found target.  used for debugging
	private List<Contour> foundContours = new ArrayList<Contour>();

	/**
	 * Configures the detector.
	 *
	 * @param polygonSides Number of lines in the polygon
	 * @param thresholder Converts the input image into a binary one
	 * @param contourToPolygon Fits a crude polygon to the shape's binary contour
	 * @param refineLine Refines the polygon's lines.  Set to null to skip step
	 * @param refineCorner Refines the polygon's corners.  Set to null to skip step
	 * @param minContourFraction Size of minimum contour as a fraction of the input image's width.  Try 0.23
	 * @param splitDistanceFraction Number of pixels as a fraction of contour length to split a new line in
	 *                             SplitMergeLineFitLoop.  Try 0.03
	 * @param outputClockwise If true then the order of the output polygons will be in clockwise order
	 * @param inputType Type of input image it's processing
	 */
	public BinaryPolygonConvexDetector(final int polygonSides,
									   InputToBinary<T> thresholder,
									   SplitMergeLineFitLoop contourToPolygon,
									   RefinePolygonLineToImage<T> refineLine,
									   SubpixelSparseCornerFit<T> refineCorner,
									   double minContourFraction,
									   double splitDistanceFraction,
									   boolean outputClockwise,
									   Class<T> inputType) {

		if( refineLine != null ) {
			if (polygonSides != refineLine.getNumberOfSides())
				throw new IllegalArgumentException("Miss matched number of lines with refineLine");
			this.refineLine = refineLine;
		}
		if( refineCorner != null ) {
			this.refineCorner = refineCorner;
		}

		this.polyNumberOfLines = polygonSides;
		this.thresholder = thresholder;
		this.inputType = inputType;
		this.minContourFraction = minContourFraction;
		this.fitPolygon = contourToPolygon;
		this.splitDistanceFraction = splitDistanceFraction;
		this.outputClockwise = outputClockwise;

		workPoly = new Polygon2D_F64(polygonSides);
		found = new FastQueue<Polygon2D_F64>(Polygon2D_F64.class,true) {
			@Override
			protected Polygon2D_F64 createInstance() {
				return new Polygon2D_F64(polygonSides);
			}
		};
	}

	/**
	 * Specifies the image's intrinsic parameters and target size
	 *
	 * @param width Width of the input image
	 * @param height Height of the input image
	 */
	// TODO move to process
	public void configure( int width , int height ) {

		// resize storage images
		binary.reshape(width, height);
		labeled.reshape(width, height);

		// adjust size based parameters based on image size
		this.minimumContour = (int)(width*minContourFraction);
		this.minimumArea = Math.pow(this.minimumContour /4.0,2);
	}

	/**
	 * Examines the undistorted gray scake input image for squares.
	 *
	 * @param gray Input image
	 */
	public void process( T gray ) {
		if( binary.width == 0 || binary.height == 0 )
			throw new RuntimeException("Did you call configure() yet? zero width/height");

		found.reset();
		foundContours.clear();

		thresholder.process(gray, binary);

		// Find quadrilaterals that could be fiducials
		findCandidateShapes(gray);
	}

	/**
	 * Finds blobs in the binary image.  Then looks for blobs that meet size and shape requirements.  See code
	 * below for the requirements.  Those that remain are considered to be target candidates.
	 */
	private void findCandidateShapes( T gray ) {
		// find binary blobs
		contourFinder.process(binary, labeled);

		// find blobs where all 4 edges are lines
		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			if( c.external.size() >= minimumContour) {
				// ignore shapes which touch the image border
				if( touchesBorder(c.external))
					continue;

				// dynamically set split distance based on polygon's size
				double splitTolerance = splitDistanceFraction*c.external.size();
				fitPolygon.setToleranceSplit(splitTolerance);
				fitPolygon.process(c.external);

				GrowQueue_I32 splits = fitPolygon.getSplits();

				// only accept polygons with the expected number of sides
				if( splits.size() != polyNumberOfLines )
					continue;

				// further improve the selection of corner points
				improveContour.fit(c.external,splits);

				// convert the format of the initial crude polygon
				for (int j = 0; j < polyNumberOfLines; j++) {
					Point2D_I32 p = c.external.get( splits.get(j));
					workPoly.get(j).set(p.x,p.y);
				}

				if( workPoly.isCCW() ) {
					UtilPolygons2D_F64.reverseOrder(workPoly);
				}
				// this only supports convex polygons
				if( !UtilPolygons2D_F64.isConvex(workPoly))
					continue;

				// make sure it's big enough
				double area = Area2D_F64.polygonConvex(workPoly);
				if( area < minimumArea )
					continue;

				// refine the polygon and add it to the found list
				if( refinePolygon(gray) ) {
					c.id = found.size();
					foundContours.add(c);
				}
			}
		}
	}

	/**
	 * Refine using corner then line or any combination, including none.  Put into list
	 * of found polygons if nothing goe wrong.
	 */
	private boolean refinePolygon( T gray ) {
		// refine the estimate
		Polygon2D_F64 refined = found.grow();
		refined.set(workPoly);

		boolean failed = false;

		if( refineCorner != null ) {
			refineCorner.initialize(gray);
			if( !refinePolygonCorners(workPoly,refined) )
				failed = true;
		}
		if( !failed && refineLine != null ) {
			refineLine.initialize(gray);
			workPoly.set(refined);
			if( !refineLine.refine(workPoly, refined) ) {
				failed = true;
			}
		}
		// or don't refine the estimate
		if( refineCorner == null && refineLine == null ) {
			refined.set(workPoly);
		}

		// if it failed, discard
		if( failed ) {
			found.removeTail();
		} else {
			if( !outputClockwise )
				UtilPolygons2D_F64.reverseOrder(refined);
		}
		return !failed;
	}

	/**
	 * Refines each vertex in the polygon independently
	 * @return true if no issues and false if it failed
	 */
	private boolean refinePolygonCorners(Polygon2D_F64 q, Polygon2D_F64 refined) {
		for (int i = 0; i < q.size(); i++) {
			Point2D_F64 v = q.get(i);
			if( !refineCorner.refine(v.x,v.y,refined.get(i)) )
				return false;
		}
		return true;
	}

	/**
	 * Checks to see if some part of the contour touches the image border.  Most likely cropped
	 */
	protected final boolean touchesBorder( List<Point2D_I32> contour ) {
		int endX = binary.width-1;
		int endY = binary.height-1;

		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			if( p.x == 0 || p.y == 0 || p.x == endX || p.y == endY )
			{
				return true;
			}
		}

		return false;
	}

	public boolean isOutputClockwise() {
		return outputClockwise;
	}

	public FastQueue<Polygon2D_F64> getFound() {
		return found;
	}

	public List<Contour> getFoundContours(){return foundContours;}

	public ImageUInt8 getBinary() {
		return binary;
	}

	public Class<T> getInputType() {
		return inputType;
	}

	public int getPolyNumberOfLines() {
		return polyNumberOfLines;
	}
}
