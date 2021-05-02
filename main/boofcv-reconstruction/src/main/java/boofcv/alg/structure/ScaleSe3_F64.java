/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import georegression.struct.se.Se3_F64;

/**
 * Scale and SE3 transform
 *
 * @author Peter Abeles
 */
public class ScaleSe3_F64 {
	/** Specifies the difference in scale. Apply to translation BEFORE applying 'transform' */
	public double scale = 1.0;

	/** Rigid body transform between */
	public final Se3_F64 transform = new Se3_F64();

	public void reset() {
		scale = 1.0;
		transform.reset();
	}
}
