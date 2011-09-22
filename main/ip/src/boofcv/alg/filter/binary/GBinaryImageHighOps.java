/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
 * Image type agnostic version of functions inside of {@link BinaryImageHighOps}.
 */
public class GBinaryImageHighOps {

	/**
	 * <p>
	 * Hysteresis thresholding and blob labeling with a connect-4 rule. The input image is thresholded and the resulting
	 * blobs are labeled.
	 * </p>
	 *
	 * <p>
	 * Hysteresis thresholding works by first detecting if a pixel is within a more stringent threshold.  If it is
	 * then a less stringent threshold is used for all the connected pixels. The threshold direction determines
	 * if the lower or upper threshold is more or less stringent.  When thresholding down the the lower threshold
	 * is more stringent and the upper less. The opposite is true for when being thresholded up.
	 * </p>
	 *
	 * @param input Input intensity image. Not modified.
	 * @param output Output labeled binary image. Modified.
	 * @param lowerThreshold Lower threshold.
	 * @param upperThreshold Upper threshold.
	 * @param down If it is being thresholded down or up.
	 * @param work Work image which stores intermediate results and is the same size as the input image.  If null one will be declared internally.
	 * @return Number of blobs found.
	 */
	public static <T extends ImageBase>
			void hysteresisLabel4( T input , ImageSInt32 output ,
								   double lowerThreshold , double upperThreshold , boolean down ,
								   ImageUInt8 work )
	{
		if( input instanceof ImageFloat32 ) {
			BinaryImageHighOps.hysteresisLabel4((ImageFloat32)input,output,(float)lowerThreshold,(float)upperThreshold,down,work);
		} else if( input instanceof ImageFloat64 ) {
			BinaryImageHighOps.hysteresisLabel4((ImageFloat64)input,output,lowerThreshold,upperThreshold,down,work);
		} else if( input instanceof ImageUInt8 ) {
			BinaryImageHighOps.hysteresisLabel4((ImageUInt8)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt16) {
			BinaryImageHighOps.hysteresisLabel4((ImageSInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageUInt16) {
			BinaryImageHighOps.hysteresisLabel4((ImageUInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt32) {
			BinaryImageHighOps.hysteresisLabel4((ImageSInt32)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		}
	}

	/**
	 * <p>
	 * Hysteresis thresholding and blob labeling with a connect-8 rule. The input image is thresholded and the resulting
	 * blobs are labeled.
	 * </p>
	 *
	 * <p>
	 * Hysteresis thresholding works by first detecting if a pixel is within a more stringent threshold.  If it is
	 * then a less stringent threshold is used for all the connected pixels. The threshold direction determines
	 * if the lower or upper threshold is more or less stringent.  When thresholding down the the lower threshold
	 * is more stringent and the upper less. The opposite is true for when being thresholded up.
	 * </p>
	 *
	 * @param input Input intensity image. Not modified.
	 * @param output Output labeled binary image. Modified.
	 * @param lowerThreshold Lower threshold.
	 * @param upperThreshold Upper threshold.
	 * @param down If it is being thresholded down or up.
	 * @param work Work image which stores intermediate results and is the same size as the input image.  If null one will be declared internally.
	 * @return Number of blobs found.
	 */
	public static <T extends ImageBase>
			void hysteresisLabel8( T input , ImageSInt32 output ,
								   double lowerThreshold , double upperThreshold , boolean down ,
								   ImageUInt8 work )
	{
		if( input instanceof ImageFloat32 ) {
			BinaryImageHighOps.hysteresisLabel8((ImageFloat32)input,output,(float)lowerThreshold,(float)upperThreshold,down,work);
		} else if( input instanceof ImageFloat64 ) {
			BinaryImageHighOps.hysteresisLabel8((ImageFloat64)input,output,lowerThreshold,upperThreshold,down,work);
		} else if( input instanceof ImageUInt8 ) {
			BinaryImageHighOps.hysteresisLabel8((ImageUInt8)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt16) {
			BinaryImageHighOps.hysteresisLabel8((ImageSInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageUInt16) {
			BinaryImageHighOps.hysteresisLabel8((ImageUInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt32) {
			BinaryImageHighOps.hysteresisLabel8((ImageSInt32)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		}
	}
}
