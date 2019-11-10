/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.block;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;

/**
 * Brute force block matching stereo
 *
 * @author Peter Abeles
 */
public abstract class DisparityBlockMatchNaive<T extends ImageBase<T>> {

	protected int radius;
	protected int width;
	protected int minDisparity,maxDisparity;

	protected double[] scores;

	public boolean minimize = true;

	public DisparityBlockMatchNaive(int radius, int minDisparity, int maxDisparity) {
		this.radius = radius;
		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;

		this.width = radius*2+1;
		this.scores = new double[maxDisparity-minDisparity];
	}

	public void process(T left , T right , GrayU8 disparity ) {
		ImageMiscOps.fill(disparity,maxDisparity-minDisparity+1);
		for (int y = radius; y < left.height - radius; y++) {
			for (int x = radius+minDisparity; x < left.width - radius; x++) {
				for (int d = minDisparity; d < maxDisparity; d++) {
					if( x-radius-d >= 0 )
						scores[d-minDisparity] = computeScore(left,right,x,y,d);
					else {
						scores[d-minDisparity] = minimize ? Double.MAX_VALUE : -Double.MAX_VALUE;
					}
				}

				int bestDisparity = 0;
				double bestScore = scores[0];

				for (int d = 1; d < maxDisparity - minDisparity; d++) {
					double s = scores[d];
					if( minimize ) {
						if( s < bestScore ) {
							bestScore = s;
							bestDisparity = d;
						}
					} else {
						if( s > bestScore ) {
							bestScore = s;
							bestDisparity = d;
						}
					}
				}

				disparity.set(x,y,bestDisparity);
			}
		}
	}

	public abstract double computeScore( T left , T right , int cx , int cy , int disparity );
}
