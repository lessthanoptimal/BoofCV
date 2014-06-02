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

package boofcv.abst.flow;

import boofcv.alg.flow.HornSchunckPyramid;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Implementation of {@link boofcv.abst.flow.DenseOpticalFlow} for {@link boofcv.alg.flow.HornSchunck}.
 *
 * @author Peter Abeles
 */
public class HornSchunckPyramid_to_DenseOpticalFlow<T extends ImageSingleBand>
	implements DenseOpticalFlow<T>
{
	HornSchunckPyramid<T> hornSchunck;
	Class<T> imageType;

	public HornSchunckPyramid_to_DenseOpticalFlow(HornSchunckPyramid<T> hornSchunck, Class<T> imageType ) {
		this.hornSchunck = hornSchunck;
		this.imageType = imageType;
	}

	@Override
	public void process(T source, T destination, ImageFlow flow) {

		hornSchunck.process(source,destination);

		ImageFloat32 flowX = hornSchunck.getFlowX();
		ImageFloat32 flowY = hornSchunck.getFlowY();

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
