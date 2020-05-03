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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.*;
import boofcv.struct.Configuration;

/**
 * Configuration for creating {@link DescribeRegionPoint}
 *
 * @author Peter Abeles
 */
public class ConfigDescribeRegionPoint implements Configuration {

	/** The feature descriptor is used. Not always used. */
	public DescriptorType type = DescriptorType.SURF_FAST;

	/** Describes the scale-space used by SIFT */
	public ConfigSiftScaleSpace scaleSpaceSift = new ConfigSiftScaleSpace();

	public ConfigSurfDescribe.Fast surfFast = new ConfigSurfDescribe.Fast();
	public ConfigSurfDescribe.Stability surfStability = new ConfigSurfDescribe.Stability();
	public ConfigSiftDescribe sift = new ConfigSiftDescribe();
	public ConfigBrief brief = new ConfigBrief(false);
	public ConfigTemplateDescribe template = new ConfigTemplateDescribe();

	@Override
	public void checkValidity() {
		surfFast.checkValidity();
		surfStability.checkValidity();
		sift.checkValidity();
		brief.checkValidity();
		template.checkValidity();
	}

	public void setTo( ConfigDescribeRegionPoint src ) {
		this.type = src.type;
		this.scaleSpaceSift.setTo(src.scaleSpaceSift);
		this.surfFast.setTo(src.surfFast);
		this.surfStability.setTo(src.surfStability);
		this.sift.setTo(src.sift);
		this.brief.setTo(src.brief);
		this.template.setTo(src.template);
	}

	public enum DescriptorType {
		SURF_FAST,SURF_STABLE,SIFT,BRIEF,TEMPLATE,
	}
}
