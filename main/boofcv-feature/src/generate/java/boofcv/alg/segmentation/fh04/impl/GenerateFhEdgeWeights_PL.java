/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.fh04.impl;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;
import boofcv.struct.ConnectRule;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateFhEdgeWeights_PL extends CodeGeneratorBase {
	GenerateFhEdgeWeights_PL() {
		super.className = "off";
	}

	@Override
	public void generateCode() throws FileNotFoundException {
		create(AutoTypeImage.F32,ConnectRule.EIGHT);
		create(AutoTypeImage.U8,ConnectRule.EIGHT);
		create(AutoTypeImage.F32,ConnectRule.FOUR);
		create(AutoTypeImage.U8,ConnectRule.FOUR);
	}

	protected void create( AutoTypeImage imageType , ConnectRule rule) throws FileNotFoundException {

		className = "FhEdgeWeights"+rule.getShortName()+"_PL"+imageType.getAbbreviatedType();
		initFile();
		printPreamble(imageType,rule);
		printConstructor(imageType);
		printProcess(imageType,rule);
		printCheckAround(imageType,rule);
		printCheck(imageType);
		printType(imageType);
		out.print("}\n");
	}

	private void printPreamble( AutoTypeImage imageType , ConnectRule rule ) {

		String imageName = imageType.getSingleBandName();

		String ruleName = rule.getShortName();

		out.print("import boofcv.struct.image."+imageName+";\n" +
				"import boofcv.struct.image.ImageType;\n" +
				"import boofcv.alg.segmentation.fh04.FhEdgeWeights;\n" +
				"import boofcv.struct.image.Planar;\n" +
				"import org.ddogleg.struct.DogArray;\n" +
				"\n" +
				"import static boofcv.alg.segmentation.fh04.SegmentFelzenszwalbHuttenlocher04.Edge;\n" +
				"\n" +
				"/**\n" +
				" * <p>Computes edge weight as the F-norm different in pixel value for {@link Planar} images.\n" +
				" * A "+ruleName+"-connect neighborhood is considered.</p>\n" +
				" *\n" +
				generateDocString("Peter Abeles") +
				"public class "+className+" implements FhEdgeWeights<Planar<"+imageName+">> {\n\n");
	}

	private void printConstructor( AutoTypeImage imageType ) {
		String sumType = imageType.getSumType();

		out.print("\t"+sumType+"[] pixelColor = new "+sumType+"[1];\n\n");
	}

	private void printProcess( AutoTypeImage imageType , ConnectRule rule ) {

		String imageName = imageType.getSingleBandName();
		String sumType = imageType.getSumType();
		String bitwise = imageType.getBitWise();

		int startX = rule == ConnectRule.FOUR ? 0 : 1;

		String weightString = rule == ConnectRule.EIGHT ? ",weight3=0,weight4=0" : "";

		out.print("\t@Override\n" +
				"\tpublic void process(Planar<"+imageName+"> input, DogArray<Edge> edges) {\n" +
				"\t\tif( pixelColor.length != input.getNumBands() ) {\n" +
				"\t\t\tpixelColor = new "+sumType+"[ input.getNumBands() ];\n" +
				"\t\t}\n" +
				"\t\tfinal int numBands = pixelColor.length;\n" +
				"\n" +
				"\t\tedges.reset();\n" +
				"\t\tint w = input.width-1;\n" +
				"\t\tint h = input.height-1;\n" +
				"\n" +
				"\t\t// First consider the inner pixels\n" +
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tint indexSrc = input.startIndex + y*input.stride + "+startX+";\n" +
				"\t\t\tint indexDst =                  + y*input.width  + "+startX+";\n" +
				"\n" +
				"\t\t\tfor( int x = "+startX+"; x < w; x++ , indexSrc++ , indexDst++ ) {\n" +
				"\n" +
				"\t\t\t\t"+sumType+" weight1=0,weight2=0"+weightString+";\n" +
				"\n" +
				"\t\t\t\tfor( int i = 0; i < numBands; i++ ) {\n" +
				"\t\t\t\t\t"+imageName+" band = input.getBand(i);\n" +
				"\n" +
				"\t\t\t\t\t"+sumType+" color0 = band.data[indexSrc]"+bitwise+";                       // (x,y)\n" +
				"\t\t\t\t\t"+sumType+" color1 = band.data[indexSrc+1]"+bitwise+";                     // (x+1,y)\n" +
				"\t\t\t\t\t"+sumType+" color2 = band.data[indexSrc+input.stride]"+bitwise+";          // (x,y+1)\n" +
				"\t\t\t\t\t"+sumType+" diff1 = color0-color1;\n" +
				"\t\t\t\t\t"+sumType+" diff2 = color0-color2;\n" +
				"\t\t\t\t\tweight1 += diff1*diff1;\n" +
				"\t\t\t\t\tweight2 += diff2*diff2;\n");
		if( rule == ConnectRule.EIGHT ) {
			out.print(
				"\t\t\t\t\t"+sumType+" color3 = band.data[indexSrc+1+input.stride]"+bitwise+";        // (x+1,y+1)\n" +
				"\t\t\t\t\t"+sumType+" color4 = band.data[indexSrc-1+input.stride]"+bitwise+";        // (x-1,y+1)\n" +
				"\t\t\t\t\t"+sumType+" diff3 = color0-color3;\n" +
				"\t\t\t\t\t"+sumType+" diff4 = color0-color4;\n" +
				"\t\t\t\t\tweight3 += diff3*diff3;\n" +
				"\t\t\t\t\tweight4 += diff4*diff4;\n");
		}

		out.print(
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\tEdge e1 = edges.grow();\n" +
				"\t\t\t\tEdge e2 = edges.grow();\n" +
				"\n" +
				"\t\t\t\te1.sortValue = (float)Math.sqrt(weight1);\n" +
				"\t\t\t\te1.indexA = indexDst;\n" +
				"\t\t\t\te1.indexB = indexDst+1;\n" +
				"\n" +
				"\t\t\t\te2.sortValue = (float)Math.sqrt(weight2);\n" +
				"\t\t\t\te2.indexA = indexDst;\n" +
				"\t\t\t\te2.indexB = indexDst+input.width;\n" +
				"\n");
		if( rule == ConnectRule.EIGHT ) {
			out.print(
				"\t\t\t\tEdge e3 = edges.grow();\n" +
				"\t\t\t\tEdge e4 = edges.grow();\n" +
				"\n" +
				"\t\t\t\te3.sortValue = (float)Math.sqrt(weight3);\n" +
				"\t\t\t\te3.indexA = indexDst;\n" +
				"\t\t\t\te3.indexB = indexDst+1+input.width;\n" +
				"\n" +
				"\t\t\t\te4.sortValue = (float)Math.sqrt(weight4);\n" +
				"\t\t\t\te4.indexA = indexDst;\n" +
				"\t\t\t\te4.indexB = indexDst-1+input.width;\n");
		}
		out.print(
				"\t\t\t}\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// Handle border pixels\n");

		if( rule == ConnectRule.EIGHT ) {
			out.print(
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tcheckAround(0,y,input,edges);\n" +
				"\t\t\tcheckAround(w,y,input,edges);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor( int x = 0; x < w; x++ ) {\n" +
				"\t\t\tcheckAround(x,h,input,edges);\n" +
				"\t\t}\n" +
				"\t}\n\n");
		} else {
			out.print(
				"\t\tfor( int y = 0; y < h; y++ ) {\n" +
				"\t\t\tcheckAround(w,y,input,edges);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tfor( int x = 0; x < w; x++ ) {\n" +
				"\t\t\tcheckAround(x,h,input,edges);\n" +
				"\t\t}\n" +
				"\t}\n\n");
		}
	}

	private void printCheckAround( AutoTypeImage imageType , ConnectRule rule ) {

		String imageName = imageType.getSingleBandName();
		String bitwise = imageType.getBitWise();

		out.print("\tprivate void checkAround( int x , int y ,\n" +
				"\t\t\t\t\t\t\t  Planar<"+imageName+"> input ,\n" +
				"\t\t\t\t\t\t\t  DogArray<Edge> edges )\n" +
				"\t{\n" +
				"\t\tint indexSrc = input.startIndex + y*input.stride + x;\n" +
				"\t\tint indexA =                      y*input.width  + x;\n" +
				"\n" +
				"\t\tfinal int numBands = pixelColor.length;\n" +
				"\n" +
				"\t\tfor( int i = 0; i < numBands; i++ ) {\n" +
				"\t\t\t"+imageName+" band = input.getBand(i);\n" +
				"\t\t\tpixelColor[i] =  band.data[indexSrc]"+bitwise+";\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tcheck(x+1, y, pixelColor,indexA,input,edges);\n" +
				"\t\tcheck(x  ,y+1,pixelColor,indexA,input,edges);\n");
		if( rule == ConnectRule.EIGHT ) {
			out.print(
				"\t\tcheck(x+1,y+1,pixelColor,indexA,input,edges);\n" +
				"\t\tcheck(x-1,y+1,pixelColor,indexA,input,edges);\n");
		}
		out.print("\t}\n\n");
	}

	private void printCheck( AutoTypeImage imageType ) {

		String imageName = imageType.getSingleBandName();
		String sumType = imageType.getSumType();
		String bitwise = imageType.getBitWise();

		out.print("\tprivate void check( int x , int y , "+sumType+" color0[] , int indexA,\n" +
				"\t\t\t\t\t\tPlanar<"+imageName+"> input ,\n" +
				"\t\t\t\t\t\tDogArray<Edge> edges ) {\n" +
				"\t\tif( !input.isInBounds(x,y) )\n" +
				"\t\t\treturn;\n" +
				"\n" +
				"\t\tint indexSrc = input.startIndex + y*input.stride + x;\n" +
				"\t\tint indexB   =                  + y*input.width  + x;\n" +
				"\n" +
				"\t\tfloat weight = 0;\n" +
				"\n" +
				"\t\tfinal int numBands = pixelColor.length;\n" +
				"\n" +
				"\t\tfor( int i = 0; i < numBands; i++ ) {\n" +
				"\t\t\t"+imageName+" band = input.getBand(i);\n" +
				"\n" +
				"\t\t\t"+sumType+" color = band.data[indexSrc]"+bitwise+";\n" +
				"\t\t\t"+sumType+" diff = color0[i]-color;\n" +
				"\t\t\tweight += diff*diff;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tEdge e1 = edges.grow();\n" +
				"\n" +
				"\t\te1.sortValue = (float)Math.sqrt(weight);\n" +
				"\t\te1.indexA = indexA;\n" +
				"\t\te1.indexB = indexB;\n" +
				"\t}\n\n");
	}

	private void printType( AutoTypeImage imageType ) {
		String imageName = imageType.getSingleBandName();

		out.print("\t@Override\n" +
				"\tpublic ImageType<Planar<"+imageName+">> getInputType() {\n" +
				"\t\treturn ImageType.pl(3,"+imageName+".class);\n" +
				"\t}\n\n");
	}

	public static void main(String[] args) throws FileNotFoundException {
		GenerateFhEdgeWeights_PL generator = new GenerateFhEdgeWeights_PL();
		generator.generateCode();
	}
}
