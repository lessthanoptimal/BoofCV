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

package boofcv.factory.geo;

import boofcv.struct.Configuration;

/**
 * Configuration parameters for solving the PnP problem
 *
 * @author Peter Abeles
 */
public class ConfigPnP implements Configuration {

	/**
	 * Which algorithm should it use. Only use essential matrix ones.
	 */
	public EnumPNP which = EnumPNP.P3P_FINSTERWALDER;

	/**
	 * How many points should be used to resolve ambiguity in the solutions?
	 */
	public int numResolve = 1;

	/**
	 * Number of iterations for EPNP. Ignored by everything else
	 */
	public int epnpIterations = 10;

	public ConfigPnP setTo( ConfigPnP src ) {
		this.which = src.which;
		this.numResolve = src.numResolve;
		this.epnpIterations = src.epnpIterations;
		return this;
	}

	@Override
	public void checkValidity() {
//		switch (which) {
//			case P3P_FINSTERWALDER:
//			case P3P_GRUNERT:
//				break;
//
//			default:
//				throw new IllegalArgumentException("EPnP isn't handled here yet");
//		}
	}
}
