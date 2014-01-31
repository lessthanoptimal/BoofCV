/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

/**
 * List of connectivity rules.
 *
 * @author Peter Abeles
 */
public enum ConnectRule {
	/**
	 * Four connect neighborhood.  (1,0) (0,1) (-1,0) (0,1)
	 */
	FOUR("4"),
	/**
	 * Eight connect neighborhood. (1,0) (0,1) (-1,0) (0,1) (1,1) (-1,1) (1,-1) (-1,-1)
	 */
	EIGHT("8");

	String shortName;

	private ConnectRule(String shortName) {
		this.shortName = shortName;
	}

	public String getShortName() {
		return shortName;
	}
}
