/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.filter.binary.BinaryContourInterface;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.binary.ContourPacked;
import boofcv.misc.MovingAverage;
import boofcv.struct.ConfigLength;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.metric.Area2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Detects convex polygons with the specified number of sides in an image. Shapes are assumed to be black shapes
 * against a white background, allowing for thresholding to be used. Subpixel refinement is done using the
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
 * The returned polygons will encompass the entire black polygon. Here is a simple example in 1D. If all pixels are
 * white, but pixels ranging from 5 to 10, inclusive, then the returned boundaries would be 5.0 to 11.0. This
 * means that coordinates 5.0 &le; x &lt; 11.0 are all black. 11.0 is included, but note that the entire pixel 11 is white.
 * </p>
 *
 * <p>Notes:
 * <ul>
 * <li>If a lens distortion model is provided for lens distortion, the returned polygon will be in undistorted.</li>
 * </ul>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectPolygonFromContour<T extends ImageGray<T>> implements VerbosePrint {

	// minimum size of a shape's contour as a fraction of the image width
	private ConfigLength minimumContourConfig;
	private int minimumContour;
	private double minimumArea; // computed from minimumContour

	@Getter private BinaryContourFinder contourFinder;
	private BinaryContourInterface.Padded contourPadded;
	int imageWidth, imageHeight; // input image shape

	// finds the initial polygon around a target candidate
	private PointsToPolyline contourToPolyline;
	private final DogArray_I32 splits = new DogArray_I32();

	// Used to prune false positives
	private ContourEdgeIntensity<T> contourEdgeIntensity;

	/** extra information for found shapes */
	@Getter DogArray<Info> foundInfo = new DogArray<>(Info::new);

	// true if points touching the border are NOT pruned
	private boolean canTouchBorder;

	// work space for initial polygon
	private Polygon2D_F64 polygonWork = new Polygon2D_F64(); // undistorted pixel coordinate
	private final Polygon2D_F64 polygonDistorted = new Polygon2D_F64(); // distorted pixel coordinates;

	/**
	 * <p>Is the polygon sorted in clockwise direction?</p>
	 * WARNING: This is clockwise with +y up and not the standard
	 * +y down found in computer vision. This is because a general purpose geometry library is used and it
	 * caused more errors to swap between the two standards.
	 */
	@Getter @Setter private boolean outputClockwiseUpY;

	/** transforms which can be used to handle lens distortion */
	@Getter protected @Nullable PixelTransform<Point2D_F32> distToUndist, undistToDist;
	protected Point2D_F32 distortedPoint = new Point2D_F32();

	/** How intense the edge along a contour needs to be for it to be processed */
	@Getter @Setter double contourEdgeThreshold;

	/** helper used to customize low level behaviors internally */
	@Setter private PolygonHelper helper;

	// storage space for contour in undistorted pixels
	private final DogArray<Point2D_I32> undistorted = new DogArray<>(Point2D_I32::new);

	/** type of input gray scale image it can process */
	@Getter private Class<T> inputType;

	// indicates which corners touch the border
	private final DogArray_B borderCorners = new DogArray_B();

	// temporary storage for a contour
	private final DogArray<Point2D_I32> contourTmp = new DogArray<>(Point2D_I32::new);
	List<Point2D_I32> polygonPixel = new ArrayList<>();

	// times for internal profiling
	MovingAverage milliContour = new MovingAverage(0.8);
	MovingAverage milliShapes = new MovingAverage(0.8);

	// If not null then it will print verbose messages to this stream
	@Nullable PrintStream verbose = null;

	/**
	 * Configures the detector.
	 *
	 * @param contourToPolyline Fits a crude polygon to the shape's binary contour
	 * @param minimumContour Minimum allowed length of a contour. Copy stored internally. Try 50 pixels.
	 * @param outputClockwiseUpY If true then the order of the output polygons will be in clockwise order
	 * @param touchBorder if true then shapes which touch the image border are allowed
	 * @param contourEdgeThreshold Polygons with an edge intensity less than this are discarded.
	 * @param inputType Type of input image it's processing
	 */
	public DetectPolygonFromContour( PointsToPolyline contourToPolyline,
									 ConfigLength minimumContour,
									 boolean outputClockwiseUpY,
									 boolean touchBorder,
									 double contourEdgeThreshold,
									 double tangentEdgeIntensity,
									 BinaryContourFinder contourFinder,
									 Class<T> inputType ) {

		this.minimumContourConfig = minimumContour.copy(); // local copy so that external can be modified
		this.contourToPolyline = contourToPolyline;
		this.outputClockwiseUpY = outputClockwiseUpY;
		this.canTouchBorder = touchBorder;
		this.contourEdgeThreshold = contourEdgeThreshold;
		this.contourFinder = contourFinder;
		this.inputType = inputType;

		if (contourFinder instanceof BinaryContourInterface.Padded) {
			contourPadded = (BinaryContourInterface.Padded)contourFinder;
		}

		if (!this.contourToPolyline.isLoop())
			throw new IllegalArgumentException("ContourToPolygon must be configured for loops");

		if (contourEdgeThreshold > 0) {
			this.contourEdgeIntensity = new ContourEdgeIntensity<>(30, 1, tangentEdgeIntensity, inputType);
		}

		polygonWork = new Polygon2D_F64(1);
	}

	/**
	 * For unit testing
	 */
	protected DetectPolygonFromContour() {}

	/**
	 * <p>Specifies transforms which can be used to change coordinates from distorted to undistorted and the opposite
	 * coordinates. The undistorted image is never explicitly created.</p>
	 *
	 * @param width Input image width. Used in sanity check only.
	 * @param height Input image height. Used in sanity check only.
	 * @param distToUndist Transform from distorted to undistorted image.
	 * @param undistToDist Transform from undistorted to distorted image.
	 */
	public void setLensDistortion( int width, int height,
								   @Nullable PixelTransform<Point2D_F32> distToUndist,
								   @Nullable PixelTransform<Point2D_F32> undistToDist ) {

		this.distToUndist = distToUndist;
		this.undistToDist = undistToDist;
	}

	public void resetRuntimeProfiling() {
		milliContour.reset();
		milliShapes.reset();
	}

	/**
	 * Discard previously set lens distortion models
	 */
	public void clearLensDistortion() {
		this.distToUndist = null;
		this.undistToDist = null;
	}

	/**
	 * Examines the undistorted gray scale input image for squares. If p
	 *
	 * @param gray Input image
	 */
	public void process( T gray, GrayU8 binary ) {
		if (verbose != null) verbose.println("ENTER  DetectPolygonFromContour.process()");

		if (contourPadded != null && !contourPadded.isCreatePaddedCopy()) {
			int padding = 2;
			if (gray.width + padding != binary.width || gray.height + padding != binary.height) {
				throw new IllegalArgumentException("Including padding, expected a binary image with shape "
						+ (gray.width + padding) + "x" + (gray.height + padding));
			}
		} else {
			InputSanityCheck.checkSameShape(binary, gray);
		}
		if (imageWidth != gray.width || imageHeight != gray.height)
			configure(gray.width, gray.height);

		// reset storage for output. Call reset individually here to ensure that all references
		// are nulled from last time
		for (int i = 0; i < foundInfo.size; i++) {
			foundInfo.get(i).reset();
		}
		foundInfo.reset();

		if (contourEdgeIntensity != null)
			contourEdgeIntensity.setImage(gray);

		long time0 = System.nanoTime();

		// find all the contours
		contourFinder.process(binary);

		long time1 = System.nanoTime();

		// Using the contours find the polygons
		findCandidateShapes();

		long time2 = System.nanoTime();

		double a = (time1 - time0)*1e-6;
		double b = (time2 - time1)*1e-6;

		milliContour.update(a);
		milliShapes.update(b);

		if (verbose != null) verbose.println("EXIT  DetectPolygonFromContour.process()");
	}

	/**
	 * Specifies the image's intrinsic parameters and target size
	 *
	 * @param width Width of the input image
	 * @param height Height of the input image
	 */
	private void configure( int width, int height ) {

		this.imageWidth = width;
		this.imageHeight = height;

		// adjust size based parameters based on image size
		this.minimumContour = minimumContourConfig.computeI(Math.min(width, height));
		this.minimumContour = Math.max(4, minimumContour); // This is needed to avoid processing zero or other impossible
		this.minimumArea = Math.pow(this.minimumContour/4.0, 2);
		contourFinder.setMinContour(minimumContour);

		if (helper != null)
			helper.setImageShape(width, height);
	}

	/**
	 * Finds blobs in the binary image. Then looks for blobs that meet size and shape requirements. See code
	 * below for the requirements. Those that remain are considered to be target candidates.
	 */
	private void findCandidateShapes() {

		// find blobs where all 4 edges are lines
		List<ContourPacked> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size(); i++) {
			ContourPacked c = blobs.get(i);

			contourTmp.reset();
			contourFinder.loadContour(c.externalIndex, contourTmp);
			if (contourTmp.size() >= minimumContour) {
				float edgeInside = -1, edgeOutside = -1;

//				System.out.println("----- candidate "+contourTmp.size()+"  "+contourTmp.get(0));

				// ignore shapes which touch the image border
				boolean touchesBorder = touchesBorder(contourTmp.toList());
				if (!canTouchBorder && touchesBorder) {
					if (verbose != null) verbose.println("rejected polygon, touched border");
					continue;
				}

				if (helper != null)
					if (!helper.filterContour(contourTmp.toList(), touchesBorder, true))
						continue;

				// filter out contours which are noise
				if (contourEdgeIntensity != null) {
					contourEdgeIntensity.process(contourTmp.toList(), true);
					edgeInside = contourEdgeIntensity.getEdgeInsideAverage();
					edgeOutside = contourEdgeIntensity.getEdgeOutsideAverage();

					// take the ABS because CCW/CW isn't known yet
					if (Math.abs(edgeOutside - edgeInside) < contourEdgeThreshold) {
						if (verbose != null) verbose.println("rejected polygon. contour edge intensity");
						continue;
					}
				}

				// remove lens distortion
				List<Point2D_I32> undistorted;
				if (distToUndist != null) {
					undistorted = this.undistorted.toList();
					removeDistortionFromContour(contourTmp.toList(), this.undistorted);
					if (helper != null)
						if (!helper.filterContour(this.undistorted.toList(), touchesBorder, false))
							continue;
				} else {
					undistorted = contourTmp.toList();
				}

				if (helper != null) {
					helper.configureBeforePolyline(contourToPolyline, touchesBorder);
				}

				// Find the initial approximate fit of a polygon to the contour
				if (!contourToPolyline.process(undistorted, splits)) {
					if (verbose != null)
						verbose.println("rejected polygon initial fit failed. contour size = " + contourTmp.size());
					continue;
				}

				// determine the polygon's orientation
				polygonPixel.clear();
				for (int j = 0; j < splits.size; j++) {
					polygonPixel.add(undistorted.get(splits.get(j)));
				}

				// Note the CCW here uses a standard geometric coordinate system with +y up, not +y down
				boolean isCCW = UtilPolygons2D_I32.isCCW(polygonPixel);

				// Now that the orientation is known it can check to see if it's actually trying to fit to a
				// white blob instead of a black blob
				if (contourEdgeIntensity != null) {
					// before it assumed it was CCW
					if (!isCCW) {
						float tmp = edgeInside;
						edgeInside = edgeOutside;
						edgeOutside = tmp;
					}

					if (edgeInside > edgeOutside) {
						if (verbose != null) verbose.println("White blob. Rejected");
						continue;
					}
				}

				// see if it should be flipped so that the polygon has the correct orientation
				if (outputClockwiseUpY == isCCW) {
					flip(splits.data, splits.size);
				}

				// convert the format of the initial crude polygon
				polygonWork.vertexes.resize(splits.size());
				polygonDistorted.vertexes.resize(splits.size());
				for (int j = 0; j < splits.size(); j++) {
					Point2D_I32 p = undistorted.get(splits.get(j));
					Point2D_I32 q = contourTmp.get(splits.get(j));
					polygonWork.get(j).setTo(p.x, p.y);
					polygonDistorted.get(j).setTo(q.x, q.y);
				}

				if (touchesBorder) {
					determineCornersOnBorder(polygonDistorted, borderCorners);
				} else {
					borderCorners.resize(0);
				}

				if (helper != null) {
					if (!helper.filterPixelPolygon(polygonWork, polygonDistorted, borderCorners, touchesBorder)) {
						if (verbose != null) verbose.println("rejected by helper.filterPixelPolygon()");
						continue;
					}
				}

				// make sure it's big enough
				double area = Area2D_F64.polygonSimple(polygonWork);

				if (area < minimumArea) {
					if (verbose != null) verbose.println("Rejected area");
					continue;
				}

				// Get the storage for a new polygon. This is recycled and has already been cleaned up
				Info info = foundInfo.grow();

				if (distToUndist != null) {
					// changed the save points in the packed contour list with undistorted coordinates
					contourFinder.writeContour(c.externalIndex, undistorted);
				}

				// save results
				info.splits.setTo(splits);
				info.contourTouchesBorder = touchesBorder;
				info.external = true;
				info.edgeInside = edgeInside;
				info.edgeOutside = edgeOutside;
				info.contour = c;
				info.polygon.setTo(polygonWork);
				info.polygonDistorted.setTo(polygonDistorted);
				info.borderCorners.setTo(borderCorners);
			}
		}
	}

	// TODO move into ddogleg? primitive flip  <--- I think this is specific to polygons
	public static void flip( int[] a, int N ) {
		int H = N/2;

		for (int i = 1; i <= H; i++) {
			int j = N - i;
			int tmp = a[i];
			a[i] = a[j];
			a[j] = tmp;
		}
	}

	/**
	 * Check to see if corners are touching the image border
	 *
	 * @param polygon Polygon in distorted (original image) pixels
	 * @param onImageBorder storage for corner indexes
	 */
	void determineCornersOnBorder( Polygon2D_F64 polygon, DogArray_B onImageBorder ) {
		onImageBorder.reset();
		for (int i = 0; i < polygon.size(); i++) {
			Point2D_F64 p = polygon.get(i);

			onImageBorder.add(p.x <= 1 || p.y <= 1 || p.x >= imageWidth - 2 || p.y >= imageHeight - 2);
		}
	}

	/**
	 * Returns the undistorted contour for a shape. Data is potentially recycled the next time
	 * any function in this class is invoked.
	 *
	 * @param info Which shape
	 * @return List of points in the contour
	 */
	public List<Point2D_I32> getContour( Info info ) {
		contourTmp.reset();
		contourFinder.loadContour(info.contour.externalIndex, contourTmp);
		return contourTmp.toList();
	}

