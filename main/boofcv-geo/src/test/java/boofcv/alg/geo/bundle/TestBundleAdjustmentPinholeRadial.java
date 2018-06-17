/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentPinholeRadial {
	@Test
	public void withSkew() {
		double[][]parameters = new double[][]{{300,200,400,400,0.01,0.02,-0.001,0.002,0.1},{400,600,1000,1000,0.01,0.02,-0.001,0.002,2}};
		new GenericChecksBundleAdjustmentCamera(new BundleAdjustmentPinholeRadial(false),0.02){}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	public void withoutSkew() {
		double[][]parameters = new double[][]{{300,200,400,400,0.01,0.02,-0.001,0.002},{400,600,1000,1000,0.01,0.02,-0.001,0.002}};
		new GenericChecksBundleAdjustmentCamera(new BundleAdjustmentPinholeRadial(true),0.02){}
				.setParameters(parameters)
				.checkAll();
	}
}