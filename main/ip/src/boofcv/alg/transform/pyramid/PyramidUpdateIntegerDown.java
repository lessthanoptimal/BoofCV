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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.convolve.GenericConvolveDown;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolveDown;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;

/**
 * <p>
 * Convolves a re-normalizable blur kernel across the image before down sampling.  This is useful for creating
 * a Gaussian pyramid as well as other standard pyramids.
 * </p>
 *
 * <p>
 * NOTE: This blur magnitude is constant for each level in the pyramid. In general it is desirable to
 * have it dependent on each level's scale factor.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidUpdateIntegerDown<T extends ImageBase> implements PyramidUpdaterDiscrete<T> {

	// stores the results from the first convolution
	private T temp;
	GenericConvolveDown<T,T> horizontal;
	GenericConvolveDown<T,T> vertical;

	public PyramidUpdateIntegerDown(Kernel1D kernel, Class<T> imageType ) {

		horizontal = FactoryConvolveDown.convolve(kernel,imageType,imageType,
				BorderType.NORMALIZED,true,1);
		vertical = FactoryConvolveDown.convolve(kernel,imageType,imageType,
				BorderType.NORMALIZED,false,1);

	}

	@Override
	public void update(T input , PyramidDiscrete<T> pyramid ) {

		if( !pyramid.isInitialized() ||
				pyramid.getInputWidth() != input.width ||
				pyramid.getInputHeight() != input.height )
			pyramid.initialize(input.width,input.height);
		
		if( temp == null )
			// declare it to be the latest image that it might need to be, resize below
			temp = (T)input._createNew(input.width/2,input.height);

		if (pyramid.scale[0] == 1) {
			if (pyramid.isSaveOriginalReference()) {
				pyramid.setFirstLayer(input);
			} else {
				pyramid.getLayer(0).setTo(input);
			}
		} else {
			int skip = pyramid.scale[0];

			horizontal.setSkip(skip);
			vertical.setSkip(skip);

			temp.reshape(input.width/skip,input.height);
			horizontal.process(input,temp);
			vertical.process(temp,pyramid.getLayer(0));
		}

		for (int index = 1; index < pyramid.getNumLayers(); index++) {
			int skip = pyramid.scale[index]/pyramid.scale[index-1];
			T prev = pyramid.getLayer(index-1);
			temp.reshape(prev.width/skip,prev.height);

			horizontal.setSkip(skip);
			vertical.setSkip(skip);

			horizontal.process(prev,temp);
			vertical.process(temp,pyramid.getLayer(index));
		}
	}

}
