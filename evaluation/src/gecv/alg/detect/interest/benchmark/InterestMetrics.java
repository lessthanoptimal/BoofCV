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

package gecv.alg.detect.interest.benchmark;

import gecv.abst.detect.interest.InterestPointDetector;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public class InterestMetrics<T extends ImageBase>
{
	public String name;
	public InterestPointDetector<T> detector;
	public Object metric;

	public InterestMetrics(String name, InterestPointDetector<T> detector) {
		this.name = name;
		this.detector = detector;
	}

	public <T>T getMetric() {
		return (T)metric;
	}

	public void setMetric(Object metric) {
		this.metric = metric;
	}
}
