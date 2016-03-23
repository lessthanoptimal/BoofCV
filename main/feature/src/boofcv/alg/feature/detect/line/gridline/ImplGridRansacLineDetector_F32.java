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

package boofcv.alg.feature.detect.line.gridline;

import boofcv.alg.feature.detect.line.GridRansacLineDetector;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.metric.UtilAngle;
import georegression.struct.line.LinePolar2D_F32;
import org.ddogleg.fitting.modelset.ModelMatcher;

/**
 * Implementation of {@link GridRansacLineDetector} for {@link GrayF32}
 *
 * @author Peter Abeles
 */
public class ImplGridRansacLineDetector_F32 extends GridRansacLineDetector<GrayF32>{

	public ImplGridRansacLineDetector_F32(int regionSize, int maxDetectLines, ModelMatcher<LinePolar2D_F32, Edgel> robustMatcher) {
		super(regionSize, maxDetectLines, robustMatcher);
	}

	protected void detectEdgels(int index0 , int x0 , int y0 ,
								GrayF32 derivX , GrayF32 derivY ,
								GrayU8 binaryEdges) {

		edgels.reset();
		for( int y = 0; y < regionSize; y++ ) {
			int index = index0 + y*binaryEdges.stride;

			for( int x = 0; x < regionSize; x++ ) {
				if( binaryEdges.data[index++] != 0 ) {
					Edgel e = edgels.grow();
					int xx = x0+x;
					int yy = y0+y;

					e.set(xx,yy);

					float dx = derivX.unsafe_get(xx,yy);
					float dy = derivY.unsafe_get(xx,yy);

					e.theta = UtilAngle.atanSafe(dy, dx);
				}
			}
		}
	}
}
