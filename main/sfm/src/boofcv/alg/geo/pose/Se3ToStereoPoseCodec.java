/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.struct.sfm.StereoPose;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelCodec;

/**
 * Allows a {@link ModelCodec} for {@link Se3_F64} to be used by {@link StereoPose}. Only the worldToCam0
 * transform is modified.
 *
 * @author Peter Abeles
 */
public class Se3ToStereoPoseCodec implements ModelCodec<StereoPose> {

	// converts to and from parameterization
	private ModelCodec<Se3_F64> codec;

	public Se3ToStereoPoseCodec(ModelCodec<Se3_F64> codec) {
		this.codec = codec;
	}

	@Override
	public void decode(double[] input, StereoPose outputModel) {
		codec.decode(input,outputModel.worldToCam0);
	}

	@Override
	public void encode(StereoPose model, double[] param) {
		codec.encode(model.worldToCam0, param);
	}

	@Override
	public int getParamLength() {
		return codec.getParamLength();
	}
}
