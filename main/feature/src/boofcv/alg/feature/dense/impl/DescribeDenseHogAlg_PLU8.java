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

package boofcv.alg.feature.dense.impl;

import boofcv.alg.feature.dense.DescribeDenseHogAlg;
import boofcv.struct.image.*;

import javax.annotation.Generated;

/**
 * Implementation pf {@link DescribeDenseHogAlg} for {@link GrayF32}.
 *
 * @author Peter Abeles
 */
@Generated("")
public class DescribeDenseHogAlg_PLU8 extends DescribeDenseHogAlg
		<Planar<GrayU8>,Planar<GrayS16>>
{
	int numBands;

	public DescribeDenseHogAlg_PLU8(int orientationBins, int widthCell, int widthBlock, int stepBlock, int numBands ) {
		super(orientationBins, widthCell, widthBlock,stepBlock, ImageType.pl(numBands,GrayU8.class));
		this.numBands = numBands;
	}

	@Override
	public void computeDerivative(int pixelIndex) {

		float maxDX=0,maxDY=0;
		float maxNorm = 0;

		for (int i = 0; i < numBands; i++) {
			float dx = derivX.bands[i].data[pixelIndex];
			float dy = derivY.bands[i].data[pixelIndex];

			float n = dx*dx + dy*dy;
			if( n > maxNorm ) {
				maxNorm = n;
				maxDX = dx;
				maxDY = dy;
			}
		}

		pixelDX = maxDX;
		pixelDY = maxDY;
	}
}
