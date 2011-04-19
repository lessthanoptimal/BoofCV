/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.extract;


/**
 * A corner extractor that lets the user specify how many features it needs.
 *
 * @author Peter Abeles
 */
public interface CornerRequestExtractor extends CornerExtractor {
	/**
	 * This used to request a certain number of features be returned.  this is just
	 * a suggestion and more or less features can be returned.  It might even be
	 * ignored by some algorithms.  If it can the implementing algorithm should return
	 * at least this many features.
	 *
	 * @param numFeatures Number of features it should try to return.
	 */
	public void requestNumFeatures(int numFeatures);
}
