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

package gecv.alg.interpolate;

import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class InterpolateOps {

	public static void scale( ImageFloat32 input , ImageFloat32 output ,
							  InterpolatePixel<ImageFloat32> interpolation )
	{
		float ratioW = (float)input.width/(float)output.width;
		float ratioH = (float)input.height/(float)output.height;

		interpolation.setImage(input);
		
		for( int y = 0; y < output.height; y++ ) {
			for( int x = 0; x < output.width; x++ ) {
				float v = interpolation.get(x*ratioW,y*ratioH);
				output.set(x,y,v);
			}
		}
	}
}
