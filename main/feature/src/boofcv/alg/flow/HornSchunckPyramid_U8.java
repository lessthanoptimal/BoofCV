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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * Pyramidal implementation of Horn-Schunck based on the discussion in [1].  The problem formulation has been
 * modified from the original found in [2] to account for larger displacements.
 *
 * @author Peter Abeles
 */
public class HornSchunckPyramid_U8 {

	// relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	float w;

	ImageFloat32 flowX = new ImageFloat32(1,1);
	ImageFloat32 flowY = new ImageFloat32(1,1);


	public void process( ImagePyramid<ImageUInt8> image1 ,
						 ImagePyramid<ImageUInt8> image2 ,
						 ImageSInt16[] derivX2 , ImageSInt16[] derivY2 ,
						 ImageFlow output ) {



		for( int i = image1.getNumLayers()-1; i >= 0; i-- ) {

		}
	}

	protected void processLayer( ImageUInt8 image1 , ImageUInt8 image2 ,
								 ImageSInt16 derivX2 , ImageSInt16 derivY2,
								 ImageFloat32 flowX , ImageFloat32 flowY) {


	}
}
