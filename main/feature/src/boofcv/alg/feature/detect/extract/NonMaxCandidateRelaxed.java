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

package boofcv.alg.feature.detect.extract;


import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;

/**
 * <p/>
 * Only examine around candidate regions for corners using a relaxed rule
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxCandidateRelaxed extends NonMaxCandidateStrict {

	public NonMaxCandidateRelaxed(int searchRadius, float thresh, int border )
	{
	    super(searchRadius,thresh,border);
	}

	public NonMaxCandidateRelaxed() {
	}

	@Override
	public void process(ImageFloat32 intensityImage, QueueCorner candidates, QueueCorner corners) {

		this.input = intensityImage;

		// pixels indexes larger than these should not be examined
		int endX = intensityImage.width-ignoreBorder;
		int endY = intensityImage.height-ignoreBorder;

		final int stride = intensityImage.stride;

		final float inten[] = intensityImage.data;

		for (int iter = 0; iter < candidates.size; iter++) {
			Point2D_I16 pt = candidates.data[iter];

			if( pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= endX || pt.y >= endY )
				continue;

			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val < thresh || val == Float.MAX_VALUE ) continue;

			int x0 = Math.max(ignoreBorder,pt.x - radius);
			int y0 = Math.max(ignoreBorder,pt.y - radius);
			int x1 = Math.min(endX, pt.x + radius + 1);
			int y1 = Math.min(endY, pt.y + radius + 1);

			boolean success = true;
			failed:
			for( int i = y0; i < y1; i++ ) {
				int index = input.startIndex + i * input.stride + x0;
				for( int j = x0; j < x1; j++ , index++ ) {
					// don't compare the center point against itself
					if ( center == index )
						continue;

					if (val < input.data[index]) {
						success = false;
						break failed;
					}
				}
			}
			if( success )
				corners.add(pt.x,pt.y);
		}
	}

}
