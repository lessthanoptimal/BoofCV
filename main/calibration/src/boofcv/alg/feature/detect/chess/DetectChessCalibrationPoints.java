/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.InvalidCalibrationTarget;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.detect.quadblob.OrderPointsIntoGrid;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Detects calibration points inside a chessboard calibration target.  A crude approximation of the chess
 * board is found by thresholding the image dynamically.  Once found corner points are detected and pruned.
 * The remaining points are computed to sub-pixel accuracy by fitting a 2D quadratic to feature intensity
 * in a 3x3 region.
 * </p>
 * <p/>
 * <p>
 * The found control points are ensured to be returned in a row-major format with the correct number of rows and columns,
 * with a counter clockwise ordering.  Note that when viewed on the monitor this will appear to be clockwise because
 * the y-axis points down.
 * </p>
 *
 * @author Peter Abeles
 */
public class DetectChessCalibrationPoints<T extends ImageSingleBand, D extends ImageSingleBand> {
	// stores image derivative
	private D derivX;
	private D derivY;

	// number of control points 
	private int numColsPoints;
	private int numRowsPoints;

	// radius of the feature being detected
	private int radius;

	// detects the chess board 
	private DetectChessSquaresBinary findBound;
	// binary images used to detect chess board
	private ImageUInt8 binary = new ImageUInt8(1, 1);
	private ImageUInt8 eroded = new ImageUInt8(1, 1);
	// Threshold used to create binary image.  if < 0 then a threshold is automatically selected
	private double userBinaryThreshold = -1;
	// the actual threshold used
	private double actualBinaryThreshold;
	// relative blob size threshold.  Adjusted relative to image size.  Small objects are pruned
	private double relativeSizeThreshold;

	// point detection algorithms
	private GeneralFeatureIntensity<T, D> intensityAlg;
	private GeneralFeatureDetector<T, D> detectorAlg;

	// point location at subpixel accuracy
	private List<Point2D_F64> subpixel;

	// number of points it expects to observer in the target
	private int expectedPoints;

	// rectangle the target is contained inside of
	private ImageRectangle targetRect;

	// puts points into the correct order
	private OrderPointsIntoGrid orderAlg = new OrderPointsIntoGrid();

	// true if it found the rectangular bound
	private boolean foundBound;

	// storage for image histogram
	private int histogram[] = new int[256];

	/**
	 * Configures detection parameters
	 *
	 * @param numCols       Number of columns in the grid.  Target dependent.
	 * @param numRows       Number of rows in the grid.  Target dependent.
	 * @param radius        Side of interest point detection region.  Typically 5
	 * @param relativeSizeThreshold Increases or decreases the minimum allowed blob size. Try 1.0
	 * @param imageType     Type of image being processed
	 */
	public DetectChessCalibrationPoints(int numCols, int numRows, int radius,
										double relativeSizeThreshold ,
										Class<T> imageType) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		this.radius = radius;
		this.relativeSizeThreshold = relativeSizeThreshold;

		this.numColsPoints = numCols-1;
		this.numRowsPoints = numRows-1;

		expectedPoints = numColsPoints * numRowsPoints;

		derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);

		intensityAlg = FactoryIntensityPoint.shiTomasi(radius, true, derivType);
