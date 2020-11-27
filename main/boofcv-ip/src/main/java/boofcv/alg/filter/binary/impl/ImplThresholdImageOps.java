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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>Operations for thresholding images and converting them into a binary image.</p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplThresholdImageOps</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.binary.impl.GenerateImplThresholdImageOps")
@SuppressWarnings("Duplicates")
public class ImplThresholdImageOps {

	public static GrayU8 threshold( GrayF32 input , GrayU8 output ,
										float threshold , boolean down )
	{
		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) > threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 threshold( GrayF64 input , GrayU8 output ,
										double threshold , boolean down )
	{
		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) > threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 threshold( GrayU8 input , GrayU8 output ,
										int threshold , boolean down )
	{
		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]& 0xFF) <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]& 0xFF) > threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 threshold( GrayS16 input , GrayU8 output ,
										int threshold , boolean down )
	{
		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) > threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 threshold( GrayU16 input , GrayU8 output ,
										int threshold , boolean down )
	{
		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]& 0xFFFF) <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]& 0xFFFF) > threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 threshold( GrayS32 input , GrayU8 output ,
										int threshold , boolean down )
	{
		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for( int i = input.width; i>0; i-- ) {
					output.data[indexOut++] = (byte)((input.data[indexIn++]) > threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 localMean( GrayU8 input , GrayU8 output ,
											 ConfigLength width , float scale , boolean down ,
											 GrayU8 storage1 , GrayU8 storage2 ,
											 @Nullable GrowArray<DogArray_I32> storage3 ) {

		int radius = width.computeI(Math.min(input.width,input.height))/2;

		GrayU8 mean = storage1;

		BlurImageOps.mean(input,mean,radius,storage2,storage3);

		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = mean.startIndex + y*mean.stride;

				int end = indexIn + input.width;

				while(indexIn < end) {
					float threshold = (mean.data[indexMean++]& 0xFF) * scale;
					output.data[indexOut++] = (input.data[indexIn++]& 0xFF) <= threshold ? (byte)1:0;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = mean.startIndex + y*mean.stride;

				int end = indexIn + input.width;

				while(indexIn < end) {
					float threshold = (mean.data[indexMean++]& 0xFF);
					output.data[indexOut++] = (input.data[indexIn++]& 0xFF)*scale > threshold ? (byte)1:0;
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 localGaussian( GrayU8 input , GrayU8 output ,
										ConfigLength width , float scale , boolean down ,
										GrayU8 storage1 , GrayU8 storage2 ) {

		int radius = width.computeI(Math.min(input.width,input.height))/2;

		GrayU8 blur = storage1;

		BlurImageOps.gaussian(input,blur,-1,radius,storage2);

		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = blur.startIndex + y*blur.stride;

				int end = indexIn + input.width;

				for( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {
					float threshold = (blur.data[indexMean]& 0xFF) * scale;

					if( (input.data[indexIn]& 0xFF) <= threshold )
						output.data[indexOut] = 1;
					else
						output.data[indexOut] = 0;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = blur.startIndex + y*blur.stride;

				int end = indexIn + input.width;

				for( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {
					int threshold = (blur.data[indexMean]& 0xFF);

					if( (input.data[indexIn]& 0xFF) * scale > threshold )
						output.data[indexOut] = 1;
					else
						output.data[indexOut] = 0;
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 localMean( GrayU16 input , GrayU8 output ,
											 ConfigLength width , float scale , boolean down ,
											 GrayU16 storage1 , GrayU16 storage2 ,
											 @Nullable GrowArray<DogArray_I32> storage3 ) {

		int radius = width.computeI(Math.min(input.width,input.height))/2;

		GrayU16 mean = storage1;

		BlurImageOps.mean(input,mean,radius,storage2,storage3);

		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = mean.startIndex + y*mean.stride;

				int end = indexIn + input.width;

				while(indexIn < end) {
					float threshold = (mean.data[indexMean++]& 0xFFFF) * scale;
					output.data[indexOut++] = (input.data[indexIn++]& 0xFFFF) <= threshold ? (byte)1:0;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = mean.startIndex + y*mean.stride;

				int end = indexIn + input.width;

				while(indexIn < end) {
					float threshold = (mean.data[indexMean++]& 0xFFFF);
					output.data[indexOut++] = (input.data[indexIn++]& 0xFFFF)*scale > threshold ? (byte)1:0;
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 localGaussian( GrayU16 input , GrayU8 output ,
										ConfigLength width , float scale , boolean down ,
										GrayU16 storage1 , GrayU16 storage2 ) {

		int radius = width.computeI(Math.min(input.width,input.height))/2;

		GrayU16 blur = storage1;

		BlurImageOps.gaussian(input,blur,-1,radius,storage2);

		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = blur.startIndex + y*blur.stride;

				int end = indexIn + input.width;

				for( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {
					float threshold = (blur.data[indexMean]& 0xFFFF) * scale;

					if( (input.data[indexIn]& 0xFFFF) <= threshold )
						output.data[indexOut] = 1;
					else
						output.data[indexOut] = 0;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = blur.startIndex + y*blur.stride;

				int end = indexIn + input.width;

				for( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {
					int threshold = (blur.data[indexMean]& 0xFFFF);

					if( (input.data[indexIn]& 0xFFFF) * scale > threshold )
						output.data[indexOut] = 1;
					else
						output.data[indexOut] = 0;
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 localMean( GrayF32 input , GrayU8 output ,
											 ConfigLength width , float scale , boolean down ,
											 GrayF32 storage1 , GrayF32 storage2 ,
											 @Nullable GrowArray<DogArray_F32> storage3 ) {

		int radius = width.computeI(Math.min(input.width,input.height))/2;

		GrayF32 mean = storage1;

		BlurImageOps.mean(input,mean,radius,storage2,storage3);

		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = mean.startIndex + y*mean.stride;

				int end = indexIn + input.width;

				while(indexIn < end) {
					float threshold = (mean.data[indexMean++]) * scale;
					output.data[indexOut++] = (input.data[indexIn++]) <= threshold ? (byte)1:0;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = mean.startIndex + y*mean.stride;

				int end = indexIn + input.width;

				while(indexIn < end) {
					float threshold = (mean.data[indexMean++]);
					output.data[indexOut++] = (input.data[indexIn++])*scale > threshold ? (byte)1:0;
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}

	public static GrayU8 localGaussian( GrayF32 input , GrayU8 output ,
										ConfigLength width , float scale , boolean down ,
										GrayF32 storage1 , GrayF32 storage2 ) {

		int radius = width.computeI(Math.min(input.width,input.height))/2;

		GrayF32 blur = storage1;

		BlurImageOps.gaussian(input,blur,-1,radius,storage2);

		if( down ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = blur.startIndex + y*blur.stride;

				int end = indexIn + input.width;

				for( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {
					float threshold = (blur.data[indexMean]) * scale;

					if( (input.data[indexIn]) <= threshold )
						output.data[indexOut] = 1;
					else
						output.data[indexOut] = 0;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for( int y = 0; y < input.height; y++ ) {
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;
				int indexMean = blur.startIndex + y*blur.stride;

				int end = indexIn + input.width;

				for( ; indexIn < end; indexIn++ , indexOut++, indexMean++ ) {
					float threshold = (blur.data[indexMean]);

					if( (input.data[indexIn]) * scale > threshold )
						output.data[indexOut] = 1;
					else
						output.data[indexOut] = 0;
				}
			}
			//CONCURRENT_ABOVE });
		}

		return output;
	}


}
