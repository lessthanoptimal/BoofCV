/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.color;

import boofcv.alg.color.impl.ImplColorRgb;
import boofcv.alg.color.impl.ImplColorRgb_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.*;

/**
 * <p>Contains functions related to working with RGB images and converting RGB images to gray-scale using a weighted
 * equation. The weighted equation is designed to mimic the intensity seen by the human eye.</p>
 *
 * <b>Weighted Equation</b><br>
 * gray = 0.299*r + 0.587*g + 0.114*b
 *
 * @author Peter Abeles
 */
public class ColorRgb {

	public static int rgbToGray_Weighted( int r , int g , int b ) {
		return (int)(0.299*r + 0.587*g + 0.114*b);
	}

	public static float rgbToGray_Weighted( float r , float g , float b ) {
		return 0.299f*r + 0.587f*g + 0.114f*b;
	}

	public static double rgbToGray_Weighted( double r , double g , double b ) {
		return 0.299*r + 0.587*g + 0.114*b;
	}

	public static void rgbToGray_Weighted( ImageMultiBand rgb , ImageGray gray ) {
		gray.reshape(rgb.width,rgb.height);
		switch( rgb.getImageType().getFamily() ) {
			case PLANAR:
				if( gray instanceof GrayU8 ) {
					if(BoofConcurrency.USE_CONCURRENT ) {
						ImplColorRgb_MT.rgbToGray_Weighted_U8((Planar<GrayU8>)rgb,(GrayU8)gray);
					} else {
						ImplColorRgb.rgbToGray_Weighted_U8((Planar<GrayU8>)rgb,(GrayU8)gray);
					}
				} else if( gray instanceof GrayF32 ) {
					if(BoofConcurrency.USE_CONCURRENT ) {
						ImplColorRgb_MT.rgbToGray_Weighted_F32((Planar<GrayF32>)rgb,(GrayF32)gray);
					} else {
						ImplColorRgb.rgbToGray_Weighted_F32((Planar<GrayF32>)rgb,(GrayF32)gray);
					}
				} else if( gray instanceof GrayF64 ) {
					if(BoofConcurrency.USE_CONCURRENT ) {
						ImplColorRgb_MT.rgbToGray_Weighted_F64((Planar<GrayF64>)rgb,(GrayF64)gray);
					} else {
						ImplColorRgb.rgbToGray_Weighted_F64((Planar<GrayF64>)rgb,(GrayF64)gray);
					}
				} else {
					throw new IllegalArgumentException("Unsupported type "+gray.getClass().getSimpleName());
				}
				break;

			case INTERLEAVED:
				if( gray instanceof GrayU8 ) {
					if(BoofConcurrency.USE_CONCURRENT ) {
						ImplColorRgb_MT.rgbToGray_Weighted((InterleavedU8)rgb,(GrayU8)gray);
					} else {
						ImplColorRgb.rgbToGray_Weighted((InterleavedU8)rgb,(GrayU8)gray);
					}
				} else if( gray instanceof GrayF32 ) {
					if(BoofConcurrency.USE_CONCURRENT ) {
						ImplColorRgb_MT.rgbToGray_Weighted((InterleavedF32)rgb,(GrayF32)gray);
					} else {
						ImplColorRgb.rgbToGray_Weighted((InterleavedF32)rgb,(GrayF32)gray);
					}
				} else if( gray instanceof GrayF64 ) {
					if(BoofConcurrency.USE_CONCURRENT ) {
						ImplColorRgb_MT.rgbToGray_Weighted((InterleavedF64)rgb,(GrayF64)gray);
					} else {
						ImplColorRgb.rgbToGray_Weighted((InterleavedF64)rgb,(GrayF64)gray);
					}
				} else {
					throw new IllegalArgumentException("Unsupported type "+gray.getClass().getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("rgb must be planar or interleaved");
		}
	}
}
