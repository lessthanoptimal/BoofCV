/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.slic;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSegmentSlic_U8 {

	/**
	 * Give it an easy image to segment and see how well it does.
	 */
	@Test
	public void easyTest() {
		ImageUInt8 input = new ImageUInt8(30,40);
		ImageSInt32 output = new ImageSInt32(30,40);

		ImageMiscOps.fillRectangle(input, 100, 0, 0, 15, 40);

		SegmentSlic_U8 alg = new SegmentSlic_U8(10,10,10, ConnectRule.EIGHT , ImageUInt8.class);

		alg.process(input,output);


		// TODO make sure that each segment is all one color in the original image
	}

	@Test
	public void stuff() {
		fail("implement");
	}
}
