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

package gecv.abst.detect.extract;

/**
 * @author Peter Abeles
 */
public class FactoryFeatureFromIntensity
{

	/**
	 * Creates a corner extractor with the following properties.  It is assumed that any detector returned will
	 * be of the non-max suppression variety.  
	 *
	 * @param useCandidateList Will it use the provided list of candidate features?
	 * @param ignoreExisting Will it skip over features that have already been detected?
	 * @param acceptRequestNumber Will it detect features until the specified number have been found?
	 * @return A feature extractor.
	 */
	public CornerExtractor create( boolean useCandidateList , boolean ignoreExisting , boolean acceptRequestNumber )
	{
		return null;
	}
}
