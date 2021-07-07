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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociateEuclidean_F64;
import boofcv.alg.sfm.SfmTestHelper;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAssociateStereo2D extends BoofStandardJUnit {

	Se3_F64 leftToRight;
	StereoParameters param;

	Point2D_F64 leftP = new Point2D_F64();
	Point2D_F64 rightP = new Point2D_F64();
	DogArray<Point2D_F64> pointsLeft = new DogArray<>(Point2D_F64::new);
	DogArray<Point2D_F64> pointsRight = new DogArray<>(Point2D_F64::new);
	DogArray<TupleDesc_F64> descLeft, descRight;

	ScoreAssociateEuclidean_F64 scorer = new ScoreAssociateEuclidean_F64();

	@BeforeEach
	public void setup() {
		leftToRight = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.01, -0.001, 0.005, leftToRight.getR());
		leftToRight.getT().setTo(-0.1, 0, 0);

		param = new StereoParameters();
		param.right_to_left = leftToRight.invert(null);

		param.left = new CameraPinholeBrown(400, 500, 0.1, 160, 120, 320, 240).fsetRadial(0, 0);
		param.right = new CameraPinholeBrown(380, 505, 0.05, 165, 115, 320, 240).fsetRadial(0, 0);

		descLeft = new DogArray<>(() -> new TupleDesc_F64(10));
		descRight = new DogArray<>(() -> new TupleDesc_F64(10));

		pointsLeft.reset();
		pointsRight.reset();
	}

	/**
	 * Very simple positive case with only a perfect observation and descriptor
	 */
	@Test void positive() {
		Point3D_F64 X = new Point3D_F64(0.02, -0.5, 3);

		SfmTestHelper.renderPointPixel(param, X, leftP, rightP);
		pointsLeft.grow().setTo(leftP);
		pointsRight.grow().setTo(rightP);

		descLeft.grow();
		descRight.grow();

		AssociateStereo2D<TupleDesc_F64> alg = new AssociateStereo2D<>(scorer, 0.5);

		alg.setCalibration(param);

		alg.setSource(pointsLeft, descLeft);
		alg.setDestination(pointsRight, descRight);

		alg.associate();

		DogArray<AssociatedIndex> matches = alg.getMatches();

		assertEquals(1, matches.size);
	}

	/**
	 * Makes the observation in the left image &gt; right image along x-axis
	 */
	@Test void constraintX() {
		// zap the rotation so that no adjustment should need to be done
		CommonOps_DDRM.setIdentity(param.right_to_left.getR());
		Point3D_F64 X = new Point3D_F64(0.02, -0.5, 3);

		SfmTestHelper.renderPointPixel(param, X, leftP, rightP);

		// mangle the x-axis
		leftP.x = rightP.x - 0.25;

		pointsLeft.grow().setTo(leftP);
		pointsRight.grow().setTo(rightP);

		descLeft.grow();
		descRight.grow();

		AssociateStereo2D<TupleDesc_F64> alg = new AssociateStereo2D<>(scorer, 0.5);

		alg.setCalibration(param);

		alg.setSource(pointsLeft, descLeft);
		alg.setDestination(pointsRight, descRight);

		alg.associate();

		// at the current tolerance they should still match
		assertEquals(1, alg.getMatches().size);

		// make the tolerance tighter
		alg = new AssociateStereo2D<>(scorer, 0.01);
		alg.setCalibration(param);
		alg.setSource(pointsLeft, descLeft);
		alg.setDestination(pointsRight, descRight);
		alg.associate();
		assertEquals(0, alg.getMatches().size);
	}

	/**
	 * Makes sure observations have the same y-axis
	 */
	@Test void constraintY() {
		// zap the rotation so that no adjustment should need to be done
		Point3D_F64 X = new Point3D_F64(0.02, -0.5, 3);

		SfmTestHelper.renderPointPixel(param, X, leftP, rightP);

		// mangle the y-axis
		leftP.y += 0.25;

		pointsLeft.grow().setTo(leftP);
		pointsRight.grow().setTo(rightP);

		descLeft.grow();
		descRight.grow();

		AssociateStereo2D<TupleDesc_F64> alg = new AssociateStereo2D<>(scorer, 0.5);

		alg.setCalibration(param);

		alg.setSource(pointsLeft, descLeft);
		alg.setDestination(pointsRight, descRight);

		alg.associate();

		// at the current tolerance they should still match
		assertEquals(1, alg.getMatches().size);

		// make the tolerance tighter
		alg = new AssociateStereo2D<>(scorer, 0.01);
		alg.setCalibration(param);
		alg.setSource(pointsLeft, descLeft);
		alg.setDestination(pointsRight, descRight);
		alg.associate();
		assertEquals(0, alg.getMatches().size);
	}
}
