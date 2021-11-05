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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.*;
import boofcv.struct.Configuration;

/**
 * Configuration for creating {@link DescribePointRadiusAngle}
 *
 * @author Peter Abeles
 */
public class ConfigDescribeRegion implements Configuration {

	/** The feature descriptor is used. Not always used. */
	public Type type = Type.SURF_FAST;

	/** Describes the scale-space used by SIFT */
	public ConfigSiftScaleSpace scaleSpaceSift = new ConfigSiftScaleSpace();

	public ConfigSurfDescribe.Fast surfFast = new ConfigSurfDescribe.Fast();
	public ConfigSurfDescribe.Stability surfStability = new ConfigSurfDescribe.Stability();
	public ConfigSiftDescribe sift = new ConfigSiftDescribe();
	public ConfigBrief brief = new ConfigBrief(false);
	public ConfigTemplateDescribe template = new ConfigTemplateDescribe();

	/** Convert the descriptor into a different format */
	public ConfigConvertTupleDesc convert = new ConfigConvertTupleDesc();

	@Override
	public void checkValidity() {
		surfFast.checkValidity();
		surfStability.checkValidity();
		sift.checkValidity();
		brief.checkValidity();
		template.checkValidity();
		convert.checkValidity();
	}

	public ConfigDescribeRegion setTo( ConfigDescribeRegion src ) {
		this.type = src.type;
		this.scaleSpaceSift.setTo(src.scaleSpaceSift);
		this.surfFast.setTo(src.surfFast);
		this.surfStability.setTo(src.surfStability);
		this.sift.setTo(src.sift);
		this.brief.setTo(src.brief);
		this.template.setTo(src.template);
		this.convert.setTo(src.convert);
		return this;
	}

	public enum Type {
		SURF_FAST,
		SURF_STABLE,
		SURF_COLOR_FAST,
		SURF_COLOR_STABLE,
		SIFT,
		BRIEF,
		TEMPLATE,
	}
}
