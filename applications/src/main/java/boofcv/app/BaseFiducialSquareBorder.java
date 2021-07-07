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

import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.app.fiducials.CreateSquareFiducialDocumentImage;
import boofcv.app.fiducials.CreateSquareFiducialDocumentPDF;
import boofcv.generate.Unit;
import org.kohsuke.args4j.Option;

/**
 * <p>
 * Base class for generating square fiducial PDF documents for printing. Fiducials are placed in a regular grid.
 * The width of each element in the grid is the fiducial's width (pattern + black border) and a white border. The
 * grid starts in the page's lower left and corner.
 * </p>
 *
 * <pre>
 * Border:  The border is a no-go zone where the fiducial can't be printed inside of. This is only taken in account
 *          when automatic centering or layout of the grid on the page is requested.
 * Offset: Where the fiducial is offset inside the page. Always used. If centering is requested then the offset
 *         is automatically computed and any user provided value ignored.
 * PrintInfo: If true it will draw a string above the fiducial with the fiducial's  name and it's size
 * </pre>
 *
 * @author Peter Abeles
 */
public abstract class BaseFiducialSquareBorder extends BaseFiducialSquare {

	@Option(name = "-bw", aliases = {"--BlackBorder"}, usage = "Fractional width of black border")
	public float blackBorderFractionalWidth = 0.25f;

	@Override
	protected CreateFiducialDocumentImage createRendererImage( String filename ) {
		CreateSquareFiducialDocumentImage ret = new CreateSquareFiducialDocumentImage(filename);
		ret.setBlackBorderFractionalWidth(blackBorderFractionalWidth);
		ret.setWhiteBorder((int)spaceBetween);
		return ret;
	}

	@Override
	protected CreateFiducialDocumentPDF createRendererPdf( String documentName, PaperSize paper, Unit units ) {
		CreateSquareFiducialDocumentPDF ret = new CreateSquareFiducialDocumentPDF(documentName, paper, units);
		ret.blackBorderFractionalWidth = blackBorderFractionalWidth;
		return ret;
	}
}
