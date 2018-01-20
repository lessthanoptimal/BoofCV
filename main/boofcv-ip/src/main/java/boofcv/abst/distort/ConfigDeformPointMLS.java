/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.distort;

import boofcv.alg.distort.mls.ImageDeformPointMLS_F32;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link ImageDeformPointMLS_F32}
 *
 * @author Peter Abeles
 */
public class ConfigDeformPointMLS implements Configuration {

	/**
	 * Deformation model it should use
	 */
	public TypeDeformMLS type = TypeDeformMLS.RIGID;

	/**
	 * Number of columns in precomputed distortion grd
	 */
	public int cols = 50;

	/**
	 * Number of rows in precomputed distortion grd
	 */
	public int rows = 50;

	/**
	 * Used to tune distance function
	 */
	public float alpha = 3.0f/2.0f;

	@Override
	public void checkValidity() {

	}
}
