/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.f.EssentialNister5;
import boofcv.alg.geo.f.FundamentalLinear7;
import boofcv.alg.geo.f.FundamentalLinear8;
import boofcv.alg.geo.h.HomographyLinear4;
import boofcv.alg.geo.pose.PnPLepetitEPnP;

/**
 * Factory for creating low level non-abstracted algorithms related to geometric vision
 *
 * @author Peter Abeles
 */
public class FactoryGeometryAlgs {

	/**
	 * Creates a new instance of {@link EssentialNister5}.  See class documentation
	 * for information on parameters.
	 */
	public static EssentialNister5 essential5() {
		return new EssentialNister5();
	}

	/**
	 * Creates a new instance of {@link FundamentalLinear7}.  See class documentation
	 * for information on parameters.
	 */
	public static FundamentalLinear7 fundamental7( boolean computeFundamental ) {
		return new FundamentalLinear7(computeFundamental);
	}

	/**
	 * Creates a new instance of {@link FundamentalLinear8}.  See class documentation
	 * for information on parameters.
	 */
	public static FundamentalLinear8 fundamental8( boolean computeFundamental ) {
		return new FundamentalLinear8(computeFundamental);
	}

	/**
	 * Creates a new instance of {@link HomographyLinear4}.  See class documentation
	 * for information on parameters.
	 */
	public static HomographyLinear4 homography4( boolean normalize ) {
		return new HomographyLinear4(normalize);
	}

	/**
	 * Creates a new instance of {@link PnPLepetitEPnP}.  See class documentation
	 * for information on parameters.
	 */
	public static PnPLepetitEPnP pnpLepetit( double magicNumber ) {
		return new PnPLepetitEPnP(magicNumber);
	}
}
