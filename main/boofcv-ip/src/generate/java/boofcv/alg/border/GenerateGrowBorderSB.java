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

package boofcv.alg.border;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateGrowBorderSB extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();
		createImage(AutoTypeImage.I8);
		createImage(AutoTypeImage.I16);
		createImage(AutoTypeImage.S32);
		createImage(AutoTypeImage.S64);
		createImage(AutoTypeImage.F32);
		createImage(AutoTypeImage.F64);
		out.println("}");
	}

	private void printPreamble() {
		out.print(
				"import boofcv.struct.border.*;\n" +
				"import boofcv.struct.image.*;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * Implementations of {@link GrowBorder} for single band images.\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public interface GrowBorderSB<T extends ImageGray<T>,PixelArray> extends  GrowBorder<T,PixelArray>\n" +
				"{\n" +
				"\tabstract class SB_I_S32<T extends GrayI<T>,PixelArray> implements GrowBorderSB<T,PixelArray> {\n" +
				"\t\tT image;\n" +
				"\t\tImageBorder_S32<T> border;\n" +
				"\t\tImageType<T> imageType;\n" +
				"\n" +
				"\t\tpublic SB_I_S32(ImageType<T> imageType) {\n" +
				"\t\t\tthis.imageType = imageType;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t@Override\n" +
				"\t\tpublic void setBorder(ImageBorder<T> _border) {\n" +
				"\t\t\tborder = (ImageBorder_S32<T>)_border;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t@Override\n" +
				"\t\tpublic void setImage(T image) {\n" +
				"\t\t\tthis.image = image;\n" +
				"\t\t\tthis.border.setImage(image);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t@Override\n" +
				"\t\tpublic ImageType<T> getImageType() {\n" +
				"\t\t\treturn imageType;\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void createImage(AutoTypeImage type ) {

		String className = "SB_"+type.getAbbreviatedType();
		String dataType = type.getDataType();
		String typeCastFromSum = type.getTypeCastFromSum();

		String imageType = type.getSingleBandName();
		String signature;

		if( type.isInteger() && type.getNumBits() < 32 ) {
			signature =
					"\tclass "+className+"<T extends "+imageType+"<T>> extends SB_I_S32<T,"+dataType+"[]> {\n" +
					"\n" +
					"\t\tpublic "+className+"(ImageType<T> imageType) {\n" +
					"\t\t\tsuper(imageType);\n" +
					"\t\t}\n" +
					"\n";
		} else {
			String borderType;
			if( type.isInteger() && type.getNumBits() == 32 ) {
				borderType = "ImageBorder_S32<"+imageType+">";
			} else {
				borderType = type.getBorderNameSB();
			}
			signature =
					"\tclass "+className+" implements GrowBorderSB<"+imageType+","+dataType+"[]> {\n" +
					"\t\t"+imageType+" image;\n" +
					"\t\t"+borderType+" border;\n" +
					"\n" +
					"\t\t@Override\n" +
					"\t\tpublic void setBorder(ImageBorder<"+imageType+"> border) {\n" +
					"\t\t\tthis.border = ("+borderType+")border;\n" +
					"\t\t}\n" +
					"\n" +
					"\t\t@Override\n" +
					"\t\tpublic void setImage("+imageType+" image) {\n" +
					"\t\t\tthis.image = image;\n" +
					"\t\t\tthis.border.setImage(image);\n" +
					"\t\t}\n" +
					"\n" +
					"\t\t@Override\n" +
					"\t\tpublic ImageType<"+imageType+"> getImageType() {\n" +
					"\t\t\treturn ImageType."+className+";\n" +
					"\t\t}\n";

		}

		out.print(signature +
				"\t\t@Override\n" +
				"\t\tpublic void growRow(int y, int borderLower , int borderUpper, "+dataType+"[] output, int offset) {\n" +
				"\t\t\tint idxDst = offset;\n" +
				"\t\t\tif( y < 0 || y >= image.height ) {\n" +
				"\t\t\t\tint end = image.width+borderUpper;\n" +
				"\t\t\t\tfor (int i = -borderLower; i < end; i++) {\n" +
				"\t\t\t\t\toutput[idxDst++] = "+typeCastFromSum+"border.getOutside(i, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tfor (int i = borderLower; i > 0; i--) {\n" +
				"\t\t\t\t\toutput[idxDst++] = "+typeCastFromSum+"border.getOutside(-i, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tSystem.arraycopy(image.data, image.getIndex(0, y), output, idxDst, image.width);\n" +
				"\t\t\t\tidxDst += image.width;\n" +
				"\t\t\t\tfor (int i = 0; i < borderUpper; i++) {\n" +
				"\t\t\t\t\toutput[idxDst++] = "+typeCastFromSum+"border.getOutside(image.width + i, y);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t@Override\n" +
				"\t\tpublic void growCol(int x, int borderLower , int borderUpper, "+dataType+"[] output, int offset) {\n" +
				"\t\t\tint idxDst = offset;\n" +
				"\n" +
				"\t\t\tif( x < 0 || x >= image.width ) {\n" +
				"\t\t\t\tint end = image.height+borderUpper;\n" +
				"\t\t\t\tfor (int i = -borderLower; i < end; i++) {\n" +
				"\t\t\t\t\toutput[idxDst++] = "+typeCastFromSum+"border.getOutside(x, i);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tint idxSrc = image.startIndex + x;\n" +
				"\t\t\t\tfor (int i = borderLower; i > 0; i--) {\n" +
				"\t\t\t\t\toutput[idxDst++] = "+typeCastFromSum+"border.getOutside(x, -i);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tfor (int y = 0; y < image.height; y++, idxSrc += image.stride) {\n" +
				"\t\t\t\t\toutput[idxDst++] = image.data[idxSrc];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tfor (int i = 0; i < borderUpper; i++) {\n" +
				"\t\t\t\t\toutput[idxDst++] = "+typeCastFromSum+"border.getOutside(x, image.height + i);\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		new GenerateGrowBorderSB().generateCode();
	}
}
