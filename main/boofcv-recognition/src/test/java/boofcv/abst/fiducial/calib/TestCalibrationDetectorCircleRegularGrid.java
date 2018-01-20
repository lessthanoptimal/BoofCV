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
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.struct.image.GrayF32;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestCalibrationDetectorCircleRegularGrid extends GenericPlanarCalibrationDetectorChecks {

	public TestCalibrationDetectorCircleRegularGrid() {
		targetConfigs.add( new ConfigCircleRegularGrid(4, 3, 30,50));

		// Does a good job detecting the ellipses, but a shit job determining with the tangent points
		// The lens distortion moves them so that they aren't even close
		fisheyeMatchTol = 7;
	}

	/**
	 * Reduce the intensity of fisheye distortion by moving the markers away from the border
	 */
	@Override
	protected void createFisheyePoses() {
		Se3_F64 markerToWorld = new Se3_F64();
		// up close exploding - center
		markerToWorld.T.set(0,0,0.08);
		fisheye_poses.add(markerToWorld.copy());

		// up close exploding - left
		markerToWorld.T.set(0.1,0,0.18);
		fisheye_poses.add(markerToWorld.copy());

		markerToWorld.T.set(0.25,0,0.2);
		fisheye_poses.add(markerToWorld.copy());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,-0.2,0,markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

		markerToWorld.T.set(0.3,0,0.15);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,-1,0,markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,-1,0.15,markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,-1,0.27,markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

	}

	@Override
	public void renderTarget(Object layout, double length3D , GrayF32 image, List<Point2D_F64> points2D) {

		ConfigCircleRegularGrid config = (ConfigCircleRegularGrid)layout;


		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(40,1);

		double radiusPixels = 20;
		double centerDistancePixels = 2*radiusPixels*config.centerDistance/config.circleDiameter;

		renderer.circleRegular(config.numRows,config.numCols,radiusPixels*2,centerDistancePixels);

//		ShowImages.showWindow(renderer.getBufferred(),"Rendered",true);
//		BoofMiscOps.sleep(100000);

		image.setTo(renderer.getGrayF32());

		double centerDistanceWorld = length3D*centerDistancePixels/(double)image.getWidth();
		double radiusWorld = length3D*radiusPixels/(double)image.getWidth();

		points2D.clear();
		points2D.addAll( CalibrationDetectorCircleRegularGrid.
				createLayout(config.numRows, config.numCols, centerDistanceWorld,radiusWorld*2 ));
	}

	@Override
	public DetectorFiducialCalibration createDetector(Object layout) {
		return FactoryFiducialCalibration.circleRegularGrid((ConfigCircleRegularGrid)layout);
	}
}
