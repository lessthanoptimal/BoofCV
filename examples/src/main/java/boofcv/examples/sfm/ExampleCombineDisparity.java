/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.sfm;

import boofcv.alg.mvs.MultiViewToFusedDisparity;
import boofcv.struct.image.GrayU8;

/**
 * This example shows how multiple disparity images computed with a common "center" image can be combined
 * into a single disparity image that has less noise and simplify processing. This is a common intermediate
 * step in a MVS pipeline.
 *
 * @author Peter Abeles
 */
public class ExampleCombineDisparity {
	public static void main( String[] args ) {

		// TODO scene previously computed

		// TODO load selected images


		// This is the code which combines/fuses multiple disparity images together. It employs a very simple
		// algorithm based on voting. See class description for details.
		var fuser = new MultiViewToFusedDisparity<GrayU8>();

//		fuser.initialize(scene, images);
//
//		fuser.process(3,pairs);

	}
}
