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

package boofcv.alg.segmentation.slic;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.ms.ClusterLabeledImage;
import boofcv.alg.segmentation.ms.MergeSmallRegions;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.struct.ConnectRule;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Arrays;

/**
 * <p>
 * K-means based superpixel image segmentation, see [1].  The image is broken up into superpixels (clusters of
 * connected pixels) in a grid like pattern.  A connectivity rule of 4 or 8 is enforced across all the clusters.
 * Clustering is done using k-means, where each point is composed on the 2D image coordinate and an intensity
 * value in each color band, thus K = 2+numBands.  Instead of computing the distance of each cluster's center
 * from each point only points within a distance of S si considered.  The difference in scale difference between
 * pixels and image intensity is handled through a user configurable tuning parameter.
 * </p>
 *
 * <p>
 * Deviations from paper:
 * <ul>
 * <li>In the paper a LAB color space is always used.  In this implementation a general purpose N-dimensional
 * color space is used.</li>
 * <li>To correctly support LAB or other color spaces a specialized implementation might be needed to ensure
 * the intensity of a pixel is computed correctly.</li>
 * <li>Small regions are merged into other regions based on how similar their color is.  In the paper
 * a small region is merged into the largest region it is connected to.</li>
 * </ul>
 * </p>
 *
 * <p>
 * [1] Radhakrishna Achanta, Appu Shaji, Kevin Smith, Aurelien Lucchi, Pascal Fua, and Sabine Süsstrunk,
 * SLIC Superpixels, EPFL Technical Report no. 149300, June 2010.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SegmentSlic<T extends ImageBase> {
	// border which ensures there is a 3x3 neighborhood around the initial clusters and that there are pixels
	// which can be sampled when computing the gradient
	public static final int BORDER = 2;

	// number of bands in the input image
	private int numBands;

	// the number of regions/superpixels.  K in the paper
	private int numberOfRegions;

	// spacial weighting tuning parameter  Is also m in the paper.
	private float m;

	// Number of iterations
	private int totalIterations;

	// Space between superpixel centers.  S in the paper
	protected int gridInterval;
	// Adjustment to spacial distance.  Computed from m and gridInterval
	private float adjustSpacial;

	// The image being processed
	protected T input;

	// Initial segmentation before connectivity is enforced
	private GrayS32 initialSegments = new GrayS32(1,1);
	// number of members in each region
	private GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
	// color of each region
	private FastQueue<float[]> regionColor;
	// merges smaller regions into larger ones
	private MergeSmallRegions<T> mergeSmall;

	// ensures that all pixels in segment are connected
	protected ClusterLabeledImage segment;

	// storage for clusters and pixel information
	protected FastQueue<Cluster> clusters;
	protected FastQueue<Pixel> pixels = new FastQueue<>(Pixel.class, true);

	// type of input image
	protected ImageType<T> imageType;

	// connectivity rule
	protected ConnectRule connectRule;

	public SegmentSlic( int numberOfRegions , float m , int totalIterations ,
						ConnectRule connectRule , ImageType<T> imageType ) {
		this.numberOfRegions = numberOfRegions;
		this.m = m;
		this.totalIterations = totalIterations;
		this.numBands = imageType.getNumBands();
		this.connectRule = connectRule;
		this.imageType = imageType;

		ComputeRegionMeanColor<T> regionColor = FactorySegmentationAlg.regionMeanColor(imageType);
		this.mergeSmall = new MergeSmallRegions<>(-1, connectRule, regionColor);
		this.segment = new ClusterLabeledImage(connectRule);
		this.regionColor = new ColorQueue_F32(numBands);

		// custom declaration for pixel color
		clusters = new FastQueue<Cluster>(Cluster.class,true) {
			@Override
			protected Cluster createInstance() {
				Cluster c = new Cluster();
				c.color = new float[ SegmentSlic.this.numBands ];
				return c;
			}
		};
	}

	public void process( T input , GrayS32 output ) {
		InputSanityCheck.checkSameShape(input,output);
		if( input.width < 2*BORDER || input.height < 2*BORDER)
			throw new IllegalArgumentException(
					"Image is too small to process.  Must have a width and height of at least "+(2*BORDER));

		// initialize all the data structures
		initalize(input);

		// Seed the clusters
		initializeClusters();

		// Perform the modified k-means iterations
		for( int i = 0; i < totalIterations; i++ ) {
			computeClusterDistance();
			updateClusters();
		}

		// Assign labels to each pixel based on how close it is to a cluster
		computeClusterDistance();
		assignLabelsToPixels(initialSegments,regionMemberCount,regionColor);

		// Assign disconnected pixels to the largest cluster they touch
		int N = input.width*input.height/numberOfRegions;
		segment.process(initialSegments,output,regionMemberCount);
		mergeSmall.setMinimumSize(N / 2);
		mergeSmall.process(input,output,regionMemberCount,regionColor);
	}

	/**
	 * prepares all data structures
	 */
	protected void initalize(T input) {
		this.input = input;
		pixels.resize(input.width * input.height);
		initialSegments.reshape(input.width, input.height);

		// number of usable pixels that cluster centers can be placed in
		int numberOfUsable = (input.width-2*BORDER)*(input.height-2*BORDER);
		gridInterval = (int)Math.sqrt( numberOfUsable/(double)numberOfRegions);

		if( gridInterval <= 0 )
			throw new IllegalArgumentException("Too many regions for an image of this size");

		// See equation (1)
		adjustSpacial = m/gridInterval;
	}

	/**
	 * initialize all the clusters at regularly spaced intervals.  Their locations are perturbed a bit to reduce
	 * the likelihood of a bad location.  Initial color is set to the image color at the location
	 */
	protected void initializeClusters() {

		int offsetX = Math.max(BORDER,((input.width-1) % gridInterval)/2);
		int offsetY = Math.max(BORDER,((input.height-1) % gridInterval)/2);

		int clusterId = 0;
		clusters.reset();
		for( int y = offsetY; y < input.height-BORDER; y += gridInterval ) {
			for( int x = offsetX; x < input.width-BORDER; x += gridInterval ) {
				Cluster c = clusters.grow();
				c.id = clusterId++;
				if( c.color == null)
					c.color = new float[numBands];

				// sets the location and color at the local minimal gradient point
				perturbCenter( c , x , y );
			}
		}
	}

	/**
	 * Set the cluster's center to be the pixel in a 3x3 neighborhood with the smallest gradient
	 */
	protected void perturbCenter( Cluster c , int x , int y ) {
		float best = Float.MAX_VALUE;
		int bestX=0,bestY=0;

		for( int dy = -1; dy <= 1; dy++ ) {
			for( int dx = -1; dx <= 1; dx++ ) {
				float d = gradient(x + dx, y + dy);
				if( d < best ) {
					best = d;
					bestX = dx;
					bestY = dy;
				}
			}
		}

		c.x = x+bestX;
		c.y = y+bestY;
		setColor(c.color,x+bestX,y+bestY);
	}

	/**
	 * Computes the gradient at the specified pixel
	 */
	protected float gradient(int x, int y) {
		float dx = getIntensity(x+1,y) - getIntensity(x-1,y);
		float dy = getIntensity(x,y+1) - getIntensity(x,y-1);

		return dx*dx + dy*dy;
	}

	/**
	 * Sets the cluster's to the pixel color at that location
	 */
	public abstract void setColor( float[] color , int x , int y );

	/**
	 * Performs a weighted add to the cluster's color at the specified pixel in the image
	 */
	public abstract void addColor( float[] color , int index , float weight );

	/**
	 * Euclidean Squared distance away that the pixel is from the provided color
	 */
	public abstract float colorDistance( float[] color , int index );

	/**
	 * Intensity of the pixel at the specified location
	 */
	public abstract float getIntensity(int x, int y);

	/**
	 * Computes how far away each cluster is from each pixel.  Expectation step.
	 */
	protected void computeClusterDistance() {
		for( int i = 0; i < pixels.size; i++ ) {
			pixels.data[i].reset();
		}

		for( int i = 0; i < clusters.size; i++ ) {
			Cluster c = clusters.data[i];

			// compute search bounds
			int centerX = (int)(c.x + 0.5f);
			int centerY = (int)(c.y + 0.5f);

			int x0 = centerX - gridInterval; int x1 = centerX + gridInterval + 1;
			int y0 = centerY - gridInterval; int y1 = centerY + gridInterval + 1;

			if( x0 < 0 ) x0 = 0;
			if( y0 < 0 ) y0 = 0;
			if( x1 > input.width ) x1 = input.width;
			if( y1 > input.height ) y1 = input.height;

			for( int y = y0; y < y1; y++ ) {
				int indexPixel = y*input.width + x0;
				int indexInput = input.startIndex + y*input.stride + x0;

				int dy = y-centerY;

				for( int x = x0; x < x1; x++ ) {
					int dx = x-centerX;

					float distanceColor = colorDistance(c.color,indexInput++);
					float distanceSpacial = dx*dx + dy*dy;
					pixels.data[indexPixel++].add(c,distanceColor + adjustSpacial*distanceSpacial);
				}
			}
		}
	}

	/**
	 * Update the value of each cluster using  Maximization step.
	 */
	protected void updateClusters() {
		for( int i = 0; i < clusters.size; i++ ) {
			clusters.data[i].reset();
		}

		int indexPixel = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexInput = input.startIndex + y*input.stride;
			for( int x =0; x < input.width; x++ , indexPixel++ , indexInput++) {
				Pixel p = pixels.get(indexPixel);

				// convert the distance each cluster is from the pixel into weights
				p.computeWeights();

				for( int i = 0; i < p.clusters.size; i++ ) {
					ClusterDistance d = p.clusters.data[i];
					d.cluster.x += x*d.distance;
					d.cluster.y += y*d.distance;
					d.cluster.totalWeight += d.distance;
					addColor(d.cluster.color,indexInput,d.distance);
				}
			}
		}

		// recompute the center of each cluster
		for( int i = 0; i < clusters.size; i++ ) {
			clusters.data[i].update();
		}
	}

	/**
	 * Selects which region each pixel belongs to based on which cluster it is the closest to
	 */
	public void assignLabelsToPixels( GrayS32 pixelToRegions ,
									  GrowQueue_I32 regionMemberCount ,
									  FastQueue<float[]> regionColor ) {

		regionColor.reset();
		for( int i = 0; i < clusters.size(); i++ ) {
			float[] r = regionColor.grow();
			float[] c = clusters.get(i).color;
			for( int j = 0; j < numBands; j++ ) {
				r[j] = c[j];
			}
		}

		regionMemberCount.resize(clusters.size());
		regionMemberCount.fill(0);

		int indexPixel = 0;
		for( int y = 0; y < pixelToRegions.height; y++ ) {
			int indexOutput = pixelToRegions.startIndex + y*pixelToRegions.stride;
			for( int x =0; x < pixelToRegions.width; x++ , indexPixel++ , indexOutput++) {
				Pixel p = pixels.data[indexPixel];

				// It is possible for a pixel to be unassigned if all the means move too far away from it
				// Default to a non-existant cluster if that's the case
				int best = -1;
				float bestDistance = Float.MAX_VALUE;
				// find the region/cluster which it is closest to
				for( int j = 0; j < p.clusters.size; j++ ) {
					ClusterDistance d = p.clusters.data[j];
					if( d.distance < bestDistance ) {
						bestDistance = d.distance;
						best = d.cluster.id;
					}
				}
				if( best == -1 ) {
					regionColor.grow();
					best = regionMemberCount.size();
					regionMemberCount.add(0);
				}

				pixelToRegions.data[indexOutput] = best;
				regionMemberCount.data[best]++;
			}
		}
	}

	public GrowQueue_I32 getRegionMemberCount() {
		return regionMemberCount;
	}

	public FastQueue<Cluster> getClusters() {
		return clusters;
	}

	/**
	 * K-means clustering information for each pixel.  Stores distance from each cluster mean.
	 */
	public static class Pixel
	{
		// list of clusters it is interacting with
		public FastQueue<ClusterDistance> clusters = new FastQueue<>(12, ClusterDistance.class, true);

		public void add( Cluster c , float distance ) {
			// make sure it isn't already full.  THis should almost never happen
			ClusterDistance d = clusters.grow();

			d.cluster = c;
			d.distance = distance;
		}

		public void computeWeights() {
			if( clusters.size == 1 ) {
				clusters.data[0].distance = 1;
			} else {
				float sum = 0;
				for( int i = 0; i < clusters.size; i++ ) {
					sum += clusters.data[i].distance;
				}
				for( int i = 0; i < clusters.size; i++ ) {
					clusters.data[i].distance =  1.0f - clusters.data[i].distance/sum;
				}
			}
		}

		public void reset() {
			clusters.reset();
		}
	}

	/**
	 * Stores how far a cluster is from the specified pixel
	 */
	public static class ClusterDistance
	{
		// the cluster
		public Cluster cluster;
		// how far the pixel is from the cluster
		public float distance;
	}

	/**
	 * The mean in k-means.  Point in image (x,y) and color space.
	 */
	public static class Cluster
	{
		// unique ID for the cluster
		public int id;

		// location of the cluster in the image and color space
		public float x;
		public float y;
		public float color[];

		// the total.  Used when being updated
		public float totalWeight;

		public void reset() {
			x = y = 0;
			Arrays.fill(color,0);
			totalWeight = 0;
		}

		public void update() {
			x /= totalWeight;
			y /= totalWeight;
			for( int i = 0; i < color.length; i++ ) {
				color[i] /= totalWeight;
			}
		}
	}

	public ImageType<T> getImageType() {
		return imageType;
	}

	public ConnectRule getConnectRule() {
		return connectRule;
	}
}
