/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.bundle;

import java.util.Map;

/**
 * <p>Interface for an object which describes the camera's state. It provides functions for serializing and
 * deserializing so that the state can be saved to disk. The camera model will typecast this to the appropriate
 * class internally.</p>
 *
 * <p>Typically the camera state will be used to store parameters such as the current focal length or similar.</p>
 *
 * @author Peter Abeles
 */
public interface BundleCameraState {
	/**
	 * Set's the classes state to the value contained in this map
	 */
	BundleCameraState setTo( Map<String, Object> src );

	/**
	 * Convert's the values into a map format where each class's field has a corresponding key with the same name and
	 * primitive value or primitive array. This is used for serialization to YAML.
	 */
	Map<String, Object> toMap();

	/**
	 * Returns true if the provided camera state is identical to this camera state. If they are different types
	 * then it should return false.
	 */
	boolean isIdentical( BundleCameraState b );
}
