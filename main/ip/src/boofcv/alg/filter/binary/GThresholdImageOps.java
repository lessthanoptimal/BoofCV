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

	public static <T extends ImageSingleBand>
	void thresholdBlobs( T input , ImageSInt32 labeled ,
						 int results[] , int numBlobs ,
						 double threshold , boolean down )
	{
		if( input instanceof ImageFloat32 ) {
			ThresholdImageOps.thresholdBlobs((ImageFloat32)input,labeled,results,numBlobs,(float)threshold,down);
		} else if( input instanceof ImageUInt8 ) {
			ThresholdImageOps.thresholdBlobs((ImageUInt8)input,labeled,results,numBlobs,(int)threshold,down);
		} else if( input instanceof ImageUInt16) {
			ThresholdImageOps.thresholdBlobs((ImageUInt16)input,labeled,results,numBlobs,(int)threshold,down);
		} else if( input instanceof ImageSInt16) {
			ThresholdImageOps.thresholdBlobs((ImageSInt16)input,labeled,results,numBlobs,(int)threshold,down);
		} else if( input instanceof ImageSInt32 ) {
			ThresholdImageOps.thresholdBlobs((ImageSInt32)input,labeled,results,numBlobs,(int)threshold,down);
		} else if( input instanceof ImageFloat64 ) {
			ThresholdImageOps.thresholdBlobs((ImageFloat64)input,labeled,results,numBlobs,threshold,down);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

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
	public static <T extends ImageSingleBand>
			void hysteresisLabel4( T input , ImageSInt32 output ,
								   double lowerThreshold , double upperThreshold , boolean down ,
								   ImageUInt8 work )
	{
		if( input instanceof ImageFloat32 ) {
			ThresholdImageOps.hysteresisLabel4((ImageFloat32)input,output,(float)lowerThreshold,(float)upperThreshold,down,work);
		} else if( input instanceof ImageFloat64 ) {
			ThresholdImageOps.hysteresisLabel4((ImageFloat64)input,output,lowerThreshold,upperThreshold,down,work);
		} else if( input instanceof ImageUInt8 ) {
			ThresholdImageOps.hysteresisLabel4((ImageUInt8)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt16) {
			ThresholdImageOps.hysteresisLabel4((ImageSInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageUInt16) {
			ThresholdImageOps.hysteresisLabel4((ImageUInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt32) {
			ThresholdImageOps.hysteresisLabel4((ImageSInt32)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
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
	public static <T extends ImageSingleBand>
			void hysteresisLabel8( T input , ImageSInt32 output ,
								   double lowerThreshold , double upperThreshold , boolean down ,
								   ImageUInt8 work )
	{
		if( input instanceof ImageFloat32 ) {
			ThresholdImageOps.hysteresisLabel8((ImageFloat32)input,output,(float)lowerThreshold,(float)upperThreshold,down,work);
		} else if( input instanceof ImageFloat64 ) {
			ThresholdImageOps.hysteresisLabel8((ImageFloat64)input,output,lowerThreshold,upperThreshold,down,work);
		} else if( input instanceof ImageUInt8 ) {
			ThresholdImageOps.hysteresisLabel8((ImageUInt8)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt16) {
			ThresholdImageOps.hysteresisLabel8((ImageSInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageUInt16) {
			ThresholdImageOps.hysteresisLabel8((ImageUInt16)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		} else if( input instanceof ImageSInt32) {
			ThresholdImageOps.hysteresisLabel8((ImageSInt32)input,output,(int)lowerThreshold,(int)upperThreshold,down,work);
		}
	}
}
