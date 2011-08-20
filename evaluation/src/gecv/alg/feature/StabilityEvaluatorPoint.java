/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature;

import gecv.struct.image.ImageBase;
import jgrl.struct.affine.Affine2D_F32;


/**
 * Evaluates information extracted at automatically selected points inside the image.  Points
 * are selected using a feature detector.  Once the points have been selected inside the initial image they are
 * transformed to their location in the distorted image.
 *
 * @author Peter Abeles
 */
public class StabilityEvaluatorPoint<T extends ImageBase>
		implements StabilityEvaluator<T> {
	@Override
	public void extractInitial(StabilityAlgorithm alg, T image) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public double[] evaluateImage(StabilityAlgorithm alg, T image, Affine2D_F32 transform) {
		return new double[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String[] getMetricNames() {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}
}
