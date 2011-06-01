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

import gecv.abst.filter.FilterImageInterface;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;


/**
 * Updates a pyramid by filtering another image pyramid.  For example, computing the
 * derivative from another pyramid.
 *
 * @author Peter Abeles
 */
public class FilterPyramid<In extends ImageBase , Out extends ImageBase> {


	protected FilterImageInterface<In,Out> filter;

	public FilterPyramid(FilterImageInterface<In, Out> filter ) {
		this.filter = filter;
	}

	public void update( ImagePyramid<In> input , ImagePyramid<Out> output) {

		for( int i = 0; i < output.getNumLayers(); i++ ) {
			In imageIn = input.getLayer(i);
			Out imageOut = output.getLayer(i);

			filter.process(imageIn,imageOut);
		}
	}

}
