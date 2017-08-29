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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorSquareGrid extends GenericPlanarCalibrationDetectorChecks {

	private final static ConfigSquareGrid config = new ConfigSquareGrid(3, 2, 30,30);

	public TestCalibrationDetectorSquareGrid() {
		targetConfigs.add(  new ConfigSquareGrid(3, 2, 30,30) );
		fisheyeMatchTol = 4.0;
	}

	@Test
	public void createLayout() {
		List<Point2D_F64> l = CalibrationDetectorSquareGrid.createLayout(3, 2, 0.1, 0.2);

		assertEquals(4*6,l.size());

		double w = l.get(1).x - l.get(0).x;
		double h = l.get(0).y - l.get(4).y ;

		assertEquals(0.1,w,1e-8);
		assertEquals(0.1,h,1e-8);

		double s = l.get(2).x - l.get(1).x;

		assertEquals(0.2, s, 1e-8);
	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {
		ConfigSquareGrid config = (ConfigSquareGrid)layout;

		double squareWidthPixels = 20;
		double spacingPixels = squareWidthPixels*(config.spaceWidth/config.squareWidth);
		double borderPixels = 20;

		int imageWidth = (int)(config.numCols*squareWidthPixels + (config.numCols-1)*spacingPixels + 2*borderPixels+0.5);
		int imageHeight = (int)(config.numRows*squareWidthPixels + (config.numRows-1)*spacingPixels + 2*borderPixels+0.5);

		image.reshape(imageWidth,imageHeight);
		ImageMiscOps.fill(image, 255);

		for (int i = 0; i < config.numRows; i++ ){
			double y = borderPixels + i * (squareWidthPixels+spacingPixels);

			for (int j = 0; j < config.numCols; j++) {
				double x = borderPixels + j * (squareWidthPixels + spacingPixels);

				ImageMiscOps.fillRectangle(image, 0, (int)(x+0.5), (int)(y+0.5),
						(int)squareWidthPixels, (int)squareWidthPixels);
			}
		}

		double squareWidthWorld = length3D*squareWidthPixels/(double)imageWidth;
		double spacingWorld = length3D*spacingPixels/(double)imageWidth;

		points2D.clear();
		points2D.addAll( CalibrationDetectorSquareGrid.
				createLayout(config.numRows, config.numCols, squareWidthWorld,spacingWorld ));
	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.squareGrid((ConfigSquareGrid)layout);
	}
}