//		intensityAlg = FactoryIntensityPoint.harris(radius,0.04f,true,derivType);

		NonMaxSuppression extractor =
				FactoryFeatureExtractor.nonmax(new ConfigExtract(radius + 2, 20, radius + 2, true));
		detectorAlg = new GeneralFeatureDetector<T, D>(intensityAlg, extractor);

		// minContourSize is specified later after the image's size is known
		findBound = new DetectChessSquaresBinary(numCols, numRows, 0);

		reset();
	}

	/**
	 * Forgets any past history and resets the detector
	 */
	public void reset() {
	}

	public boolean process(T gray) {
		// initialize data structures
		targetRect = null;
		subpixel = new ArrayList<Point2D_F64>();

		binary.reshape(gray.width, gray.height);
		eroded.reshape(gray.width, gray.height);

		adjustForImageSize(gray.width,gray.height);

		// detect the chess board
		if (!(foundBound = detectChessBoard(gray,userBinaryThreshold,true)))
			return false;

		// rectangle that contains the area of interest
		targetRect = findBound.getBoundRect();

		T subGray = (T) gray.subimage(targetRect.x0, targetRect.y0, targetRect.x1, targetRect.y1);
		derivX.reshape(subGray.width, subGray.height);
		derivY.reshape(subGray.width, subGray.height);

		// extended border to avoid false positives
		GImageDerivativeOps.sobel(subGray, derivX, derivY, BorderType.EXTENDED);

		// detect interest points
		detectorAlg.process(subGray, derivX, derivY, null, null, null);

		QueueCorner corners = detectorAlg.getMaximums();

		// put points into original image coordinates
		List<Point2D_F64> points = convert(corners, targetRect.x0, targetRect.y0);

		// prune features which are not near any candidate corners
		pruneFarFromBlobCorners(points, findBound.getCandidatePoints());

		// make sure enough points were detected
		if (points.size() < expectedPoints)
			return false;

		// select N brightest features
		points = selectBrightest(points, intensityAlg.getIntensity(), targetRect.x0, targetRect.y0);

		// compute pixels to sub-pixel accuracy
		for (Point2D_F64 p : points)
			subpixel.add(refineSubpixel(p, targetRect.x0, targetRect.y0, intensityAlg.getIntensity()));

		try {
			orderAlg.process(subpixel);
		} catch( InvalidCalibrationTarget e ) {
			System.err.println(e.getMessage());
			return false;
		}

		if (numColsPoints * numRowsPoints != orderAlg.getNumCols() * orderAlg.getNumRows()) {
			System.err.println("Unexpected grid size");
			return false;
		}

		subpixel = UtilCalibrationGrid.rotatePoints(orderAlg.getOrdered(),
				orderAlg.getNumRows(), orderAlg.getNumCols(),
				numRowsPoints, numColsPoints);

		return subpixel != null;
	}

	/**
	 * Adjust image processing parameters for the input image size
	 */
	private void adjustForImageSize( int imgWidth , int imgHeight ) {
		int size = (int)(relativeSizeThreshold*40.0/640.0*imgWidth);

		if( size < 10 )
			size = 10;

		findBound.setMinimumContourSize(size);
	}

	/**
	 * Threshold the image and find squares
	 */
	private boolean detectChessBoard(T gray, double threshold , boolean first ) {

		actualBinaryThreshold = threshold;
		if( actualBinaryThreshold < 0 )
			actualBinaryThreshold = UtilCalibrationGrid.selectThreshold(gray,histogram);

		GThresholdImageOps.threshold(gray, binary, actualBinaryThreshold, true);

		// erode to make the squares separated
		BinaryImageOps.erode8(binary, eroded);

		if (findBound.process(eroded)) {
			if( userBinaryThreshold < 0 ) {
				// second pass to improve threshold
				return detectChessBoardSubImage(gray);
			} else {
				return true;
			}
		} else if( first && userBinaryThreshold < 0 ) {
			// if the target is small and the background dark, the threshold will be too low
			// if the target is small and the background white, it will still estimate a good threshold
			// so try a larger value before giving up
			threshold = (255+threshold)/2;
			return detectChessBoard(gray,threshold,false);
		}

		return false;
	}

	/**
	 * Look for the target only inside a specific region.  The threshold can be more accurate selected this way
	 */
	private boolean detectChessBoardSubImage( T gray ) {

		// region the target is contained inside of
		ImageRectangle targetBound = findBound.getBoundRect();

		// expand the bound a bit
		int w = targetBound.getWidth();
		int h = targetBound.getHeight();

		ImageRectangle expanded = new ImageRectangle();
		int adj = (int)(w*0.12);
		expanded.x0 = targetBound.x0-adj;
		expanded.x1 = targetBound.x1+adj;
		adj = (int)(h*0.12);
		expanded.y0 = targetBound.y0-adj;
		expanded.y1 = targetBound.y1+adj;

		BoofMiscOps.boundRectangleInside(gray, expanded);

		// create sub-images for processing.  recompute threshold just around the area of interest and
		// only look for the target inside that
		T subGray = (T)gray.subimage(expanded.x0,expanded.y0,expanded.x1,expanded.y1);
		actualBinaryThreshold = UtilCalibrationGrid.selectThreshold(subGray,histogram);

		GImageMiscOps.fill(binary,0);
		ImageUInt8 subBinary = (ImageUInt8)binary.subimage(expanded.x0,expanded.y0,expanded.x1,expanded.y1);
		GThresholdImageOps.threshold(subGray, subBinary, actualBinaryThreshold, true);

		// The new threshold tends to require two erodes
		BinaryImageOps.erode8(binary, eroded);
		BinaryImageOps.erode4(eroded, binary);
		BinaryImageOps.dilate4(binary, eroded);

		if (findBound.process(eroded))
			return true;

		return false;
	}

	/**
	 * Computes a feature location to sub-pixel accuracy by fitting a 2D quadratic polynomial
	 * to corner intensities.
	 * <p/>
	 * Through experimentation the mean instead of a quadratic fit was found to produce a better
	 * result.  Most papers seem to recommend using the quadratic.
	 *
	 * @param pt        Point in image coordinates
	 * @param x0        intensity image x offset
	 * @param y0        intensity image y offset
	 * @param intensity Intensity image
	 * @return Sub-pixel point location
	 */
	private Point2D_F64 refineSubpixel(Point2D_F64 pt,
									   int x0, int y0,
									   ImageFloat32 intensity) {
		int r = radius + 3;
		ImageRectangle area = new ImageRectangle((int) (pt.x - r - x0), (int) (pt.y - r - y0),
				(int) (pt.x + r - x0 + 1), (int) (pt.y + r + 1 - y0));
		BoofMiscOps.boundRectangleInside(intensity, area);

		// sample feature intensity values in the local region
		float meanX = 0, meanY = 0, sum = 0;
		for (int i = area.y0; i < area.y1; i++) {
			for (int j = area.x0; j < area.x1; j++) {
				float value = intensity.get(j, i);

				meanX += j * value;
				meanY += i * value;
				sum += value;
			}
		}
		meanX /= sum;
		meanY /= sum;

		return new Point2D_F64(x0 + meanX, y0 + meanY);
	}

	/**
	 * Ensures that the detected points are in correct grid order.  This is done by using the
	 * predicted point locations, which are already in order
	 *
	 * @param predicted Predicted and order points
	 * @param detected  Detect corner location
	 * @return Ordered detected points
	 */
	private List<Point2D_I32> orderPoints(List<Point2D_I32> predicted, List<Point2D_I32> detected) {
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();

		for (Point2D_I32 p : predicted) {
			double bestDist = Double.MAX_VALUE;
			Point2D_I32 best = null;

			for (Point2D_I32 d : detected) {
				double dist = UtilPoint2D_I32.distance(p, d);

				if (dist < bestDist) {
					bestDist = dist;
					best = d;
				}
			}

			ret.add(best);
		}

		return ret;
	}

	/**
	 * Prunes detected corners that are not near any of the corners
	 */
	private void pruneFarFromBlobCorners(List<Point2D_F64> corners , List<Point2D_I32> initial ) {

		int tolerance = 10;
		Iterator<Point2D_F64> iter = corners.iterator();

		while( iter.hasNext() ) {
			Point2D_F64 c = iter.next();
			int x = (int)c.x;
			int y = (int)c.y;

			boolean matched = false;

			for( Point2D_I32 i : initial )
				if( UtilPoint2D_I32.distance(x,y,i.x,i.y) < tolerance )  {
					matched = true;
					break;
				}
			if( !matched )
				iter.remove();
		}
	}

	/**
	 * Converts from a corner in the sub-image into a point in the full image
	 */
	private List<Point2D_F64> convert(QueueCorner found, int x0, int y0) {
		List<Point2D_F64> points = new ArrayList<Point2D_F64>();
		for (int i = 0; i < found.size(); i++) {
			Point2D_I16 c = found.get(i);
			points.add(new Point2D_F64(c.x + x0, c.y + y0));
		}
		return points;
	}

	/**
	 * Out of the remaining points, just select the brightest to remove any remaining false positives
	 */
	private List<Point2D_F64> selectBrightest(List<Point2D_F64> points, ImageFloat32 intensity,
											  int offX, int offY) {
		if (points.size() == expectedPoints)
			return points;


		double values[] = new double[points.size()];
		int indexes[] = new int[points.size()];

		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);

			values[i] = -intensity.get((int) (p.x - offX), (int) (p.y - offY));
		}

		new QuickSort_F64().sort(values, points.size(), indexes);

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for (int i = 0; i < expectedPoints; i++) {
			ret.add(points.get(indexes[i]));
		}

		return ret;
	}

	/**
	 * Only part of the image is processed when detecting features.  This copies the detected part into
	 * the provided image
	 *
	 * @param wholeImage Image being written to
	 */
	public void renderIntensity(ImageFloat32 wholeImage) {
		if( targetRect == null ) {
			ImageMiscOps.fill(wholeImage,0);
		} else {
			ImageFloat32 found = intensityAlg.getIntensity();
			ImageFloat32 out = wholeImage.subimage(targetRect.x0, targetRect.y0, targetRect.x1, targetRect.y1);
			out.setTo(found);
		}
	}

	public DetectChessSquaresBinary getFindBound() {
		return findBound;
	}

	public List<Point2D_F64> getPoints() {
		return subpixel;
	}

	public ImageUInt8 getBinary() {
			return eroded;
	}

	public double getActualBinaryThreshold() {
		return actualBinaryThreshold;
	}

	public void setUserBinaryThreshold(double userBinaryThreshold) {
		this.userBinaryThreshold = userBinaryThreshold;
	}

	public double getUserBinaryThreshold() {
		return userBinaryThreshold;
	}

	public boolean isFoundBound() {
		return foundBound;
	}

	public List<Point2D_F64> detectedPointFeatures() {

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		int offX = targetRect.x0, offY = targetRect.y0;
		QueueCorner found = detectorAlg.getMaximums();

		for( int i = 0; i < found.size; i++ ) {
			Point2D_I16 c = found.get(i);
			ret.add(new Point2D_F64(offX + c.x , offY + c.y) );
		}

		return ret;
	}
}
