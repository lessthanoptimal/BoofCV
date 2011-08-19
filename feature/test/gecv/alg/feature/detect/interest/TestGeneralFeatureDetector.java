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

package gecv.alg.feature.detect.interest;

import gecv.struct.feature.ScalePoint;
import gecv.struct.image.ImageFloat32;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class TestGeneralFeatureDetector extends GenericFeatureDetector {

	@Override
	protected Object createDetector() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	protected List<ScalePoint> detectFeature(ImageFloat32 input, double[] scales, Object detector) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
