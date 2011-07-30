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

package gecv.struct.pyramid;

import gecv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImagePyramid {

	@Test
	public void getScalingAtLayer() {
		ImagePyramid<ImageUInt8> pyramid = new DiscreteImagePyramid<ImageUInt8>(true,null,1,2,3);


		assertEquals(1,pyramid.getScalingAtLayer(0),1e-4);
		assertEquals(2,pyramid.getScalingAtLayer(1),1e-4);
		assertEquals(6,pyramid.getScalingAtLayer(2),1e-4);
	}
}
