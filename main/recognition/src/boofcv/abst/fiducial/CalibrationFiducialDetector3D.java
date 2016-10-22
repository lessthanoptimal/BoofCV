/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * 3D pose implementation of {@link CalibrationFiducialDetector}
 *
 * @author Peter Abeles
 */
public class CalibrationFiducialDetector3D<T extends ImageGray>
	extends FiducialDetectorToDetector3D<T>
{
	private CalibrationFiducialDetector<T> detector;

	private FastQueue<PointIndex2D_F64> detectedControl = new FastQueue<>(PointIndex2D_F64.class,true);

	public CalibrationFiducialDetector3D(CalibrationFiducialDetector<T> detector) {
		super(detector);
		this.detector = detector;
	}

	@Override
	public double getWidth(int which) {
		return detector.getWidth();
	}

	@Override
	protected List<PointIndex2D_F64> getDetectedControl(int which) {
		detectedControl.reset();

		CalibrationObservation view = detector.getCalibDetector().getDetectedPoints();

		return view.points;
	}

	@Override
	protected List<Point2D3D> getControl3D(int which) {
		return detector.getPoints2D3D();
	}

	public CalibrationFiducialDetector<T> getDetectorImage() {
		return detector;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return detector.getCalibrationPoints();
	}
}