//	/**
//	 * Check to see if corners are touching the image border
//	 * @param polygon Refined polygon
//	 * @param corners storage for corner indexes
//	 */
//	void determineCornersOnBorder( Polygon2D_F64 polygon , DogArray_B corners , float tol ) {
//		corners.reset();
//		for (int i = 0; i < polygon.size(); i++) {
//			corners.add(isUndistortedOnBorder(polygon.get(i),tol));
//		}
//	}
//
//	/**
//	 * Coverts the point into distorted image coordinates and then checks to see if it is on the image border
//	 * @param undistorted pixel in undistorted coordinates
//	 * @param tol Tolerance for a point being on the image border
//	 * @return true if on the border or false otherwise
//	 */
//	boolean isUndistortedOnBorder( Point2D_F64 undistorted , float tol ) {
//		float x,y;
//
//		if( undistToDist == null ) {
//			x = (float)undistorted.x;
//			y = (float)undistorted.y;
//		} else {
//			undistToDist.compute((int)Math.round(undistorted.x),(int)Math.round(undistorted.y));
//			x = undistToDist.distX;
//			y = undistToDist.distY;
//		}
//
//		return( x <= tol || y <= tol || x+tol >= labeled.width-1 || y+tol >= labeled.height-1 );
//	}

	/**
	 * Removes lens distortion from the found contour
	 */
	private void removeDistortionFromContour( List<Point2D_I32> distorted, DogArray<Point2D_I32> undistorted ) {
		Objects.requireNonNull(distToUndist);
		undistorted.reset();

		for (int j = 0; j < distorted.size(); j++) {
			// remove distortion
			Point2D_I32 p = distorted.get(j);
			Point2D_I32 q = undistorted.grow();

			distToUndist.compute(p.x, p.y, distortedPoint);

			// round to minimize error
			q.x = Math.round(distortedPoint.x);
			q.y = Math.round(distortedPoint.y);
		}
	}

	/**
	 * Checks to see if some part of the contour touches the image border. Most likely cropped
	 */
	protected final boolean touchesBorder( List<Point2D_I32> contour ) {
		int endX = imageWidth - 1;
		int endY = imageHeight - 1;

		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			if (p.x == 0 || p.y == 0 || p.x == endX || p.y == endY) {
				return true;
			}
		}

		return false;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> options ) {
		this.verbose = out;
	}

	public boolean isConvex() {
		return contourToPolyline.isConvex();
	}

	public void setConvex( boolean convex ) {
		contourToPolyline.setConvex(convex);
	}

	public List<ContourPacked> getAllContours() {return contourFinder.getContours();}

	public void setNumberOfSides( int min, int max ) {
		if (min < 3)
			throw new IllegalArgumentException("The min must be >= 3");
		if (max < min)
			throw new IllegalArgumentException("The max must be >= the min");

		this.contourToPolyline.setMinimumSides(min);
		this.contourToPolyline.setMaximumSides(max);
	}

	public int getMinimumSides() {
		return contourToPolyline.getMinimumSides();
	}

	public int getMaximumSides() {
		return contourToPolyline.getMaximumSides();
	}

	public double getMilliContour() {
		return milliContour.getAverage();
	}

	public double getMilliShapes() {
		return milliShapes.getAverage();
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class Info {
		/**
		 * Was it created from an external or internal contour
		 */
		public boolean external;

		/**
		 * Average pixel intensity score along the polygon's edge inside and outside
		 */
		public double edgeInside, edgeOutside;

		/**
		 * True if the shape's contour touches the image border
		 */
		public boolean contourTouchesBorder;

		/**
		 * Boolean value for each corner being along the border. If empty then non of the corners are long the border.
		 * true means the corner is a border corner.
		 */
		public DogArray_B borderCorners = new DogArray_B();

		/**
		 * Polygon in undistorted image pixels.
		 */
		public Polygon2D_F64 polygon = new Polygon2D_F64();
		/**
		 * Polygon in the original (distorted) image pixels
		 */
		public Polygon2D_F64 polygonDistorted = new Polygon2D_F64();

		public DogArray_I32 splits = new DogArray_I32();

		/**
		 * Contour that the shape was fit to. The point coordinates will be in undistorted coordinates if
		 * an undistortion function was passed in.
		 */
		public ContourPacked contour;

		public double computeEdgeIntensity() {
			return edgeOutside - edgeInside; // black square. Outside should be a high value (white) inside low (black)
		}

		public boolean hasInternal() {
			return contour.internalIndexes.size > 0;
		}

		@SuppressWarnings("NullAway")
		public void reset() {
			external = false;
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
