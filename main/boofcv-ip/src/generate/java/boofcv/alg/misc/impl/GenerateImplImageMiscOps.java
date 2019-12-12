/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;


/**
 * Generates functions inside of ImplImageMiscOps.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GenerateImplImageMiscOps extends CodeGeneratorBase {

	private AutoTypeImage imageType;
	private String imageName;
	private String imageNameI;
	private String dataType;
	private String bitWise;

	@Override
	public void generate() throws FileNotFoundException {
		printPreamble();
		printAllGeneric();
		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"import boofcv.alg.misc.ImageMiscOps;\n" +
				"import boofcv.struct.border.ImageBorder_F32;\n" +
				"import boofcv.struct.border.ImageBorder_F64;\n" +
				"import boofcv.struct.border.ImageBorder_S32;\n" +
				"import boofcv.struct.border.ImageBorder_S64;\n" +
				"\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * Implementations of functions for {@link ImageMiscOps}\n" +
				" *\n" +
				generateDocString() +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				generatedAnnotation() +
				"public class " + className + " {\n\n");
	}

	private void printAllGeneric() {
		AutoTypeImage[] types = AutoTypeImage.getGenericTypes();

		for( AutoTypeImage t : types ) {
			imageType = t;
			imageName = t.getSingleBandName();
			imageNameI = t.getInterleavedName();
			dataType = t.getDataType();
			growBorder();
		}
	}

	private void growBorder() {
		String borderName = "ImageBorder_"+imageType.getKernelType();
		String generic = "";
		String typecast;
		String srcType = imageName;
		if( imageType.isInteger() && imageType.getNumBits() < 32 ) {
			typecast = "("+dataType+")";
			generic = "<T extends GrayI"+imageType.getNumBits()+"<T>>\n\t";
			srcType = "T";
			borderName += "<T>";
		} else {
			typecast = "";
		}
		out.print("\tpublic static "+generic+"void growBorder("+srcType+" src , "+borderName+" border, int borderX0, int borderY0, int borderX1, int borderY1 , "+srcType+" dst )\n" +
				"\t{\n" +
				"\t\tdst.reshape(src.width+borderX0+borderX1, src.height+borderY0+borderY1);\n" +
				"\t\tborder.setImage(src);\n" +
				"\n" +
				"\t\t// Copy src into the inner portion of dst\n" +
				"\t\tImageMiscOps.copy(0,0,borderX0,borderY0,src.width,src.height,src,dst);\n" +
				"\n" +
				"\t\t// Top border\n" +
				"\t\tfor (int y = 0; y < borderY0; y++) {\n" +
				"\t\t\tint idxDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = "+typecast+"border.get(x-borderX0,y-borderY0);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t// Bottom border\n" +
				"\t\tfor (int y = 0; y < borderY1; y++) {\n" +
				"\t\t\tint idxDst = dst.startIndex + (dst.height-borderY1+y)*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < dst.width; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = "+typecast+"border.get(x-borderX0,src.height+y);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t\t// Left and right border\n" +
				"\t\tfor (int y = borderY0; y < dst.height-borderY1; y++) {\n" +
				"\t\t\tint idxDst = dst.startIndex + y*dst.stride;\n" +
				"\t\t\tfor (int x = 0; x < borderX0; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = "+typecast+"border.get(x-borderX0,y-borderY0);\n" +
				"\t\t\t}\n" +
				"\t\t\tidxDst = dst.startIndex + y*dst.stride+src.width+borderX0;\n" +
				"\t\t\tfor (int x = 0; x < borderX1; x++) {\n" +
				"\t\t\t\tdst.data[idxDst++] = "+typecast+"border.get(src.width+x,y-borderY0);\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateImplImageMiscOps gen = new GenerateImplImageMiscOps();
		gen.generate();
	}
}
