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

package boofcv.app;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * Allows arrays of longs to be passed in as a command line argument
 *
 * @author Peter Abeles
 */
public class LongArrayOptionHandler extends OptionHandler<Long> {
	public LongArrayOptionHandler( CmdLineParser parser, OptionDef option, Setter<Long> setter ) {
		super(parser, option, setter);
	}

	/**
	 * Returns {@code "LONG[]"}.
	 *
	 * @return return "LONG[]";
	 */
	@Override
	public String getDefaultMetaVariable() {
		return "LONG[]";
	}

	/**
	 * Tries to parse {@code Long[]} argument from {@link Parameters}.
	 */
	@Override
	public int parseArguments( Parameters params ) throws CmdLineException {
		int counter = 0;
		for (; counter < params.size(); counter++) {
			String param = params.getParameter(counter);

			if (param.startsWith("-")) {
				break;
			}

			for (String p : param.split(" ")) {
				setter.addValue(Long.parseLong(p));
			}
		}

		return counter;
	}
}
