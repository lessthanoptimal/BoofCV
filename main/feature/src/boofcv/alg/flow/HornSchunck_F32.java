/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;

/**
 * Implementation of {@link HornSchunck} for {@link ImageFloat32}.
 *
 * @author Peter Abeles
 */
public class HornSchunck_F32 extends HornSchunck<ImageFloat32> {

	public HornSchunck_F32(float alpha, int numIterations) {
		super(alpha,numIterations);
	}

	@Override
	public void process( ImageFloat32 derivX , ImageFloat32 derivY ,
						 ImageFloat32 derivT , ImageFlow output) {

		if( derivX.isSubimage() || derivY.isSubimage() || derivT.isSubimage() )
			throw new IllegalArgumentException("No sub-images allowed.  More efficient processing.");

		averageFlow.reshape(output.width,output.height);
		output.fillZero();

		int N = output.width*output.height;

		for( int iter = 0; iter < numIterations; iter++ ) {

			borderAverageFlow(output,averageFlow);
			innerAverageFlow(output,averageFlow);

			for( int i = 0; i < N; i++ ) {
				float dx = derivX.data[i];
				float dy = derivY.data[i];
				float dt = derivT.data[i];

				ImageFlow.D aveFlow = averageFlow.data[i];

				float u = aveFlow.x;
				float v = aveFlow.y;

				ImageFlow.D flow = output.data[i];
				float r = (dx*u + dy*v + dt)/(alpha2 + dx*dx + dy*dy);
				flow.x = u - dx*r;
				flow.y = v - dy*r;
			}
		}
	}
}
