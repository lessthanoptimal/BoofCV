/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.struct.calib.CameraModel;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CROperatorForward;

/// Camera projection model for [CROperatorForward]. This interface adds the
/// ability to encode the camera into a parameter vector
public interface CRCameraModel<T> extends CROperatorForward<T> {

	/// Assigns the camera model to match the passed in model. If the camera models are not the same then throw
	/// an exception
	<M extends CRCameraModel<T>> M setTo( CameraModel src );

	/// Converts the camera's parameters into a vector
	void encodeCamera( CRArray<double[]> dst );
}
