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

package gecv.alg.detect.intensity;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;

/**
 * <p>
 * Generic interface for fast corner detection algorithms.  The general idea is that at the points in a circle around
 * the center point should either be mostly above or mostly below the center pixel's intensity value value.  With
 * this information candidates can be quickly eliminated.  See the paper: "Faster and better: a machine learning
 * approach to corner detection" by Edward Rosten, Reid Porter, and Tom Drummond.
 * <p/>
 *
 * @author Peter Abeles
 */
public interface FastCornerIntensity<T extends ImageBase> extends CornerIntensity<T> {

	/**
	 * Extracts corner features from the provided image.
	 *
	 * @param input Input image which corners are to be detected inside of.
	 */
	public void process( T input );

	/**
	 * Returns a list of candidate locations for corners.  All other pixels are assumed to not be corners.
	 *
	 * @return List of potential corners.
	 */
	public QueueCorner getCandidates();
	
}
