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

package boofcv.alg.sfm.robust;

import boofcv.alg.sfm.overhead.CameraPlaneProjection;
import boofcv.struct.sfm.PlanePtPixel;
import georegression.fitting.MotionTransformPoint;
import georegression.fitting.se.MotionSe2PointSVD_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses {@link georegression.fitting.MotionTransformPoint} to estimate the rigid body motion
 * from key-frame to current-frame in 2D between two observations of a point on the plane.
 *
 * @author Peter Abeles
 */
public class GenerateSe2_PlanePtPixel implements
		ModelGenerator<Se2_F64, PlanePtPixel> {
	// estimates rigid body motion from two sets of associated points
	@Getter @Setter MotionTransformPoint<Se2_F64, Point2D_F64> estimator = new MotionSe2PointSVD_F64();

	// code for projection to/from plane
	@Getter private CameraPlaneProjection planeProjection = new CameraPlaneProjection();

	List<Point2D_F64> from = new ArrayList<>();
	DogArray<Point2D_F64> to = new DogArray<>(Point2D_F64::new);

	public GenerateSe2_PlanePtPixel( CameraPlaneProjection planeProjection ) {
		this.planeProjection = planeProjection;
	}

	public GenerateSe2_PlanePtPixel() {}

	/**
	 * Specify extrinsic camera properties
	 *
	 * @param planeToCamera Transform from plane to camera reference frame
	 */
	public void setExtrinsic( Se3_F64 planeToCamera ) {
		planeProjection.setPlaneToCamera(planeToCamera, true);
	}

	@Override
	public boolean generate( List<PlanePtPixel> dataSet, Se2_F64 keyToCurr ) {
		from.clear();
		to.reset();

		for (int i = 0; i < dataSet.size(); i++) {
			PlanePtPixel p = dataSet.get(i);

			Point2D_F64 planeCurr = to.grow();

			// project current observation onto the plane
			if (planeProjection.normalToPlane(p.normalizedCurr.x, p.normalizedCurr.y, planeCurr)) {
				from.add(p.getPlaneKey());
			} else {
				to.removeTail();
			}
		}

		if (!estimator.process(from, to.toList()))
			return false;

		keyToCurr.setTo(estimator.getTransformSrcToDst());
		return true;
	}

	@Override
	public int getMinimumPoints() {
		return estimator.getMinimumPoints();
	}

	public GenerateSe2_PlanePtPixel newConcurrent() {
		return new GenerateSe2_PlanePtPixel(planeProjection);
	}
}
