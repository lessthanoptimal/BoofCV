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

package boofcv.abst.sfm.d3;

import boofcv.alg.distort.DoNothingPixelTransform_F32;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageGray;
import georegression.struct.se.Se3_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Performs empirical validation of depth visual odometry algorithms using synthetic images.  Only a crude test
 *
 * @author Peter Abeles
 */
public abstract class CheckVisualOdometryDepthSim<I extends ImageGray,Depth extends ImageGray>
	extends VideoSequenceSimulator<I>
{
	CameraPinholeRadial param = new CameraPinholeRadial(200,201,0,width/2,height/2,width,height).fsetRadial(0,0);
	DepthVisualOdometry<I,Depth> algorithm;

	I left;
	Depth depth;

	protected int numSquares = 100;

	double depthUnits = 0.002;
	double tolerance = 0.02;

	public CheckVisualOdometryDepthSim(Class<I> inputType, Class<Depth> depthType) {
		super(320, 240, inputType);

		left = GeneralizedImageOps.createSingleBand(inputType,width,height);
		depth = GeneralizedImageOps.createSingleBand(depthType,width,height);

		createSquares(numSquares,1,2);
	}

	public CheckVisualOdometryDepthSim(Class<I> inputType, Class<Depth> depthType, double tolerance) {
		this(inputType,depthType);
		this.tolerance = tolerance;
	}

	public void setAlgorithm(DepthVisualOdometry<I,Depth>  algorithm) {
		this.algorithm = algorithm;
	}

	@Test
	public void moveForward() {
		algorithm.reset();
		algorithm.setCalibration(param, new DoNothingPixelTransform_F32());

		Se3_F64 worldToLeft = new Se3_F64();

		for( int i = 0; i < 10; i++ ) {
			worldToLeft.getT().z = i*0.05;

			// render the images
			setIntrinsic(param);
			left.setTo(render(worldToLeft));
			renderDepth(worldToLeft,depth,depthUnits);

			// process the images
			assertTrue(algorithm.process(left,depth));

			// Compare to truth.  Only go for a crude approximation
			Se3_F64 foundWorldToLeft = algorithm.getCameraToWorld().invert(null);

//			worldToLeft.getT().print();
//			foundWorldToLeft.getT().print();

			assertTrue(MatrixFeatures.isIdentical(foundWorldToLeft.getR(),worldToLeft.getR(),0.1));
			assertTrue(foundWorldToLeft.getT().distance(worldToLeft.getT()) < tolerance );
		}
	}
}
