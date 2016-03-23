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

import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class TestFlowKlt_to_DenseOpticalFlow extends GeneralDenseOpticalFlowChecks<GrayF32> {

	public TestFlowKlt_to_DenseOpticalFlow() {
		super(GrayF32.class);
	}

	@Override
	public DenseOpticalFlow<GrayF32> createAlg(Class<GrayF32> imageType) {
		return (DenseOpticalFlow)FactoryDenseOpticalFlow.flowKlt(null,2,imageType,null);
	}
}
