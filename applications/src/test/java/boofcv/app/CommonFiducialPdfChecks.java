/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.After;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class CommonFiducialPdfChecks {
	String document_name = "target";

	public BufferedImage loadImage() throws IOException {
		PDDocument document = PDDocument.load(new File(document_name+".pdf"));
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		if( document.getNumberOfPages() != 1 )
			throw new RuntimeException("Egads");
		BufferedImage output = pdfRenderer.renderImageWithDPI(0, 150, ImageType.RGB);
		document.close();
		return output;
	}

	@After
	public void cleanup() {
		new File(document_name+".pdf").delete();
	}
}
