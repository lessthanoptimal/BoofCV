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

package boofcv.alg.geo.d2;

import boofcv.alg.InputSanityCheck;
import boofcv.core.image.FactorySingleBandImage;
import boofcv.core.image.SingleBandImage;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.ImageBase;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 * Computes several metrics to evaluate how well image stabilization has been performed using an arbitrary
 * video.
 * </p>
 *
 * <p>
 * Metrics:
 * <ul>
 * <li>Fractional area overlap</li>
 * <li>Pixel Error</li>
 * <li>Key Frame Period Length</li>
 * <li>Number of key frames</li>
 * </ul>
 * </p>
 *
 * <p>
 * Fractional area overlap records how much of the stabilized image overlaps with the keyframe.  Pixel error
 * is how different the stabilized image is from the keyframe, only including pixels that are overlapped.
 * Period length is a record of how long it was until key frames needed to be changed.
 * </p>
 *
 * @author Peter Abeles
 */
public class EvaluateImageStabilization<I extends ImageBase> {

	List<Double> averagePixelError = new ArrayList<Double>();
	List<Double> fractionOverlap = new ArrayList<Double>();
	List<Integer> lengthOfPeriod = new ArrayList<Integer>();
	long numKeyFrames;

	I keyFrame;
	long startKeyFrame = 0;
	long frameNum;

	PrintStream out = System.out;

	public void printMetrics() {
		Collections.sort(averagePixelError);
		Collections.sort(fractionOverlap);
		Collections.sort(lengthOfPeriod);

		out.println("Average Pixel Error 50%: " + averagePixelError.get( averagePixelError.size()/2));
		out.println("Overlap Fraction 50%:    " + fractionOverlap.get( fractionOverlap.size()/2));
		out.println("Length Of Period 50%:    " + lengthOfPeriod.get( lengthOfPeriod.size()/2));
		out.println("Number Of Key Frames:    " + numKeyFrames);
		out.println("Total Frames:            " + frameNum);
	}

	public void update( I currentFrame , PixelTransform motion , boolean isKeyFrame )
	{
		final int width = currentFrame.getWidth();
		final int height = currentFrame.getHeight();

		if( keyFrame == null ) {
			keyFrame = (I)currentFrame._createNew(width,height);
		} else {
			InputSanityCheck.checkSameShape(keyFrame,currentFrame);
		}

		if( isKeyFrame ) {
			// save the keyframe
			keyFrame.setTo(currentFrame);

			// update keyframe change metrics
			int period = (int)(frameNum-startKeyFrame);
			if( period > 0 ) {
				lengthOfPeriod.add(period);
			}
			startKeyFrame = numKeyFrames;
			numKeyFrames++;
		} else {
			int numOverlap = 0;
			double error = 0;

			SingleBandImage a = FactorySingleBandImage.wrap(currentFrame);
			SingleBandImage b = FactorySingleBandImage.wrap(keyFrame);

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					motion.compute(x,y);
					int xx = (int)motion.distX;
					int yy = (int)motion.distY;
					if( keyFrame.isInBounds(xx,yy) ) {
						numOverlap++;
						error += Math.abs(a.get(xx,yy).doubleValue() - b.get(x,y).doubleValue());
					}
				}
			}

			if( numOverlap > 0 ) {
				error /= numOverlap;
				averagePixelError.add(error);
			}
			fractionOverlap.add( (double)numOverlap/(width*height));
		}

		frameNum++;
	}

}
