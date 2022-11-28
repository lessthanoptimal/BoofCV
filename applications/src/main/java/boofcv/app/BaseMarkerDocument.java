/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.generate.LengthUnit;
import boofcv.generate.PaperSize;
import boofcv.generate.Unit;
import org.kohsuke.args4j.Option;

/**
 * Common code for markers such as QR and Micro QR
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class BaseMarkerDocument {
	@Option(name = "-u", aliases = {"--Units"}, usage = "Name of document units. default: cm")
	protected String _unit = Unit.CENTIMETER.abbreviation;
	public Unit unit = Unit.UNKNOWN;

	@Option(name = "-p", aliases = {"--PaperSize"}, usage = "Size of paper used. See below for predefined document sizes. "
			+ "You can manually specify any size using the following notation. W:H where W is the width and H is the height. "
			+ "Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional. If no unit"
			+ " are specified the default document units are used.")
	protected String _paperSize = PaperSize.LETTER.name;
	public PaperSize paperSize;

	@Option(name = "-w", aliases = {"--MarkerWidth"}, usage = "Width of the marker. In document units.")
	public float markerWidth = -1;

	@Option(name = "-mw", aliases = {"--ModuleWidth"}, usage = "Specify size of an individual square. In document units.")
	public float moduleWidth = -1;

	@Option(name = "-s", aliases = {"--Space"}, usage = "Spacing between the fiducials. In document units.")
	public float spaceBetween = 2;

	@Option(name = "-o", aliases = {"--OutputName"}, usage = "Name of output file. Extension determines file type. E.g. marker.pdf. " +
			"Valid extensions are pdf, png, jpg, gif, bmp")
	public String fileName = "marker";

	@Option(name = "--GridFill", usage = "Flag to turn on filling the entire document with a grid of marker")
	public boolean gridFill = false;

	@Option(name = "--DrawGrid", usage = "Draws a line showing the grid")
	public boolean drawGrid = false;

	@Option(name = "--HideInfo", usage = "Flag that's used to turn off the printing of extra information")
	public boolean hideInfo = false;

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	public boolean guiMode = false;

	// if true it will send a document to the printer instead of saving it
	public boolean sendToPrinter = false;
	// specifies the file type
	public String fileType;

	protected void parsePaperSze() {
		unit = unit == Unit.UNKNOWN ? Unit.lookup(_unit) : unit;
		if (unit == Unit.UNKNOWN) {
			failExit("Must specify a valid unit or use default");
		}
		PaperSize paperSize = PaperSize.lookup(_paperSize);
		if (paperSize == null) {
			String[] words = _paperSize.split(":");
			if (words.length == 1) failExit("Paper Size: Unknown document name '" + words[0] + "'");
			if (words.length != 2) failExit("Paper Size: Expected two value+unit separated by :");
			var w = new LengthUnit(words[0], unit);
			var h = new LengthUnit(words[1], unit);
			if (w.unit != h.unit) failExit("Same units must be specified for width and height");
			paperSize = new PaperSize(w.length, h.length, w.getUnit());
		}
		this.paperSize = paperSize;
	}

	protected void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}
}
