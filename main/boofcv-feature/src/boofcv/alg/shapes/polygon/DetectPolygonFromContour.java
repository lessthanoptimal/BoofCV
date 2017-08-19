/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.shapes.polyline.MinimizeEnergyPrune;
import boofcv.alg.shapes.polyline.RefinePolyLineCorner;
import boofcv.alg.shapes.polyline.SplitMergeLineFitLoop;
import boofcv.struct.ConnectRule;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.metric.Area2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO Update documentation
 * <p>
 * Detects convex polygons with the specified number of sides in an image.  Shapes are assumed to be black shapes
 * against a white background, allowing for thresholding to be used.  Subpixel refinement is done using the
 * provided implementation of {@link RefinePolygonToGray}.
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
 * means that coordinates 5.0 &le; x &lt; 11.0 are all black.  11.0 is included, but note that the entire pixel 11 is white.
 * </p>
 *
 * <p>Notes:
 * <ul>
 * <li>If a lens distortion model is provided for lens distortion, the returned polygon will be in undistorted.</li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class DetectPolygonFromContour<T extends ImageGray<T>> {

	// minimum size of a shape's contour as a fraction of the image width
	private double minContourFraction;
	private int minimumContour; // this is image.width*minContourFraction
	private double minimumArea; // computed from minimumContour

	// does the polygon have to be convex
	private boolean convex;

	private LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	private GrayS32 labeled = new GrayS32(1,1);

	// finds the initial polygon around a target candidate
	private SplitMergeLineFitLoop fitPolygon;

	// removes extra corners
	private GrowQueue_I32 pruned = new GrowQueue_I32(); // corners after pruning
	private MinimizeEnergyPrune pruner;

	// Used to prune false positives
	private ContourEdgeIntensity<T> contourEdgeIntensity;

	// Improve the selection of corner pixels in the contour
	private RefinePolyLineCorner improveContour = new RefinePolyLineCorner(true,20);

	// extera information for found shapes
	private FastQueue<Info> foundInfo = new FastQueue<>(Info.class, true);

	// number of lines allowed in the polygon
	private int minSides,maxSides;

	// true if points touching the border are NOT pruned
	private boolean canTouchBorder;

	// work space for initial polygon
	private Polygon2D_F64 polygonWork = new Polygon2D_F64(); // undistorted pixel coordinate
	private Polygon2D_F64 polygonDistorted = new Polygon2D_F64(); // distorted pixel coordinates;

	// should the order of the polygon be on clockwise order on output?
	private boolean outputClockwise;

	// transforms which can be used to handle lens distortion
	protected PixelTransform2_F32 distToUndist, undistToDist;

	private boolean verbose = false;

	// How intense the edge along a contour needs to be for it to be processed
	double contourEdgeThreshold;

	// helper used to customize low level behaviors internally
	private PolygonHelper helper;

	// storage space for contour in undistorted pixels
	private FastQueue<Point2D_I32> undistorted = new FastQueue<>(Point2D_I32.class,true);

	// type of input gray scale image it can process
	private Class<T> inputType;

	/**
	 * Configures the detector.
	 *
	 * @param minSides minimum number of sides
	 * @param maxSides maximum number of sides
	 * @param contourToPolygon Fits a crude polygon to the shape's binary contour
	 * @param minContourFraction Size of minimum contour as a fraction of the input image's width.  Try 0.23
	 * @param outputClockwise If true then the order of the output polygons will be in clockwise order
	 * @param convex If true it will only return convex shapes
	 * @param touchBorder if true then shapes which touch the image border are allowed
	 * @param splitPenalty Penalty given to a line segment while splitting.  See {@link MinimizeEnergyPrune}
	 * @param contourEdgeThreshold Polygons with an edge intensity less than this are discarded.
	 * @param inputType Type of input image it's processing
	 */
	public DetectPolygonFromContour(int minSides, int maxSides,
									SplitMergeLineFitLoop contourToPolygon,
									double minContourFraction,
									boolean outputClockwise,
									boolean convex,
									boolean touchBorder, double splitPenalty,
									double contourEdgeThreshold,
									Class<T> inputType) {

		setNumberOfSides(minSides,maxSides);
		this.minContourFraction = minContourFraction;
		this.fitPolygon = contourToPolygon;
		this.outputClockwise = outputClockwise;
		this.convex = convex;
		this.canTouchBorder = touchBorder;
		this.contourEdgeThreshold = contourEdgeThreshold;
		this.inputType = inputType;

		if( contourEdgeThreshold > 0 ) {
			this.contourEdgeIntensity = new ContourEdgeIntensity<>(30, 1, 2.5, inputType);
		}

		pruner = new MinimizeEnergyPrune(splitPenalty);

		polygonWork = new Polygon2D_F64(1);
	}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates.  The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width.  Used in sanity check only.
	 * @param height Input image height.  Used in sanity check only.
	 * @param distToUndist Transform from distorted to undistorted image.
	 * @param undistToDist Transform from undistorted to distorted image.
	 */
	public void setLensDistortion(int width , int height ,
								  PixelTransform2_F32 distToUndist , PixelTransform2_F32 undistToDist ) {

		this.distToUndist = distToUndist;
		this.undistToDist = undistToDist;
	}

	/**
	 * Discard previously set lens distortion models
	 */
	public void clearLensDistortion() {
		this.distToUndist = null;
		this.undistToDist = null;
	}

	/**
	 * Examines the undistorted gray scake input image for squares.
	 *
	 * @param gray Input image
	 */
	public void process(T gray, GrayU8 binary) {
		if( verbose ) System.out.println("ENTER  DetectPolygonFromContour.process()");
		InputSanityCheck.checkSameShape(binary, gray);

		if( labeled.width != gray.width || labeled.height != gray.height )
			configure(gray.width,gray.height);

		// reset storage for output. Call reset individually here to ensure that all references
		// are nulled from last time
		for (int i = 0; i < foundInfo.size; i++) {
			foundInfo.get(i).reset();
		}
		foundInfo.reset();

		if( contourEdgeIntensity != null )
			contourEdgeIntensity.setImage(gray);

		// find all the contours
		contourFinder.process(binary, labeled);

		// Using the contours find the polygons
		findCandidateShapes();
		if( verbose ) System.out.println("EXIT  DetectPolygonFromContour.process()");
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

		if( helper != null )
			helper.setImageShape(width,height);
	}

	/**
	 * Finds blobs in the binary image.  Then looks for blobs that meet size and shape requirements.  See code
	 * below for the requirements.  Those that remain are considered to be target candidates.
	 */
	private void findCandidateShapes() {

		// stop fitting the polygon if it clearly has way too many sides
		int maxSidesConsider = (int)Math.ceil(maxSides*1.5);
		fitPolygon.setAbortSplits(2*maxSides);

		// find blobs where all 4 edges are lines
		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			if( c.external.size() >= minimumContour) {
				float edgeInside=-1,edgeOutside=-1;

//				System.out.println("----- candidate "+c.external.size());

				// ignore shapes which touch the image border
				boolean touchesBorder = touchesBorder(c.external);
				if( !canTouchBorder && touchesBorder ) {
					if( verbose ) System.out.println("rejected polygon, touched border");
					continue;
				}

				if( helper != null )
					if( !helper.filterContour(c.external,touchesBorder,true) )
						continue;

				// filter out contours which are noise
				if( contourEdgeIntensity != null ) {
					contourEdgeIntensity.process(c.external,true);
					edgeInside = contourEdgeIntensity.getInsideAverage();
					edgeOutside = contourEdgeIntensity.getOutsideAverage();

					// take the ABS because CCW/CW isn't known yet
					if( Math.abs(edgeOutside-edgeInside) < contourEdgeThreshold ) {
						if( verbose ) System.out.println("rejected polygon. contour edge intensity");
						continue;
					}
				}

				// remove lens distortion
				List<Point2D_I32> undistorted;
				if( distToUndist != null ) {
					undistorted = this.undistorted.toList();
					removeDistortionFromContour(c.external,this.undistorted);
					if( helper != null )
						if( !helper.filterContour(this.undistorted.toList(),touchesBorder,false) )
							continue;
				} else {
					undistorted = c.external;
				}

				// Find the initial approximate fit of a polygon to the contour
				if( !fitPolygon.process(undistorted) ) {
					if( verbose ) System.out.println("rejected polygon initial fit failed. contour size = "+c.external.size());
					continue;
				}
				GrowQueue_I32 splits = fitPolygon.getSplits();

				// determine the polygon's orientation
				List<Point2D_I32> polygonPixel = new ArrayList<>();
				for (int j = 0; j < splits.size; j++) {
					polygonPixel.add(undistorted.get(splits.get(j)));
				}

				boolean isCCW = UtilPolygons2D_I32.isCCW(polygonPixel);

				// Now that the orientation is known it can check to see if it's actually trying to fit to a
				// white blob instead of a black blob
				if( contourEdgeIntensity != null ) {
					// before it assumed it was CCW
					if( !isCCW ) {
						float tmp = edgeInside;
						edgeInside = edgeOutside;
						edgeOutside = tmp;
					}

					if( edgeInside > edgeOutside ) {
						if( verbose ) System.out.println("White blob. Rejected");
						continue;
					}
				}


				if( splits.size() > maxSidesConsider ) {
					if( verbose ) System.out.println("Way too many corners, "+splits.size()+". Aborting before improve. Contour size "+c.external.size());
					continue;
				}

				// Perform a local search and improve the corner placements
				if( !improveContour.fit(undistorted,splits) ) {
					if( verbose ) System.out.println("rejected improve contour. contour size = "+c.external.size());
					continue;
				}

				// reduce the number of corners based on an energy model
				pruner.prune(undistorted, splits, pruned);
				splits = pruned;

				// only accept polygons with the expected number of sides
				if (!expectedNumberOfSides(splits)) {
//					System.out.println("First point "+c.external.get(0));
					if( verbose ) System.out.println("rejected number of sides. "+splits.size()+"  contour "+c.external.size());
					continue;
				}

				// see if it should be flipped so that the polygon has the correct orientation
				if( outputClockwise == isCCW ) {
					flip(splits.data,splits.size);
				}

				// convert the format of the initial crude polygon
				polygonWork.vertexes.resize(splits.size());
				polygonDistorted.vertexes.resize(splits.size());
				for (int j = 0; j < splits.size(); j++) {
					Point2D_I32 p = undistorted.get( splits.get(j) );
					Point2D_I32 q = c.external.get( splits.get(j));
					polygonWork.get(j).set(p.x,p.y);
					polygonDistorted.get(j).set(q.x,q.y);
				}

				if( helper != null ) {
					if( !helper.filterPixelPolygon(polygonWork,polygonDistorted,touchesBorder) ) {
						if( verbose ) System.out.println("rejected by helper.filterPixelPolygon()");
						continue;
					}
				}

				// Filter out polygons which are not convex if requested by the user
				if( convex && !UtilPolygons2D_F64.isConvex(polygonWork)) {
					if( verbose ) System.out.println("Rejected not convex");
					continue;
				}

				// make sure it's big enough
				double area = Area2D_F64.polygonSimple(polygonWork);

				if( area < minimumArea ) {
					if( verbose ) System.out.println("Rejected area");
					continue;
				}

				// Get the storage for a new polygon. This is recycled and has already been cleaned up
				Info info = foundInfo.grow();

				// save the undistorted coordinate into external
				if( c.external != undistorted ) {
					for (int j = 0; j < c.external.size(); j++) {
						c.external.get(j).set(undistorted.get(j));
					}
				}

				// save results
				info.splits.setTo(splits);
				info.contourTouchesBorder = touchesBorder;
				info.external = true;
				info.edgeInside = edgeInside;
				info.edgeOutside = edgeOutside;
				info.label = c.id;
				info.contour = c.external;
				info.polygon.set(polygonWork);
				info.polygonDistorted.set(polygonDistorted);

				if( touchesBorder ) {
					// tolerance is a little bit above 0.5.pixels due to prior rounding to integer
					determineCornersOnBorder(info.polygonDistorted, info.borderCorners, 0.7f);
				}
			}
		}
	}

	// TODO move into ddogleg? primitive flip  <--- I think this is specific to polygons
	public static void flip( int []a , int N ) {
		int H = N/2;

		for (int i = 1; i <= H; i++) {
			int j = N-i;
			int tmp = a[i];
			a[i] = a[j];
			a[j] = tmp;
		}
	}

	/**
	 * Check to see if corners are touching the image border
	 * @param polygon Refined polygon
	 * @param corners storage for corner indexes
	 */
	void determineCornersOnBorder( Polygon2D_F64 polygon , GrowQueue_B corners , float tol ) {
		corners.reset();
		for (int i = 0; i < polygon.size(); i++) {
			corners.add(isUndistortedOnBorder(polygon.get(i),tol));
		}
	}

	/**
	 * Coverts the point into distorted image coordinates and then checks to see if it is on the image border
	 * @param undistorted pixel in undistorted coordinates
	 * @param tol Tolerance for a point being on the image border
	 * @return true if on the border or false otherwise
	 */
	boolean isUndistortedOnBorder( Point2D_F64 undistorted , float tol ) {
		float x,y;

		if( undistToDist == null ) {
			x = (float)undistorted.x;
			y = (float)undistorted.y;
		} else {
			undistToDist.compute((int)Math.round(undistorted.x),(int)Math.round(undistorted.y));
			x = undistToDist.distX;
			y = undistToDist.distY;
		}

		return( x <= tol || y <= tol || x+tol >= labeled.width-1 || y+tol >= labeled.height-1 );
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
	private void removeDistortionFromContour(List<Point2D_I32> distorted , FastQueue<Point2D_I32> undistorted  ) {
		undistorted.reset();

		for (int j = 0; j < distorted.size(); j++) {
			// remove distortion
			Point2D_I32 p = distorted.get(j);
			Point2D_I32 q = undistorted.grow();

			distToUndist.compute(p.x,p.y);

			// round to minimize error
			q.x = Math.round(distToUndist.distX);
			q.y = Math.round(distToUndist.distY);
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

	public GrayS32 getLabeled() {
		return labeled;
	}

	public boolean isOutputClockwise() {
		return outputClockwise;
	}

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

	public double getContourEdgeThreshold() {
		return contourEdgeThreshold;
	}

	public void setContourEdgeThreshold(double contourEdgeThreshold) {
		this.contourEdgeThreshold = contourEdgeThreshold;
	}

	public PixelTransform2_F32 getDistToUndist() {
		return distToUndist;
	}

	public PixelTransform2_F32 getUndistToDist() {
		return undistToDist;
	}

	/**
	 * Returns additional information on the polygon
	 */
	public FastQueue<Info> getFound() {
		return foundInfo;
	}

	public static class Info
	{
		/**
		 * Was it created from an external or internal contour
		 */
		public boolean external;

		/**
		 * The blob's label in the label image
		 */
		public int label;

		/**
		 * Average pixel intensity score along the polygon's edge inside and outside
		 */
		public double edgeInside,edgeOutside;

		/**
		 * True if the shape's contour touches the image border
		 */
		public boolean contourTouchesBorder;

		/**
		 * Boolean value for each corner being along the border.  If empty then non of the corners are long the border.
		 * true means the corner is a border corner.
		 */
		public GrowQueue_B borderCorners = new GrowQueue_B();

		/**
		 * Polygon in undistorted image pixels.
		 */
		public Polygon2D_F64 polygon = new Polygon2D_F64();
		/**
		 * Polygon in the original (distorted) image pixels
		 */
		public Polygon2D_F64 polygonDistorted = new Polygon2D_F64();


		public GrowQueue_I32 splits = new GrowQueue_I32();

		/**
		 * Contour that the shape was fit to. Might be in image pixels or undistorted image pixels
		 */
		public List<Point2D_I32> contour;

		public double computeEdgeIntensity() {
			return edgeOutside-edgeInside;
		}

		public void reset() {
			external = false;
			label = -1;
			edgeInside = edgeOutside = -1;
			contourTouchesBorder = true;
			borderCorners.reset();
			splits.reset();
			polygon.vertexes.reset();
			polygonDistorted.vertexes.reset();
			contour = null;
		}
	}
}
