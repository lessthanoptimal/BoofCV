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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * <p>
 * Detects calibration points inside a chessboard calibration target.  The first processing step is to find the
 * calibration points quickly using a binary image square detector.  The those initial points are used to seed
 * a mean-shift sub-pixel algorithm.
 * </p>
 * <p>
 * The found control points are ensured to be returned in a row-major format with the correct number of rows and columns,
 * with a counter clockwise ordering.  Note that when viewed on the monitor this will appear to be clockwise because
 * the y-axis points down.  If there are multiple valid solution then the solution with the (0,0) grid point closest
 * top the origin is selected.
 * </p>
 *
 * @author Peter Abeles
 */
public class DetectChessboardFiducial<T extends ImageSingleBand, D extends ImageSingleBand> {
	// stores image derivative
	private D derivX;
	private D derivY;

	// radius of the feature being detected
	private int radius;

	// detects the chess board 
	private DetectChessSquarePoints<T> findSeeds;
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

	// rectangle the target is contained inside of
	private ImageRectangle targetRect = new ImageRectangle();
	private Rectangle2D_F64 rect_F64 = new Rectangle2D_F64();

	// true if it found the rectangular bound
	private boolean foundBound;

	// local search algorithm for peaks in corner intensity image
	private SearchLocalPeak<ImageFloat32> localPeak =
			FactorySearchLocalPeak.meanShiftUniform(10, 1e-4f,ImageFloat32.class);

	// work space for thresholding
	private T work1;
	private T work2;

	/**
	 * Configures detection parameters
	 *
	 * @param numCols       Number of columns in the grid.  Target dependent.
	 * @param numRows       Number of rows in the grid.  Target dependent.
	 * @param radius        Side of interest point detection region.  Typically 5
	 * @param relativeSizeThreshold Increases or decreases the minimum allowed blob size. Try 1.0
	 * @param imageType     Type of image being processed
	 */
	public DetectChessboardFiducial(int numCols, int numRows, int radius,
									double relativeSizeThreshold, // TODo remove or re-active this threshold?
									BinaryPolygonConvexDetector<T> detectorSquare,
									Class<T> imageType) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		this.radius = radius;

		work1 = GeneralizedImageOps.createSingleBand(imageType,1,1);
		work2 = GeneralizedImageOps.createSingleBand(imageType,1,1);

		derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);

		intensityAlg = FactoryIntensityPoint.shiTomasi(radius, true, derivType);
//		intensityAlg = FactoryIntensityPoint.harris(radius,0.04f,true,derivType);

		// minContourSize is specified later after the image's size is known
		// TODO make separation configurable?
		findSeeds = new DetectChessSquarePoints<T>(numCols, numRows,4, detectorSquare);

		localPeak.setSearchRadius(2);

		reset();
	}

	/**
	 * Forgets any past history and resets the detector
	 */
	public void reset() {
	}

	public boolean process(T gray) {
		binary.reshape(gray.width, gray.height);
		eroded.reshape(gray.width, gray.height);


		// detect the chess board
		if (!(foundBound = detectChessBoard(gray)))
			return false;

		FastQueue<Point2D_F64> seeds = findSeeds.getCalibrationPoints();
		UtilPoint2D_F64.bounding(seeds.toList(),rect_F64);

		int buffer = radius*3;
		targetRect.x0 = (int)(rect_F64.p0.x-buffer);
		targetRect.y0 = (int)(rect_F64.p0.y-buffer);
		targetRect.x1 = (int)(rect_F64.p1.x+buffer);
		targetRect.y1 = (int)(rect_F64.p1.y+buffer);
		BoofMiscOps.boundRectangleInside(gray,targetRect);

		T subGray = (T) gray.subimage(targetRect.x0, targetRect.y0, targetRect.x1, targetRect.y1);
		derivX.reshape(subGray.width, subGray.height);
		derivY.reshape(subGray.width, subGray.height);

		// extended border to avoid false positives
		GImageDerivativeOps.gradient(DerivativeType.SOBEL,subGray, derivX, derivY, BorderType.EXTENDED);

		// Compute interest point intensity
		intensityAlg.process(subGray, derivX, derivY, null, null, null);

		// use mean-shift to get a more accurate estimate using corner intensity image
		meanShiftBlobCorners(seeds.toList(),intensityAlg.getIntensity(),targetRect);

		return true;
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

		return findSeeds.process(gray,eroded);
	}

	private void meanShiftBlobCorners( List<Point2D_F64> seeds , ImageFloat32 intensity , ImageRectangle rect ) {
		localPeak.setImage(intensity);
		for( int i = 0; i < seeds.size(); i++ ) {
			Point2D_F64 c = seeds.get(i);
			localPeak.search((float)c.x - rect.x0, (float)c.y - rect.y0);
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
			ImageFloat32 out = wholeImage.subimage(targetRect.x0, targetRect.y0, targetRect.x1, targetRect.y1);
			out.setTo(found);
		}
	}
	public DetectChessSquarePoints getFindSeeds() {
		return findSeeds;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return findSeeds.getCalibrationPoints().toList();
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
