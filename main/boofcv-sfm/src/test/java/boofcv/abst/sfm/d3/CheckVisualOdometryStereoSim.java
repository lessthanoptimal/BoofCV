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

package boofcv.abst.sfm.d3;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageGray;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performs empirical validation of stereo visual odometry algorithms using synthetic images. Only a crude test
 *
 * @author Peter Abeles
 */
abstract class CheckVisualOdometryStereoSim<I extends ImageGray<I>>
		extends VideoSequenceSimulator<I> {
	StereoParameters param = createStereoParam();

	I left;
	I right;

	protected int numSquares = 100;

	double tolerance = 0.02;

	protected CheckVisualOdometryStereoSim( Class<I> inputType ) {
		super(320, 240, inputType);

		// Turn off threads to make results repeatable
		BoofConcurrency.USE_CONCURRENT = false;

		left = GeneralizedImageOps.createSingleBand(inputType, width, height);
		right = GeneralizedImageOps.createSingleBand(inputType, width, height);

		setIntrinsic(param.getLeft());
		createSquares(numSquares, 1, 2);
	}

	protected CheckVisualOdometryStereoSim( Class<I> inputType, double tolerance ) {
		this(inputType);
		this.tolerance = tolerance;
	}

	public abstract StereoVisualOdometry<I> createAlgorithm();

	@Test
	void changeInputSize() {
		StereoVisualOdometry<I> algorithm = createAlgorithm();

		I leftSmall = GeneralizedImageOps.createSingleBand(inputType, width/2, height/2);
		I rightSmall = GeneralizedImageOps.createSingleBand(inputType, width/2, height/2);

		I leftLarge = GeneralizedImageOps.createSingleBand(inputType, width, height);
		I rightLarge = GeneralizedImageOps.createSingleBand(inputType, width, height);

		GImageMiscOps.fillUniform(leftSmall, rand, 0, 100);
		GImageMiscOps.fillUniform(leftSmall, rand, 0, 100);

		GImageMiscOps.fillUniform(leftLarge, rand, 0, 100);
		GImageMiscOps.fillUniform(rightLarge, rand, 0, 100);

		StereoParameters paramSmall = createStereoParam();
		paramSmall.left.width = paramSmall.right.width = leftSmall.width;
		paramSmall.left.height = paramSmall.right.height = leftSmall.height;

		algorithm.reset();
		algorithm.setCalibration(paramSmall);
		assertTrue(algorithm.process(leftSmall, rightSmall));

		StereoParameters paramLarge = createStereoParam();
		paramLarge.left.width = paramLarge.right.width = leftLarge.width;
		paramLarge.left.height = paramLarge.right.height = leftLarge.height;

		algorithm.reset();
		algorithm.setCalibration(paramLarge);
		assertTrue(algorithm.process(leftLarge, rightSmall));
	}

	@Test
	void moveForward() {
		StereoVisualOdometry<I> algorithm = createAlgorithm();

		algorithm.reset();
		algorithm.setCalibration(param);

		Se3_F64 worldToLeft = new Se3_F64();
		Se3_F64 worldToRight = new Se3_F64();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		for (int i = 0; i < 10; i++) {
//			System.out.println("Step i = "+i);
			worldToLeft.getT().z = i*0.05;

			worldToLeft.concat(leftToRight, worldToRight);

			// render the images
			setIntrinsic(param.getLeft());
			left.setTo(render(worldToLeft));
			setIntrinsic(param.getRight());
			right.setTo(render(worldToRight));

			// process the images
			assertTrue(algorithm.process(left, right));

			// Compare to truth. Only go for a crude approximation
			Se3_F64 foundWorldToLeft = algorithm.getCameraToWorld().invert(null);

//			worldToLeft.getT().print();
//			foundWorldToLeft.getT().print();

			assertTrue(MatrixFeatures_DDRM.isIdentical(foundWorldToLeft.getR(), worldToLeft.getR(), 0.1));
			assertTrue(foundWorldToLeft.getT().distance(worldToLeft.getT()) < tolerance);
		}
	}

	/**
	 * Move in forward direction, then give it a completely black frame which should cause it to fail motion estimation,
	 * then give it the real frame. It should resume VO as usual. If an implementation should not handle this situation
	 * gracefully then override this test and skip it.
	 */
	@Test
	void singleBadFrame() {
		StereoVisualOdometry<I> algorithm = createAlgorithm();

		algorithm.reset();
		algorithm.setCalibration(param);

		Se3_F64 worldToLeft = new Se3_F64();
		Se3_F64 worldToRight = new Se3_F64();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		int traveled = 0;
		for (int i = 0; i < 5; i++) {
			boolean badFrame = false;
			if (i == 3) {
				badFrame = true;
				GImageMiscOps.fill(left, 0);
				GImageMiscOps.fill(right, 0);
			} else {
				worldToLeft.getT().z = traveled++*0.05;
				worldToLeft.concat(leftToRight, worldToRight);

				// render the images
				setIntrinsic(param.getLeft());
				left.setTo(render(worldToLeft));
				setIntrinsic(param.getRight());
				right.setTo(render(worldToRight));
			}
			// process the images
			assertEquals(!badFrame, algorithm.process(left, right));

			if (badFrame)
				continue;

			// Compare to truth. Only go for a crude approximation
			Se3_F64 foundWorldToLeft = algorithm.getCameraToWorld().invert(null);

			assertTrue(MatrixFeatures_DDRM.isIdentical(foundWorldToLeft.getR(), worldToLeft.getR(), 0.1));
			assertTrue(foundWorldToLeft.getT().distance(worldToLeft.getT()) < tolerance);
		}
	}

	public StereoParameters createStereoParam() {
		StereoParameters ret = new StereoParameters();

		ret.setRightToLeft(new Se3_F64());
		ret.getRightToLeft().getT().setTo(-0.2, 0.001, -0.012);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.001, -0.01, 0.0023, ret.getRightToLeft().getR());

		ret.left = new CameraPinholeBrown(200, 201, 0, width/2, height/2, width, height).fsetRadial(0, 0);
		ret.right = new CameraPinholeBrown(199, 200, 0, width/2 + 2, height/2 - 6, width, height).fsetRadial(0, 0);

		return ret;
	}
}
