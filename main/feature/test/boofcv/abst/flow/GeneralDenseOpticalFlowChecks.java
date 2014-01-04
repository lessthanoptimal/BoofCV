/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.flow;

import boofcv.struct.image.ImageBase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public abstract class GeneralDenseOpticalFlowChecks<T extends ImageBase>
{
	/**
	 * Makes sure it attempts to compute flow through out the whole image.  Specially checks the image border
	 * to see if those are skipped
	 */
	@Test
	public void processWholeImage() {
		fail("implement");
	}

	/**
	 * Very simple test where every pixel moves at the same speed along x and or y direction
	 */
	@Test
	public void checkPlanarMotion() {
		fail("Implement");
	}
}
