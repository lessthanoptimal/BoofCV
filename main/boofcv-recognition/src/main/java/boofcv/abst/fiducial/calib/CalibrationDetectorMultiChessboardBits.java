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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectMultiFiducialCalibration;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.calib.chessbits.ChessboardReedSolomonDetector;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.GridShape;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.FastAccess;

import java.util.List;

import static boofcv.abst.fiducial.calib.CalibrationDetectorChessboardX.gridChess;

/**
 * Implementation of {@link DetectMultiFiducialCalibration} for {@link ChessboardReedSolomonDetector}.
 *
 * @author Peter Abeles
 */
public class CalibrationDetectorMultiChessboardBits implements DetectMultiFiducialCalibration {
	ChessboardReedSolomonDetector<GrayF32> detector;

	// Length of a square
	double squareLength;

	// Dimension of the most recently processed image
	ImageDimension dimension = new ImageDimension();

	// Storage for layouts. Not sure how big this could get so will use a map to lazily cache
	TIntObjectMap<List<Point2D_F64>> cacheLayouts = new TIntObjectHashMap<>();

	public CalibrationDetectorMultiChessboardBits( ChessboardReedSolomonDetector<GrayF32> detector,
												   double squareLength ) {
		this.detector = detector;
		this.squareLength = squareLength;
	}

	@Override public void process( GrayF32 input ) {
		detector.process(input);
		dimension.setTo(input.width, input.height);
	}

	@Override public int getDetectionCount() {
		return detector.getFound().size;
	}

	@Override public int getMarkerID( int detectionID ) {
		return detector.getFound().get(detectionID).marker;
	}

	@Override public int getTotalUniqueMarkers() {
		return detector.getUtils().markers.size();
	}

	@Override public CalibrationObservation getDetectedPoints( int detectionID ) {
		FastAccess<PointIndex2D_F64> original = detector.getFound().get(detectionID).corners;
		var found = new CalibrationObservation();
		found.width = dimension.width;
		found.height = dimension.height;

		for (int i = 0; i < original.size; i++) {
			PointIndex2D_F64 p = original.get(i);
			found.add(p.p, p.index);
		}
		return found;
	}

	@Override public List<Point2D_F64> getLayout( int markerID ) {
		List<Point2D_F64> layout = cacheLayouts.get(markerID);
		if (layout != null)
			return layout;

		GridShape shape = detector.getUtils().markers.get(markerID);

		layout = gridChess(shape.rows, shape.cols, squareLength);
		cacheLayouts.put(markerID, layout);

		return layout;
	}

	@Override public void setLensDistortion( LensDistortionNarrowFOV distortion, int width, int height ) {
		throw new IllegalArgumentException("Not handled yet");
	}
}
