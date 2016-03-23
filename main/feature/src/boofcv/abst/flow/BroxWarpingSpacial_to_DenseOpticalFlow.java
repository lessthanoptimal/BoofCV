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

package boofcv.abst.flow;

import boofcv.alg.flow.BroxWarpingSpacial;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Implementation of {@link BroxWarpingSpacial} for {@link boofcv.alg.flow.HornSchunck}.
 *
 * @author Peter Abeles
 */
public class BroxWarpingSpacial_to_DenseOpticalFlow<T extends ImageGray>
	implements DenseOpticalFlow<T>
{
	BroxWarpingSpacial<T> brox;
	Class<T> imageType;

	public BroxWarpingSpacial_to_DenseOpticalFlow(BroxWarpingSpacial<T> brox,
												  Class<T> imageType ) {
		this.brox = brox;
		this.imageType = imageType;
	}

	@Override
	public void process(T source, T destination, ImageFlow flow) {

		brox.process(source, destination);

		GrayF32 flowX = brox.getFlowX();
		GrayF32 flowY = brox.getFlowY();

		int index = 0;
		for( int y = 0; y < flow.height; y++){
			for( int x = 0; x < flow.width; x++, index++ ){
				ImageFlow.D d = flow.unsafe_get(x,y);
				d.x = flowX.data[index];
				d.y = flowY.data[index];
			}
		}
	}

	@Override
	public ImageType<T> getInputType() {
		return ImageType.single(imageType);
	}
}
