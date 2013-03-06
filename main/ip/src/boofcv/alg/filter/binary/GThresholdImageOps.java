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

package boofcv.alg.filter.binary;

import boofcv.struct.image.*;


/**
 * Weakly typed version of {@link ThresholdImageOps}.
 *
 * @author Peter Abeles
 */
public class GThresholdImageOps {

	public static <T extends ImageSingleBand>
	ImageUInt8 threshold( T input , ImageUInt8 output ,
						  double threshold , boolean down )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.threshold((ImageFloat32)input,output,(float)threshold,down);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.threshold((ImageUInt8)input,output,(int)threshold,down);
		} else if( input instanceof ImageUInt16) {
			return ThresholdImageOps.threshold((ImageUInt16)input,output,(int)threshold,down);
		} else if( input instanceof ImageSInt16) {
			return ThresholdImageOps.threshold((ImageSInt16)input,output,(int)threshold,down);
		} else if( input instanceof ImageSInt32 ) {
			return ThresholdImageOps.threshold((ImageSInt32)input,output,(int)threshold,down);
		} else if( input instanceof ImageFloat64 ) {
			return ThresholdImageOps.threshold((ImageFloat64)input,output,threshold,down);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}
}
