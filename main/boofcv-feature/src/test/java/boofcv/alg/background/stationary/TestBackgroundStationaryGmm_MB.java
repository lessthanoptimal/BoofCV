/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.struct.image.*;

/**
 * @author Peter Abeles
 */
public class TestBackgroundStationaryGmm_MB extends GenericBackgroundModelStationaryChecks {

	public TestBackgroundStationaryGmm_MB() {
		imageTypes.add(ImageType.il(3,InterleavedU8.class));
		imageTypes.add(ImageType.il(3,InterleavedF32.class));
		imageTypes.add(ImageType.pl(3,GrayU8.class));
		imageTypes.add(ImageType.pl(3,GrayF32.class));
	}

	@Override
	public <T extends ImageBase<T>> BackgroundModelStationary<T> create(ImageType<T> imageType) {
		return new BackgroundStationaryGmm_MB(1000.0f,0.001f,10,imageType);
	}
}