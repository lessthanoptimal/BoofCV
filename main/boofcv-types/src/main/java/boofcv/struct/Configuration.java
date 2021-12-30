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

package boofcv.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Complex algorithms with several parameters can specify their parameters using a separate class. This interface
 * is intended to provide a common interface for all such configuration classes.
 *
 * @author Peter Abeles
 */
public interface Configuration extends Serializable {

	/**
	 * Checks to see if the configuration is valid. If it is invalid, throw an exception explaining what is
	 * incorrect.
	 */
	void checkValidity();

	// Why don't we have this below? The setTo() function is required but not part of the interface because if
	// it was part of the interface there would be two options 1) no runtime type checking. 2) recursive
	// generics and weaker type checking. By having each implementation implement it's own custom function we
	// get strong typing at compile time. Plus there has been no situation where calling setTo() on the raw interface
	// would have been useful after all this time...
//	void setTo( Configuration src );

	/**
	 * Optional function which is called after deserialization and performs initialization
	 */
	default void serializeInitialize() {}

	/**
	 * Optional functions that tells a serializer which fields are being used and not ignored.
	 * a field is ignored when there a "type" and only members of the type are used. If empty then
	 * it's assumed all fields are active.
	 */
	default List<String> serializeActiveFields() {return new ArrayList<>();}
}
