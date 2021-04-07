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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.ScaleTranslateRotate2D;
import org.ddogleg.fitting.modelset.ModelManager;

/**
 * {@link ModelManager} for {@link ScaleTranslateRotate2D}.
 *
 * @author Peter Abeles
 */
public class ModelManagerScaleTranslateRotate2D implements ModelManager<ScaleTranslateRotate2D> {
	@Override
	public ScaleTranslateRotate2D createModelInstance() {
		return new ScaleTranslateRotate2D();
	}

	@Override
	public void copyModel( ScaleTranslateRotate2D src, ScaleTranslateRotate2D dst ) {
		dst.setTo(src);
	}
}
