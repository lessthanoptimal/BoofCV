/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.sorting.QuickSort_S32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
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

	// radius of the feature being detected
	private int radius;

	// detects the chess board 
	private DetectChessSquaresBinary findBound;
	// binary images used to detect chess board
	private ImageUInt8 binary = new ImageUInt8(1, 1);
	private ImageUInt8 eroded = new ImageUInt8(1, 1);
	// Threshold used to create binary image.  if < 0 then a threshold a local adaptive threshold is used
	private double userBinaryThreshold = -1;
	// parameters for local adaptive threshold
	private int userAdaptiveRadius = 20;
	private double userAdaptiveBias = -10;

	// point detection algorithms
	private GeneralFeatureIntensity<T, D> intensityAlg;

	// point location at subpixel accuracy
	private List<Point2D_F64> subpixel;

	// rectangle the target is contained inside of
	private ImageRectangle targetRect;

	// puts the quad blobs into order and thus the detected points
	private OrderChessboardQuadBlobs orderAlg;

	// true if it found the rectangular bound
	private boolean foundBound;

	// local search algorithm for peaks in corner intensity image
	private SearchLocalPeak<ImageFloat32> localPeak =
			FactorySearchLocalPeak.meanShiftUniform(10, 1e-4f,ImageFloat32.class);

	// work space for thresholding
	private T work1;
	private T work2;

	// storage for selecting control points from QuadBlob graphs
	private int indexes[] = new int[4];
	private int values[] = new int[4];
	private QuickSort_S32 sorter = new QuickSort_S32();

	FastQueue<Point2D_F32> corners = new FastQueue<Point2D_F32>(Point2D_F32.class,true);

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
										double relativeSizeThreshold , // TODo remove or re-active this threshold?
										Class<T> imageType) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		this.radius = radius;

		orderAlg = new OrderChessboardQuadBlobs(numCols,numRows);

		work1 = GeneralizedImageOps.createSingleBand(imageType,1,1);
		work2 = GeneralizedImageOps.createSingleBand(imageType,1,1);

		derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);

		intensityAlg = FactoryIntensityPoint.shiTomasi(radius, true, derivType);
//		intensityAlg = FactoryIntensityPoint.harris(radius,0.04f,true,derivType);

		// minContourSize is specified later after the image's size is known
		findBound = new DetectChessSquaresBinary(numCols, numRows, 10);

		localPeak.setSearchRadius(2);

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
		if (!(foundBound = detectChessBoard(gray)))
			return false;

		// rectangle that contains the area of interest
		targetRect = findBound.getBoundRect();

		T subGray = (T) gray.subimage(targetRect.x0, targetRect.y0, targetRect.x1, targetRect.y1, null);
		derivX.reshape(subGray.width, subGray.height);
		derivY.reshape(subGray.width, subGray.height);

		// extended border to avoid false positives
		GImageDerivativeOps.sobel(subGray, derivX, derivY, BorderType.EXTENDED);

		// detect interest points
		intensityAlg.process(subGray, derivX, derivY, null, null, null);

		List<QuadBlob> unorderedBlobs = findBound.getGraphBlobs();

		if( !orderAlg.order(unorderedBlobs) ) {
			return false;
		}

		seedPointsFromQuadCorner(orderAlg.getResults());
		meanShiftBlobCorners(intensityAlg.getIntensity(),targetRect);

		List<Point2D_F64> points = new ArrayList<Point2D_F64>();
		for( int i = 0; i < corners.size(); i++ ) {
			Point2D_F32 c = corners.get(i);
			points.add( new Point2D_F64(c.x,c.y));
		}

		// distance apart two points are from each other.
		int dist = (int)(points.get(0).distance(points.get(1))+1);

		// compute pixels to sub-pixel accuracy
		for (Point2D_F64 p : points)
			subpixel.add(refineSubpixel(p, dist, targetRect.x0, targetRect.y0, intensityAlg.getIntensity()));

