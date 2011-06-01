/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.pyramid;

import gecv.abst.filter.convolve.BorderType;
import gecv.abst.filter.convolve.FactoryConvolveDown;
import gecv.abst.filter.convolve.GenericConvolveDown;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.image.ImageBase;

/**
 * Convolves a re-normalizable blur kernel across the image before down sampling.  This is useful for creating
 * a Gaussian pyramid as well as other standard pyramids.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class ConvolutionPyramid<T extends ImageBase> extends PyramidUpdater<T> {

	// stores the results from the first convolution
	private T temp;
	GenericConvolveDown<T,T> horizontal;
	GenericConvolveDown<T,T> vertical;

	public ConvolutionPyramid(Kernel1D kernel, Class<T> imageType ) {

		horizontal = FactoryConvolveDown.convolve(kernel,imageType,imageType,
				BorderType.NORMALIZED,true,1);
		vertical = FactoryConvolveDown.convolve(kernel,imageType,imageType,
				BorderType.NORMALIZED,false,1);

	}

	@Override
	public void _update(T original) {

		if( temp == null )
			// declare it to be hte latest image that it might need to be, resize below
			temp = (T)original._createNew(original.width/2,original.height);

		if (pyramid.scale[0] == 1) {
			if (pyramid.saveOriginalReference) {
				pyramid.layers[0] = original;
			} else {
				pyramid.layers[0].setTo(original);
			}
		} else {
			int skip = pyramid.scale[0];

			horizontal.setSkip(skip);
			vertical.setSkip(skip);

			temp.reshape(original.width/skip,original.height);
			horizontal.process(original,temp);
			vertical.process(temp,pyramid.layers[0]);
		}

		for (int index = 1; index < pyramid.layers.length; index++) {
			int skip = pyramid.scale[index];
			T prev = pyramid.layers[index-1];
			temp.reshape(prev.width/skip,prev.height);

			horizontal.setSkip(skip);
			vertical.setSkip(skip);

			horizontal.process(prev,temp);
			vertical.process(temp,pyramid.layers[index]);
		}
	}

}
