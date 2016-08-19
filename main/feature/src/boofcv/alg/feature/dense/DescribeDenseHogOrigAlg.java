/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.dense;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribeSiftCommon;
import boofcv.alg.filter.derivative.DerivativeReduceType;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.Arrays;

/**
 * <p>
 * Implementation of the Histogram of Oriented Gradients (HOG) [1] dense feature descriptor.  Several variants
 * are described in the paper.  The algorithm used here is the "R-HOG unsigned orientation" variant.  The descriptor
 * is computed from a regular grid of cells and an unsigned histogram is computed.  Unsigned as in the angle is
 * from 0 to 180 degrees instead of 0 to 360.
 * </p>
 *
 * This is a (hopefully) faithful implementation to the algorithm described in the paper.  The descriptors are
 * computed with the following steps.
 * <ol>
 * <li>Compute image gradient using [-1,0,1] kernel</li>
 * <li>Compute histogram for each cell in the image</li>
 * <li>Compute descriptor from blocks of cells</li>
 * <li>Normalize descriptor using SIFT style L2-Hys normalization</li>
 * </ol>
 *
 * <h3>Cell Histogram</h3>
 * The image is broken up into a regular grid of "cells".  Every cell is square with a width of N pixels, where N
 * is a user specified parameter.  Within each cell a histogram is computed.  The number of histogram bins B is
 * specified by the user.  The histogram is created by computing the Euclidean norm for each pixel's gradient
 * inside the cell and then using bilinear interpolation to put it into the two neighboring bins which best match
 * its angle.
 *
 * <h3>Descriptor Blocks</h3>
 * Square blocks, which are M cells wide, are used to compute each descriptor.  The histogram is copied into a
 * {@link TupleDesc_F64} in a row-major fashion and a weight is applied.  The weight is specified for each cell
 * based on its distance from the block's center.  Gaussian weight is used with a standard deviation of 0.5*block_width.
 * Note that blocks do overlap.  See paper for details.
 *
 * <h3>Descriptor Normalization</h3>
 * First L2-normalization is applied to the descriptor.  Then min(0.2,desc[i]) is applied to all elements in the
 * descriptor.  After which L2-normalization is applied again.
 *
 * <h3>Accessing Results</h3>
 * A list of descriptor and their locations is available. The location refers to the top-left most pixel in
 * the region the descriptor is computed from.  These lists are computed in a regular grid with row-major ordering.
 * A request can be made for all descriptors computed from inside a rectangular region.
 *
 * <h3>Multi-Band Images</h3>
 * The gradient is computed for each band individually.  The band with the largest magnitude at that specific
 * pixel is used as the gradient for the pixel.
 *
 * <p>[1] Dalal, Navneet, and Bill Triggs. "Histograms of oriented gradients for human detection." Computer
 * Vision and Pattern Recognition, 2005. CVPR 2005.</p>
 *
 * @author Peter Abeles
 */
public class DescribeDenseHogOrigAlg<Input extends ImageBase> {

	ImageGradient<Input, GrayF32> gradient;

	// gradient of each pixel
	protected GrayF32 derivX = new GrayF32(1,1);
	protected GrayF32 derivY = new GrayF32(1,1);
	// orientation and magnitude of each pixel
	protected GrayF32 orientation = new GrayF32(1,1);
	protected GrayF64 magnitude = new GrayF64(1,1); // stored as F64 instead of F32 for speed

	// Storage for descriptors
	FastQueue<TupleDesc_F64> descriptions;

	// Location of each descriptor in the image, top-left corner (lower extents)
	FastQueue<Point2D_I32> locations = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

	int orientationBins; // number of orientation bins computed in a block
	int widthCell; // number of pixels wide a cell is
	int widthBlock;  // number of cells wide a block is
	int stepBlock; // how many cells are skipped between a block

	// spatial weights applied to each in a block
	// stored in a row major order
	double weights[];

	// type of input image
	ImageType<Input> imageType;

