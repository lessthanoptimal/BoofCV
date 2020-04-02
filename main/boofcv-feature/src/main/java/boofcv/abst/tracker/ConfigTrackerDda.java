/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.tracker;

/**
 * Configuration for {@link DetectDescribeAssociate}
 *
 * @author Peter Abeles
 */
public class ConfigTrackerDda {
	/**
	 * Update the description each time its successfully matched?
	 */
	public boolean updateDescription = false;

	/**
	 * Random seed
	 */
	public long seed=0xDEADBEEF;
}
