/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.border.GrowBorder;
import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;

/**
 * <p>
 * Base class for all dense stereo disparity score algorithms whose score's can be processed by
 * {@link DisparitySelect}. The scores for all possible disparities at each pixel is computed for
 * an entire row at once.  Then {@link DisparitySelect} is called to process this score.
 * </p>
 *
 * <p>
 * Score Format:  The index of the score for column i &ge; radiusX + minDisparity at disparity d is: <br>
 * index = imgWidth*(d-minDisparity-radiusX) + i - minDisparity-radiusX<br>
 * Format Comment:<br>
 * This ordering is a bit unnatural when searching for the best disparity, but reduces cache misses
 * when writing.  Performance boost is about 20%-30% depending on max disparity and image size.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DisparityBlockMatchRowFormat
		<Input extends ImageBase<Input>, Disparity extends ImageGray<Disparity>>
{
	// the minimum disparity value (inclusive)
	protected @Getter int disparityMin;
	// maximum allowed image disparity (exclusive)
	protected @Getter int disparityMax;
	// difference between max and min
	protected @Getter int disparityRange;

	// number of score elements: image_width*rangeDisparity
	protected int widthDisparityBlock;

	// radius of the region along x and y axis
	protected @Getter int radiusX,radiusY;
	// size of the region: radius*2 + 1
	protected @Getter int regionWidth,regionHeight;

	// Used to extract a row with the border added to it
	protected @Getter GrowBorder<Input,Object> growBorderL;
	protected @Getter GrowBorder<Input,Object> growBorderR;

	/**
	 * Configures disparity calculation.
	 *
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 */
	public DisparityBlockMatchRowFormat(int regionRadiusX, int regionRadiusY , ImageType<Input> imageType ) {
		this.radiusX = regionRadiusX;
		this.radiusY = regionRadiusY;

		this.regionWidth = regionRadiusX*2+1;
		this.regionHeight = regionRadiusY*2+1;

		growBorderL = (GrowBorder)FactoryImageBorder.createGrowBorder(imageType);
		growBorderR = (GrowBorder)FactoryImageBorder.createGrowBorder(imageType);
	}

	public void setBorder( ImageBorder<Input> border ) {
		growBorderL.setBorder(border.copy());
		growBorderR.setBorder(border.copy());
	}

	/**
	 * Configures the disparity search
	 *
	 * @param disparityMin Minimum disparity that it will check. Must be &ge; 0 and < maxDisparity
	 * @param disparityRange Number of possible disparity values estimated. The max possible disparity is min+range-1.
	 */
	public void configure( int disparityMin , int disparityRange ) {
		if( disparityMin < 0 )
			throw new IllegalArgumentException("Min disparity must be greater than or equal to zero. max="+disparityMin);
		if( disparityRange <= 0 )
			throw new IllegalArgumentException("Disparity range must be more than 0");

		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;
		this.disparityMax = disparityMin+disparityRange-1;
	}

	/**
	 * Computes disparity between two stereo images
	 *
	 * @param left Left rectified stereo image. Input
	 * @param right Right rectified stereo image. Input
	 * @param disparity Disparity between the two images. Output
	 */
	public void process( Input left , Input right , Disparity disparity ) {
		// initialize data structures
		InputSanityCheck.checkSameShape(left, right);

		if( disparityMax > left.width )
			throw new RuntimeException(
					"The maximum disparity is too large for this image size: max size "+left.width);

		// Stores error for all x-coordinates and disparity values along a single row
		widthDisparityBlock = left.width* disparityRange;

		_process(left,right,disparity);
	}

	/**
	 * Inner function that computes the disparity.
	 */
	public abstract void _process( Input left , Input right , Disparity disparity );

	public abstract ImageType<Input> getInputType();

	public abstract Class<Disparity> getDisparityType();

	public int getBorderX() {
		return 0;
	}

	public int getBorderY() {
		return radiusY;
	}

	/**
	 * The maximum possible error for the region
	 */
	public int getMaxRegionError() {
		return regionWidth*regionHeight*getMaxPerPixelError();
	}

	protected abstract int getMaxPerPixelError();
}
