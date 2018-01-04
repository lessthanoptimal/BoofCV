/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGThresholdImageOps {

	Random rand = new Random(234);

	/**
	 * Compare otsu against a brute force algorithm for computing variance directly.
	 */
	@Test
	public void computeOtsu() {

		for (int i = 0; i < 100; i++) {
			int histogram[] = new int[ 256 ];
			int total = 0;
			for (int j = 0; j < histogram.length; j++) {
				total += histogram[j] = rand.nextInt(400);
			}

			int best = bruteForceOtsu(histogram, total);
			int found = GThresholdImageOps.computeOtsu(histogram,histogram.length,total);

			assertEquals(best,found);
		}
	}

	private int bruteForceOtsu(int[] histogram, int total) {
		int best = -1;
		double bestScore = Double.MAX_VALUE;

		for (int j = 0; j < histogram.length-1; j++) {
			// the threshold is inclusive. <= upper value
			double stats0[] = variance(histogram,0,j,total);
			double stats1[] = variance(histogram,j+1,histogram.length-1, total);

			if( stats0 == null || stats1 == null )
				continue;

			double var0 = stats0[1];
			double weight0 = stats0[2];

			double var1 = stats1[1];
			double weight1 = stats1[2];

			double score = (weight0*var0 + weight1*var1);

			if( score < bestScore ) {
				bestScore = score;
				best = j;
			}
		}
		return best;
	}

	@Test
	public void computeOtsu2() {

		for (int i = 0; i < 100; i++) {
			int histogram[] = new int[ 256 ];
			int total = 0;
			for (int j = 0; j < histogram.length; j++) {
				total += histogram[j] = rand.nextInt(400);
			}

			int best = bruteForceOtsu2(histogram, total);
			int found = GThresholdImageOps.computeOtsu2(histogram,histogram.length,total);

			assertEquals(best,found);
		}
	}

	private int bruteForceOtsu2(int[] histogram, int total) {
		double bestScore = 0;
		double bestMean0=0,bestMean1=0;

		double stats[] = variance(histogram,0,histogram.length-1,total);
		double varianceAll = stats[1];
		for (int j = 0; j < histogram.length-1; j++) {
			// the threshold is inclusive. <= upper value
			double stats0[] = variance(histogram,0,j,total);
			double stats1[] = variance(histogram,j+1,histogram.length-1, total);

			if( stats0 == null || stats1 == null )
				continue;

			double mean0 = stats0[0];
			double var0 = stats0[1];
			double weight0 = stats0[2];

			double mean1 = stats1[0];
			double var1 = stats1[1];
			double weight1 = stats1[2];


			double withinClassVariance = (weight0*var0 + weight1*var1);
			double betweenClassVariance = varianceAll - withinClassVariance;

			if( betweenClassVariance > bestScore ) {
				bestScore = betweenClassVariance;
				bestMean0 = mean0;
				bestMean1 = mean1;
			}
		}
		return (int)((bestMean0 + bestMean1)/2.0 + 0.5);
	}


	private static double[] variance( int histogram[] , int start , int stop , int allPixels) {
		double mean = 0;
		int total = 0;
		for (int i = start; i <= stop; i++) {
			mean += i*histogram[i];
			total += histogram[i];
		}
		if( total == 0 )
			return null;

		mean /= total;

		double variance = 0;
		for (int i = start; i <= stop; i++) {
			variance += histogram[i]*(i - mean)*(i - mean);
		}
		variance /= total-1;

		return new double[]{ mean , variance, total/(double)allPixels };
	}

	/**
	 * Check to see if it handles zeros and the start and end of the histogram correctly.
	 */
	@Test
	public void computeOtsu_zeros() {
		int histogram[] = new int[ 256 ];

		int total = 0;
		for (int j = 15; j < histogram.length-40; j++) {
			total += histogram[j] = rand.nextInt(400);
		}

		int best = bruteForceOtsu(histogram, total);
		int found = GThresholdImageOps.computeOtsu(histogram,histogram.length,total);

		assertEquals(best,found);
	}

	@Test
	public void computeOtsu2_zeros() {
		int histogram[] = new int[ 256 ];

		int total = 0;
		for (int j = 15; j < histogram.length-40; j++) {
			total += histogram[j] = rand.nextInt(400);
		}

		int best = bruteForceOtsu2(histogram, total);
		int found = GThresholdImageOps.computeOtsu2(histogram,histogram.length,total);

		assertEquals(best,found);
	}

	/**
	 * The histogram is composed of two values. See if it picks a threshold that can split this sest
	 */
	@Test
	public void computeOtsu2_pathological() {
		int histogram[] = new int[ 256 ];

		histogram[10] = 200;
		histogram[120] = 500;

		int found = GThresholdImageOps.computeOtsu2(histogram,histogram.length,700);

		assertEquals(65,found); // 65 maximizes the distance between the two
	}

	@Test
	public void computeEntropy() {
		for (int i = 0; i < 100; i++) {
			int histogram[] = new int[ 256 ];
			int total = 0;
			for (int j = 0; j < histogram.length; j++) {
				total += histogram[j] = rand.nextInt(400);
			}

			int best = directComputeEntropy(histogram, histogram.length, total);
			int found = GThresholdImageOps.computeEntropy(histogram, histogram.length, total);

			assertEquals(best,found);
		}
	}

	@Test
	public void computeEntropy_zeros() {
		int histogram[] = new int[ 256 ];

		int total = 0;
		for (int j = 15; j < histogram.length-40; j++) {
			total += histogram[j] = rand.nextInt(400);
		}

		int best = directComputeEntropy(histogram, histogram.length, total);
		int found = GThresholdImageOps.computeEntropy(histogram, histogram.length, total);

		assertEquals(best,found);
	}

	/**
	 * Implementation of computeEntropy() which is almost identical to the original equations
	 */
	public static int directComputeEntropy( int histogram[] , int length , int totalPixels ) {

		double p[] = new double[length];
		for (int i = 0; i < length; i++) {
			p[i] = histogram[i]/(double)totalPixels;
		}

		double bestScore = 0;
		int bestIndex = 0;

		for (int i=0 ; i<length ; i++) {

			double sumF = 0;
			for (int j = 0; j <= i; j++) {
				sumF += p[j];
			}

			if( sumF == 0 || sumF == 1.0 ) continue;

			double sumB = 1.0-sumF;

			double HA = 0;
			for (int j = 0; j <= i; j++) {
				if( p[j] == 0 ) continue;
				HA += p[j]*Math.log(p[j]);
			}
			HA/=sumF;

			double HB = 0;
			for (int j = i+1; j < length; j++) {
				if( p[j] == 0 ) continue;
				HB += p[j]*Math.log(p[j]);
			}
			HB/=sumB;

			double entropy = Math.log(sumF) + Math.log(sumB)  - HA - HB;

			if( entropy > bestScore ) {
				bestScore = entropy;
				bestIndex = i;
			}
		}

		return bestIndex;
	}
}