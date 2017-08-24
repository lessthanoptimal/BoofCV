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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.fiducial.calib.circle.TestDetectCircleRegularGrid;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.image.ConvertImage;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorCircleRegularGrid extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigCircleRegularGrid config =
			new ConfigCircleRegularGrid(5, 4, 16,50);

	public TestCalibrationDetectorCircleRegularGrid() {
		width = 500;
		height = 600;
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {

	}

	@Override
	public void renderTarget(GrayF32 original, List<CalibrationObservation> solutions) {
		Affine2D_F64 affine = new Affine2D_F64(1,0,0,1,100,100);

		List<Point2D_F64> keypoints = new ArrayList<>();
		List<Point2D_F64> centers = new ArrayList<>();

		double diameterToDistance = config.centerDistance/config.circleDiameter;
		double radiusToDistance = diameterToDistance*2.0;
		int radiusPixels = 20;

		GrayU8 imageU8 = new GrayU8(original.width,original.height);
		TestDetectCircleRegularGrid.render(config.numRows,config.numCols,radiusPixels,
				radiusToDistance*radiusPixels,
				affine,keypoints,centers,imageU8);
		ConvertImage.convert(imageU8,original);

//		ShowImages.showWindow(original,"ASdasd");
//		BoofMiscOps.sleep(10000);

		CalibrationObservation solution = new CalibrationObservation();
		for (int i = 0; i < keypoints.size(); i++) {
			solution.add(keypoints.get(i),i);
		}
		solutions.add( solution );
	}

	@Override
	public DetectorFiducialCalibration createDetector() {
		return FactoryFiducialCalibration.circleRegularGrid(config);
	}
}
