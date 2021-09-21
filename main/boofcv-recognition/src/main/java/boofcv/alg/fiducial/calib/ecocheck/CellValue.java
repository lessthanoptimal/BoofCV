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

package boofcv.alg.fiducial.calib.ecocheck;

/**
 * Value of a decoded cell inside of {@link ECoCheckDetector}.
 *
 * @author Peter Abeles
 */
public class CellValue {
	/** Which marker the cell behinds to */
	public int markerID;
	/** Which cell inside the marker */
	public int cellID;

	public void setTo( int markerID, int cellID ) {
		this.markerID = markerID;
		this.cellID = cellID;
	}
}
