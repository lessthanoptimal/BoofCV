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

package boofcv.abst.fiducial.calib;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Configuration for detecting ChessboardBits markers. These markers are chessboard patterns with marker ID and cell ID
 * encoded in the white spaces. Intended for use in multi camera calibration. The number of unique markers and their
 * shapes is required to known before a marker can be detected as the encoding changes based on these values.
 *
 * @see boofcv.alg.fiducial.calib.chessbits.ChessboardReedSolomonDetector
 *
 * @author Peter Abeles
 */
public class ConfigChessboardBits implements Configuration {

	/** Fraction of a cell's length the data bit is */
	public double dataBitWidthFraction = 0.7;

	/** Fraction of the length the quite zone is around data bits */
	public double dataBorderFraction = 0.15;

	/** Describes how to detect the chessboard */
	public final ConfigChessboardX chessboard = new ConfigChessboardX();

	@Override public void checkValidity() {
		BoofMiscOps.checkFraction(dataBitWidthFraction, "dataBitWidthFraction must be 0 to 1.0.");
		BoofMiscOps.checkFraction(dataBorderFraction, "dataBorderFraction must be 0 to 1.0.");
		chessboard.checkValidity();
	}
}
