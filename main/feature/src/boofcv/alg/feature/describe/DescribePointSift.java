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

package boofcv.alg.feature.describe;

import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;

/**
 * <p>
 * Detects SIFT features inside the provided SIFT scale-space.  SIFT features work by sampling the image in a grid.
 * For each grid element, a histogram of the gradient's orientation is computed.  From this histogram the descriptor
 * is computed.  See [1] for the details and below for algorithmic changes.
 * </p>
 *
 * <p>
 * DESCRIPTOR INTERPOLATION: Instead of using trilinear interpolation a Gaussian weight is used instead.
 * Both methods were tried and Gaussian weight produced slightly better results.  Same results had been
 * found by others with regard to SURF descriptors.
 * </p>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints".
 * International Journal of Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointSift {

	// Image scale space
	private SiftImageScaleSpace ss;

	// Converts a distribution's sigma into a region radius to sample
	private double sigmaToRadius;

	// number of elements along the side of the descriptor grid
	private int gridWidth;
	// number of samples along a grid element's side
	// each grid is samples the square of this number and the histogram computed
	// from these samples
	private int numSamples;

	private int numHistBins;
	private double angleStep;

	// image and gradient of octave being processed
	private ImageFloat32 image;
	private ImageFloat32 derivX;
	private ImageFloat32 derivY;

	// storage for histogram
	private double[][] histograms;
	private double[] gridWeights;

	/**
	 * Configures detector
	 *
	 * @param gridWidth Number of grid elements along a side.  Typically 4
	 * @param numSamples Number of samples along a grid. Typically 8
	 * @param numHistBins Number of bins in the orientation histogram.  Typically 8
	 * @param weightSigma Adjusts descriptor element's weighting from center.  Typically 0.5
	 * @param sigmaToRadius Conversation from scale space to pixels.  Typically 2.5
	 */
	public DescribePointSift( int gridWidth , int numSamples , int numHistBins ,
							  double weightSigma , double sigmaToRadius ) {
		this.gridWidth = gridWidth;
		this.numSamples = numSamples;
		this.numHistBins = numHistBins;
		this.sigmaToRadius = sigmaToRadius;

		angleStep = 2.0*Math.PI/numHistBins;

		histograms = new double[gridWidth*gridWidth][numHistBins];

		int gridSampleLength = numSamples*gridWidth;
		gridWeights = new double[gridSampleLength*gridSampleLength];

		// make the weighting relative to descriptor length
		weightSigma *= gridSampleLength;

		int index = 0;
		for( int y = -gridSampleLength/2; y < gridSampleLength/2; y++ ) {
			for( int x = -gridSampleLength/2; x < gridSampleLength/2; x++ ) {
				// make it symmetric by sampling in the center
				double d = Math.sqrt((x+0.5)*(x+0.5) + (y+0.5)*(y+0.5));
				gridWeights[index++] = Math.exp( -0.5*d*d/(weightSigma*weightSigma));
			}
		}
	}


	public void setScaleSpace( SiftImageScaleSpace ss ) {
		this.ss = ss;
	}

	public void process(double c_x , double c_y , double scale , double orientation ,
						SurfFeature desc ) {
		// determine where this feature lies inside the scale-space
		int imageIndex = ss.scaleToImageIndex( scale );
		double pixelScale = ss.imageIndexToPixelScale( imageIndex );

		process(c_x,c_y,scale,orientation,imageIndex,pixelScale,desc);
	}

	/**
	 * Compute the descriptor with information on which level in the scale-space to use.
	 *
	 * @param c_x Location of feature in input image
	 * @param c_y Location of feature in input image
	 * @param scale Size of the feature in the input image
	 * @param orientation Orientation of the feature
	 * @param imageIndex Which octave contains the feature
	 * @param pixelScale The scale of a pixel in the octave
	 * @param desc (Output) storage for the descriptor
	 */
	public void process( double c_x , double c_y , double scale , double orientation ,
						 int imageIndex ,
						 double pixelScale ,
						 SurfFeature desc )
	{
		image = ss.getPyramidLayer(imageIndex);
		derivX = ss.getDerivativeX(imageIndex);
		derivY = ss.getDerivativeY(imageIndex);

		for( int i = 0; i < desc.value.length; i++ )
			desc.value[i] = 0;
		for( int i = 0; i < histograms.length; i++ )
			for( int j = 0; j < histograms[i].length; j++ )
				histograms[i][j] = 0;

		// account for the scale of the image in each octave
		constructHistograms(c_x/pixelScale , c_y/pixelScale, scale/pixelScale, orientation );

		computeDescriptor(desc);
	}

	private void constructHistograms( double c_x , double c_y , double scale, double orientation ) {
		double c = Math.cos(orientation);
		double s = Math.sin(orientation);

		int gridRadius = gridWidth/2;

		// This is the distance between samples
		// The size is computed by finding the width of one block in the grid, then dividing by the
		// number of samples along that side
		double sampleUnit = (2.0*scale*sigmaToRadius)/numSamples;
		// how wide a grid cell is in pixels
		double gridCellLength = numSamples*sampleUnit;
		int gridCellLengthI = (int)(gridCellLength+0.5); // round to int

//		System.out.println("-----------------------------------------");
//		System.out.println("  cell length "+gridCellLength);
//		System.out.println("  sampleUnit "+sampleUnit);

		int allSampleIndex = 0;
		for( int gy = 0; gy < gridWidth; gy++ ) {
			double gridY = (gy-gridRadius)*gridCellLength;
			for( int gx = 0; gx < gridWidth; gx++ ) {
				// top left coordinate of grid in pixels
				double gridX = (gx-gridRadius)*gridCellLength;

				// TODO Sample all pixels here
				for( int sy = 0; sy < numSamples; sy++ ) {
					double y = sy*sampleUnit + gridY;
					for( int sx = 0; sx < numSamples; sx++ , allSampleIndex++ ) {
						// Sample point in pixels in grid coordinate system
						double x = sx*sampleUnit + gridX;

						// Rotate and translate into image pixel coordinates, then round
						int px = (int)(x*c - y*s + c_x + 0.5);
						int py = (int)(x*s + y*c + c_y + 0.5);

						if( image.isInBounds(px,py) ) {
							double dx = derivX.unsafe_get(px,py);
							double dy = derivY.unsafe_get(px,py);

							// Gaussian weighting applied to whole sample area
							double w = gridWeights[allSampleIndex];

							// rotate derivative into grid coordinate system
							double adjX = ( dx*c + dy*s)*w;
							double adjY = (-dx*s + dy*c)*w;

							addToHistograms( gx-gridRadius , gy-gridRadius , x/gridCellLength , y/gridCellLength, adjX, adjY );
						}
					}
				}
			}
		}
	}

	private void addToHistograms(int gridX, int gridY, double locX, double locY, double gradX, double gradY) {

		// compute the angle and magnitude of the gradient
		int angleBin = (int)((Math.atan2(gradY,gradX)+Math.PI)/angleStep);
		if( angleBin >= numHistBins )
			angleBin = numHistBins-1;

		double gradMag = Math.sqrt(gradX*gradX + gradY*gradY);

		int gridRadius = gridWidth/2;

		int startY = gridY > -gridRadius ? -1 : 0;
		int endY = gridY < gridRadius-1 ? 1 : 0;
		int startX = gridX > -gridRadius ? -1 : 0;
		int endX = gridX < gridRadius-1 ? 1 : 0;

		// distribute into neighboring bins
		for( int offY = startY; offY <= endY; offY++ ) {
			for( int offX = startX; offX <= endX; offX++ ) {
				int binIndex = (gridY+gridRadius+offY)*gridWidth + gridX+gridRadius+offX;

				// Use an alternative weighting scheme.  You can view this as a very crude approximation
				// of a Gaussian.  exp() function is very expensive

				if( offX == 0 && offY == 0 ) {
					histograms[binIndex][angleBin] += gradMag;
				} else {
					// compute distance from center of grid element
					double distX = Math.abs(locX-(gridX+offX+0.5));
					double distY = Math.abs(locY-(gridY+offY+0.5));

					if( distX < 1 && distY < 1 ) {
						double w = (1-distX)*(1-distY);
						histograms[binIndex][angleBin] += w * gradMag;
					}
				}
			}
		}
	}

	private void computeDescriptor( SurfFeature desc ) {
		int index = 0;
		int indexGrid = 0;
		double sumSq = 0;

		for( int gy = 0; gy < gridWidth; gy++ ) {
			for( int gx = 0; gx < gridWidth; gx++ , indexGrid++ ) {
				for( int hist = 0; hist < numHistBins; hist++ ) {
					double v = desc.value[index++] = histograms[indexGrid][hist];
					sumSq += v*v;
				}
			}
		}

//		System.out.println("descriptor");
		double norm = Math.sqrt(sumSq);
		for( int i = 0; i < desc.size() ; i++ )  {
			desc.value[i] /= norm;
		}

		// cap values at 0.2 and re-normalize
		sumSq = 0;
		for( int i = 0; i < desc.size(); i++ ) {
			double v = desc.value[i];
			if( v > 0.2 )
				v = desc.value[i] = 0.2;
			sumSq += v*v;
		}
		norm = Math.sqrt(sumSq);
		for( int i = 0; i < desc.size() ; i++ )  {
			desc.value[i] /= norm;
		}
	}

	public int getDescriptorLength() {
		return gridWidth*gridWidth*numHistBins;
	}
}