	/**
	 * Configures HOG descriptor computation
	 *
	 * @param orientationBins Number of bins in a cell's histogram.  9 recommended
	 * @param widthCell Number of pixel's wide a cell is.  8 recommended
	 * @param widthBlock Number of cells's wide a black is. 3 recommended
	 * @param stepBlock Number of cells which are skipped between each block
	 */
	public DescribeDenseHogOrigAlg(int orientationBins , int widthCell , int widthBlock ,
								   int stepBlock ,
								   ImageType<Input> imageType )
	{
		if( stepBlock <= 0 )
			throw new IllegalArgumentException("stepBlock must be >= 1");

		this.imageType = imageType;

		gradient = createGradient(imageType);

		this.orientationBins = orientationBins;
		this.widthCell = widthCell;
		this.widthBlock = widthBlock;
		this.stepBlock = stepBlock;

		descriptions = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(DescribeDenseHogOrigAlg.this.orientationBins*
						DescribeDenseHogOrigAlg.this.widthBlock *DescribeDenseHogOrigAlg.this.widthBlock);
			}
		};

		computeCellWeights();
	}

	/**
	 * Given different types input images it creates the correct algorithm for computing the image gradient.  The
	 * actualy calulcation is always done using {@link DerivativeType#THREE}
	 */
	private ImageGradient<Input,GrayF32> createGradient( ImageType<Input> imageType ) {
		ImageGradient<Input,GrayF32> gradient;
		ImageType<GrayF32> typeF32 = ImageType.single(GrayF32.class);

		if( imageType.getDataType() != ImageDataType.F32 )
			throw new IllegalArgumentException("Input image type must be F32");

		if( imageType.getFamily() == ImageType.Family.GRAY) {
			gradient = FactoryDerivative.gradient(DerivativeType.THREE,imageType, typeF32);
		} else if( imageType.getFamily() == ImageType.Family.PLANAR ) {
			ImageType<Planar<GrayF32>> typePF32 = ImageType.pl(imageType.getNumBands(),GrayF32.class);
			ImageGradient<Planar<GrayF32>,Planar<GrayF32>> gradientMB =
					FactoryDerivative.gradient(DerivativeType.THREE,typePF32, typePF32);
			gradient = (ImageGradient)FactoryDerivative.gradientReduce(gradientMB, DerivativeReduceType.MAX_F, GrayF32.class);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType);
		}

		return gradient;
	}

	/**
	 * Computes the value of weights inside of a block
	 */
	protected void computeCellWeights() {

		int blockWidthPixels = widthBlock*widthCell;

		Kernel2D_F64 kernel = FactoryKernelGaussian.gaussian2D_F64(0.5*blockWidthPixels,
				blockWidthPixels/2,blockWidthPixels%2==1,false);
		KernelMath.normalizeMaxOne(kernel);
		for( double d : kernel.data )
			if( d < 0 )
				throw new RuntimeException("Egads");
		weights = kernel.data;
	}

	/**
	 * Specifies input image.  Gradient is computed immediately
	 * @param input input image
	 */
	public void setInput( Input input ) {
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);
		orientation.reshape(input.width,input.height);
		magnitude.reshape(input.width,input.height);

		// pixel gradient
		gradient.process(input,derivX,derivY);

		computePixelFeatures();
	}

	/**
	 * Computes the orientation and magnitude of each pixel
	 */
	private void computePixelFeatures() {
		for (int y = 0; y < derivX.height; y++) {
			int pixelIndex = y*derivX.width;
			int endIndex = pixelIndex+derivX.width;
			for (; pixelIndex < endIndex; pixelIndex++ ) {
				float dx = derivX.data[pixelIndex];
				float dy = derivY.data[pixelIndex];

				// angle from 0 to pi radians
				orientation.data[pixelIndex] = UtilAngle.atanSafe(dy,dx) + GrlConstants.F_PId2;
				// gradient magnitude
				magnitude.data[pixelIndex] = Math.sqrt(dx*dx + dy*dy);
			}
		}
	}

	/**
	 * Computes the descriptor across the input image
	 */
	public void process() {
		locations.reset();
		descriptions.reset();

		int stepBlockPixelsX = widthCell*stepBlock;
		int stepBlockPixelsY = widthCell*stepBlock;

		int maxY = derivX.height - widthCell*widthBlock + 1;
		int maxX = derivX.width - widthCell*widthBlock + 1;

		for (int y = 0; y < maxY; y += stepBlockPixelsY ) {
			for (int x = 0; x < maxX; x += stepBlockPixelsX ) {
				TupleDesc_F64 d = descriptions.grow();
				Arrays.fill(d.value,0);
				int histogramIndex = 0;
				for (int cellRow = 0; cellRow < widthBlock; cellRow++) {
					int blockPixelRow = cellRow*widthCell;
					for (int cellCol = 0; cellCol < widthBlock; cellCol++) {
						int blockPixelCol = cellCol*widthCell;

						computeBlockHistogram(x+blockPixelCol,y+blockPixelRow,
								blockPixelCol, blockPixelRow,
								histogramIndex,d.value);
						histogramIndex += orientationBins;
					}
				}

				DescribeSiftCommon.normalizeDescriptor(d,0.2);
				locations.grow().set(x,y);
			}
		}
	}

	/**
	 * Computes the histogram for the block with the specified lower extent
	 * @param imageX0 cell's lower extent x-axis in the image
	 * @param pixelY0 cell's lower extent y-axis in the image
	 * @param blockX0 Location of the cell in the block x-axis
	 * @param blockY0 Location of the cell in the block y-axis
	 * @param histogram Storage for the histogram
	 */
	void computeBlockHistogram( int imageX0 , int pixelY0 ,
								int blockX0 , int blockY0 ,
								int indexHist , double histogram[] ) {
		// block's width in pixels
		int blockWidth = widthCell*widthBlock;

		float angleBinSize = GrlConstants.F_PI/orientationBins;

		for (int i = 0; i < widthCell; i++) {
			int indexPixel = (pixelY0+i)*derivX.stride + imageX0;
			int indexBlock = (blockY0+i)*blockWidth + blockX0;

			for (int j = 0; j < widthCell; j++ ) {
				// angle from 0 to pi radians
				float angle = this.orientation.data[indexPixel];

				// gradient magnitude
				double magnitude = this.magnitude.data[indexPixel];

				// Gaussian weights which are centered at the block's center
				double gridWeight = this.weights[ indexBlock++ ];

				// Apply spatial weighting to magnitude
				magnitude *= gridWeight;

				// Add the weighted gradient using bilinear interpolation
				float findex0 = angle/angleBinSize;
				int index0 = (int)findex0;
				double weight1 = findex0-index0;
				index0 %= orientationBins;
				int index1 = (index0+1)%orientationBins;

				histogram[indexHist+index0] += magnitude*(1.0-weight1);
				histogram[indexHist+index1] += magnitude*weight1;
			}
		}
	}

	/**
	 * List of locations for each descriptor.
	 */
	public FastQueue<Point2D_I32> getLocations() {
		return locations;
	}

	/**
	 * List of descriptors
	 */
	public FastQueue<TupleDesc_F64> getDescriptions() {
		return descriptions;
	}

	public GrayF32 _getDerivX() {
		return derivX;
	}

	public GrayF32 _getDerivY() {
		return derivY;
	}

	/**
	 * Returns the number of pixel's wide the square region is that a descriptor was computed from
	 * @return number of pixels wide
	 */
	public int getRegionWidthPixel() {
		return widthCell*widthBlock;
	}

	public void setWidthCell(int widthCell) {
		this.widthCell = widthCell;
	}

	public int getWidthCell() {
		return widthCell;
	}

	public int getWidthBlock() {
		return widthBlock;
	}

	public int getStepBlock() {
		return stepBlock;
	}

	public int getOrientationBins() {
		return orientationBins;
	}

	public void setStepBlock(int stepBlock) {
		this.stepBlock = stepBlock;
	}

	public ImageType<Input> getImageType() {
		return imageType;
	}

	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(orientationBins*widthBlock*widthBlock);
	}
}
