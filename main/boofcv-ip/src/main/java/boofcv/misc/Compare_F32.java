/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

/**
 * Compares two scores to see which is better
 *
 * @author Peter Abeles
 */
public interface Compare_F32 {
	/**
	 * <ul>
	 *     <li>1 = scoreA is better than scoreB</li>
	 *     <li>0 = scoreA is equivalent than scoreB</li>
	 *     <li>-1 = scoreA is worse than scoreB</li>
	 * </ul>
	 */
	int compare( float scoreA , float scoreB );
}
