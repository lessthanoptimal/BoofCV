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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.fiducial.calib.circle.TestDetectAsymmetricCircleGrid;
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
public class TestCalibrationDetectorCircleAsymmGrid extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigCircleAsymmetricGrid config =
			new ConfigCircleAsymmetricGrid(5, 4, 10,50);

	public TestCalibrationDetectorCircleAsymmGrid() {
		width = 500;
		height = 600;
	}

	@Override
	public void renderTarget(GrayF32 original, List<CalibrationObservation> solutions) {
		Affine2D_F64 affine = new Affine2D_F64(1,0,0,1,100,100);

		List<Point2D_F64> pixels = new ArrayList<>();

		double radiusToDistance = config.centerDistance/config.circleRadius;
		int radiusPixels = 20;

		GrayU8 imageU8 = new GrayU8(original.width,original.height);
		TestDetectAsymmetricCircleGrid.render(config.numRows,config.numCols,radiusPixels,
				(int)(radiusToDistance*radiusPixels),
				affine,pixels,imageU8);
		ConvertImage.convert(imageU8,original);

//		ShowImages.showWindow(original,"ASdasd");
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

		CalibrationObservation solution = new CalibrationObservation();
		for (int i = 0; i < pixels.size(); i++) {
			solution.add(pixels.get(i),i);
		}
		solutions.add( solution );
	}

	@Override
	public DetectorFiducialCalibration createDetector() {
		return FactoryFiducialCalibration.circleAsymmGrid(config);
	}
}
