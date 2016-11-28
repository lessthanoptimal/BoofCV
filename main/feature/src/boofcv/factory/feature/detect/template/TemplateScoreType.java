/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.template;

/**
 * List of formulas used to score matches in a template.
 *
 * @author Peter Abeles
 */
public enum TemplateScoreType {
	/**
	 * <p>Sum of the difference squared or Euclidean error squared.</p>
	 *
	 * <p>error = Sum<sub>(o,u)</sub> [ I(x,y) - T(x-o,y-u) ]^2 </p>
	 *
	 * @see boofcv.alg.feature.detect.template.TemplateDiffSquared
	 */
	SUM_DIFF_SQ,
	/**
	 * <p>
	 * Normalized Cross Correlation (NCC) error metric.  Adds invariance to lighting conditions
	 * but is more expensive to compute.
	 * </p>
	 *
	 * @see boofcv.alg.feature.detect.template.TemplateNCC
	 */
	NCC,

	/**
	 * <p>
	 * Correlation error metric.  On large images this can be much faster than the other techniques.
	 * </p>
	 *
	 * @see boofcv.alg.feature.detect.template.TemplateCorrelationFFT
	 */
	CORRELATION
}