//		subpixel.addAll(points);

		return subpixel != null;
	}

	/**
	 * Adjust image processing parameters for the input image size
	 */
	private void adjustForImageSize( int imgWidth , int imgHeight ) {
//		int size = (int)(relativeSizeThreshold*10.0/640.0*imgWidth);
//
//		if( size < 10 )
//			size = 10;
//
//		findBound.setMinimumContourSize(size);
	}

	/**
	 * Threshold the image and find squares
	 */
	private boolean detectChessBoard(T gray ) {

		if( userBinaryThreshold <= 0 ) {
			work1.reshape(gray.width,gray.height);
			work2.reshape(gray.width,gray.height);
			GThresholdImageOps.adaptiveSquare(gray, binary, userAdaptiveRadius, userAdaptiveBias, true, work1, work2);
		} else {
			GThresholdImageOps.threshold(gray, binary, userBinaryThreshold, true);
		}

		// erode to make the squares separated
		BinaryImageOps.erode8(binary, 1, eroded);

		return findBound.process(eroded);
	}

	/**
	 * Computes a feature location to sub-pixel accuracy by fitting a 2D quadratic polynomial
	 * to corner intensities.
	 * <p/>
	 * Through experimentation the mean instead of a quadratic fit was found to produce a better
	 * result.  Most papers seem to recommend using the quadratic.
	 *
	 * @param pt        Point in image coordinates
	 * @param dist		Distsance two points are from each other approximately
	 * @param x0        intensity image x offset
	 * @param y0        intensity image y offset
	 * @param intensity Intensity image
	 * @return Sub-pixel point location
	 */
	private Point2D_F64 refineSubpixel(Point2D_F64 pt,
									   int dist ,
									   int x0, int y0,
									   ImageFloat32 intensity) {
		// adjust the search area.  For small targets you don't want to search too large of a region
		// or it will bleed into the intensity of neighboring points
		int r = Math.min(dist/4,radius + 3);
		if( r < 1 ) r = 1;
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
	 * Pick the points between two corners on different quads and the initial point for where a calibration
	 * point will be.
	 */
	private void seedPointsFromQuadCorner( List<QuadBlob> blobs ) {

		corners.reset();

		for( int i = 0; i < blobs.size(); i++ ) {
			blobs.get(i).index = i;
		}

		boolean marked[] = new boolean[ blobs.size()]; // todo remove this memory creation

		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob b = blobs.get(i);
			marked[i] = true;

			// the next blob which isn't marked should be connected to next.  The ordering
			// the blobs ensures that this strategy will populate the control points in the correct order
			int N = b.conn.size();
			for( int j = 0; j < N; j++ ) {
				values[j] = b.conn.get(j).index;
			}

			sorter.sort(values,N,indexes);

			for( int j = 0; j < N; j++ ) {
				// index of the connected blob with the lowest index
				int next = indexes[j];

				// avoid adding the same points twice
				if( marked[b.conn.get(next).index] )
					continue;

				QuadBlob c = b.conn.get(next);

				// find the corners and compute the average point
				Point2D_I32 c0 = b.corners.get(b.connIndex.data[next]);

				int indexOfB = c.conn.indexOf(b);
				Point2D_I32 c1 = c.corners.get(c.connIndex.data[indexOfB]);

				int x = (c0.x+c1.x)/2;
				int y = (c0.y+c1.y)/2;

				corners.grow().set(x,y);
			}


		}
	}

	private void meanShiftBlobCorners( ImageFloat32 intensity , ImageRectangle rect ) {
		localPeak.setImage(intensity);
		for( int i = 0; i < corners.size(); i++ ) {
			Point2D_F32 c = corners.get(i);
			localPeak.search(c.x - rect.x0, c.y - rect.y0);
			c.x = localPeak.getPeakX() + rect.x0;
			c.y = localPeak.getPeakY() + rect.y0;
		}
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
			ImageFloat32 out = wholeImage.subimage(targetRect.x0, targetRect.y0, targetRect.x1, targetRect.y1, null);
			out.setTo(found);
		}
	}

	public OrderChessboardQuadBlobs getOrderAlg() {
		return orderAlg;
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

	public void setUserBinaryThreshold(double userBinaryThreshold) {
		this.userBinaryThreshold = userBinaryThreshold;
	}

	public double getUserBinaryThreshold() {
		return userBinaryThreshold;
	}

	public int getUserAdaptiveRadius() {
		return userAdaptiveRadius;
	}

	public void setUserAdaptiveRadius(int userAdaptiveRadius) {
		this.userAdaptiveRadius = userAdaptiveRadius;
	}

	public double getUserAdaptiveBias() {
		return userAdaptiveBias;
	}

	public void setUserAdaptiveBias(double userAdaptiveBias) {
		this.userAdaptiveBias = userAdaptiveBias;
	}

	public boolean isFoundBound() {
		return foundBound;
	}
}
