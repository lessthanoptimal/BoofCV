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
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link DetectMultiFiducialCalibration} for {@link ECoCheckDetector}.
 *
 * @author Peter Abeles
 */
public class CalibrationDetectorMultiECoCheck implements DetectMultiFiducialCalibration {
	/** The ECoCheck detector */
	@Getter ECoCheckDetector<GrayF32> detector;

	/** Description of the types of markers it can detect */
	@Getter List<ConfigECoCheckMarkers.MarkerShape> markers;

	// Dimension of the most recently processed image
	ImageDimension dimension = new ImageDimension();

	// Storage for layouts. Not sure how big this could get so will use a map to lazily cache
	TIntObjectMap<List<Point2D_F64>> cacheLayouts = new TIntObjectHashMap<>();

	public CalibrationDetectorMultiECoCheck( ECoCheckDetector<GrayF32> detector,
											 List<ConfigECoCheckMarkers.MarkerShape> markers ) {
		this.detector = detector;
		this.markers = markers;
	}

	@Override public void process( GrayF32 input ) {
		detector.process(input);
		dimension.setTo(input.width, input.height);
	}

	@Override public int getDetectionCount() {
		return detector.getFound().size;
	}

	@Override public int getMarkerID( int detectionID ) {
		return detector.getFound().get(detectionID).markerID;
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
			found.points.add(original.get(i).copy());
		}
		return found;
	}

	@Override public List<Point2D_F64> getLayout( int markerID ) {
		List<Point2D_F64> layout = cacheLayouts.get(markerID);
		if (layout != null)
			return layout;

		ECoCheckUtils utils = detector.getUtils();
		ConfigECoCheckMarkers.MarkerShape shape = markers.get(markerID);

		// Create a list of points that defines the layout
		Point3D_F64 p = new Point3D_F64();
		layout = new ArrayList<>();
		int numCorners = shape.getNumCorners();
		for (int cornerID = 0; cornerID < numCorners; cornerID++) {
			utils.cornerToMarker3D(markerID, cornerID, shape.squareSize, p);
			layout.add(cornerID, new Point2D_F64(p.x, p.y));
		}

		cacheLayouts.put(markerID, layout);

		return layout;
	}

	@Override public void setLensDistortion( @Nullable LensDistortionNarrowFOV distortion, int width, int height ) {
		throw new IllegalArgumentException("Not handled yet");
	}
}
