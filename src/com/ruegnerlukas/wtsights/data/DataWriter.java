package com.ruegnerlukas.wtsights.data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ruegnerlukas.simplemath.vectors.vec2.Vector2d;
import com.ruegnerlukas.simplemath.vectors.vec2.Vector2i;
import com.ruegnerlukas.simpleutils.logging.logger.Logger;
import com.ruegnerlukas.wtsights.data.calibration.CalibrationAmmoData;
import com.ruegnerlukas.wtsights.data.calibration.CalibrationData;
import com.ruegnerlukas.wtsights.data.sight.BIndicator;
import com.ruegnerlukas.wtsights.data.sight.HIndicator;
import com.ruegnerlukas.wtsights.data.sight.SightData;
import com.ruegnerlukas.wtsights.data.sight.elements.ElementCentralVertLine;
import com.ruegnerlukas.wtsights.data.sight.elements.ElementCustomObject.Movement;
import com.ruegnerlukas.wtsights.data.sight.elements.ElementType;
import com.ruegnerlukas.wtutils.Config;
import com.ruegnerlukas.wtutils.SightUtils.ScaleMode;
import com.ruegnerlukas.wtsights.data.sight.elements.*;

public class DataWriter {

	
	public static boolean saveExternalCalibFile(CalibrationData data, File outputFile) throws Exception {

		if(data == null) {
			Logger.get().warn("Could not find calibration data: " + data);
			return false;
		}
		if(outputFile == null) {
			Logger.get().warn("Could not find file: " + outputFile);
			return false;
		}
		
		
		Logger.get().info("Writing calibration-file to " + outputFile);
		
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document doc = docBuilder.newDocument();
		
		Element rootElement = doc.createElement("ballisticData");
		doc.appendChild(rootElement);
		
		Element elementVehicle = doc.createElement(data.vehicle.name);
		elementVehicle.setAttribute("fovOut", ""+data.vehicle.fovOut);
		elementVehicle.setAttribute("fovIn", ""+data.vehicle.fovIn);
		rootElement.appendChild(elementVehicle);
				
		Element elementAmmoGroup = doc.createElement("ammo");
		elementVehicle.appendChild(elementAmmoGroup);
		
		// ammo data
		for(CalibrationAmmoData ammoData : data.ammoData) {
			
			String ammoName =  ammoData.ammo.name;
			
			Element elementAmmo = doc.createElement(ammoName);
			elementAmmoGroup.appendChild(elementAmmo);
					
			elementAmmo.setAttribute("zoomedIn", ammoData.zoomedIn ? "true" : "false");
			elementAmmo.setAttribute("imageName", ammoData.imgName);
			elementAmmo.setAttribute("markerCenter", ammoData.markerCenter.x + "," + ammoData.markerCenter.y);

			Element elementMarkerRanges = doc.createElement("markerRanges");
			elementAmmo.appendChild(elementMarkerRanges);
			
			int i = 0;
			for(Vector2i m : ammoData.markerRanges) {
				elementMarkerRanges.setAttribute("marker_" + (i++), m.x + ", " + m.y);
			}
			
		}
		
		
		// images
		Element elementImageRoot = doc.createElement("images");
		elementVehicle.appendChild(elementImageRoot);
		
		for(Entry<String,BufferedImage> entry : data.images.entrySet()) {
			String imageName = entry.getKey();
			Element elementImage = doc.createElement(imageName);
			elementImageRoot.appendChild(elementImage);
			String encodedImage = endodeImage(entry.getValue(), "jpg");
			elementImage.setAttribute("encodedData", encodedImage);
		}
		
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(outputFile);
		
		transformer.transform(source, result);
		
		Logger.get().info("External calibration file saved");
		return true;
	}
	
	
	
	
	private static String endodeImage(BufferedImage img, String format) throws IOException {
	
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(img, format, baos);
		baos.flush();
		
		String strImage = Base64.getEncoder().encodeToString(baos.toByteArray());
		baos.close();
		
		return strImage;
	}
	
	
	
	
	
	public static boolean saveSight(SightData data, CalibrationData dataCalib, File outputFile) {
		
		if(data == null) {
			Logger.get().warn("Could not find sight-data: " + data);
			return false;
		}
		if(dataCalib == null) {
			Logger.get().warn("Could not find calibration-data: " + dataCalib);
			return false;
		}
		if(outputFile == null) {
			Logger.get().warn("Could not find file: " + outputFile);
			return false;
		}
		
		
		List<String> lines = new ArrayList<String>();
			
		// metadata
		lines.add("// created with WTSightEdit " + Config.build_date);
		lines.add("// vehicle = " + (dataCalib == null ? "unknown" : dataCalib.vehicle.name));
		lines.add("");
		
		// general
		lines.add("// general");
		lines.add("thousandth:t = \"" + data.gnrThousandth.tag + "\"");
		lines.add("fontSizeMult:r = " + data.gnrFontScale);
		lines.add("lineSizeMult:r = " + data.gnrLineSize);
		lines.add("applyCorrectionToGun:b = " + (data.gnrApplyCorrectionToGun ? "yes" : "no") );
		lines.add("drawCentralLineVert:b = " + (((ElementCentralVertLine)data.getElements(ElementType.CENTRAL_VERT_LINE).get(0)).drawCentralVertLine ? "yes" : "no") );
		lines.add("drawCentralLineHorz:b = " + (((ElementCentralHorzLine)data.getElements(ElementType.CENTRAL_HORZ_LINE).get(0)).drawCentralHorzLine ? "yes" : "no") );
		lines.add("");
		
		// rangefinder
		ElementRangefinder rangefinder = (ElementRangefinder)data.getElements(ElementType.RANGEFINDER).get(0);
		lines.add("// rangefinder");
		lines.add("rangefinderHorizontalOffset:r = " + rangefinder.position.x);
		lines.add("rangefinderVerticalOffset:r = " + rangefinder.position.y);
		lines.add("rangefinderProgressBarColor1:c = " + (int)(rangefinder.color1.getRed()*255) + "," + (int)(rangefinder.color1.getGreen()*255) + "," + (int)(rangefinder.color1.getBlue()*255) + "," + (int)(rangefinder.color1.getOpacity()*255));
		lines.add("rangefinderProgressBarColor2:c = " + (int)(rangefinder.color2.getRed()*255) + "," + (int)(rangefinder.color2.getGreen()*255) + "," + (int)(rangefinder.color2.getBlue()*255) + "," + (int)(rangefinder.color2.getOpacity()*255));
		lines.add("rangefinderTextScale:r = " + rangefinder.textScale);
		lines.add("rangefinderUseThousandth:b = " + (rangefinder.useThousandth ? "yes" : "no") );
		lines.add("");
		
		// horz range indicators
		ElementHorzRangeIndicators horzRange = (ElementHorzRangeIndicators)data.getElements(ElementType.HORZ_RANGE_INDICATORS).get(0);
		lines.add("// horizontal range indicators");
		lines.add("crosshairHorVertSize:p2 = " + horzRange.sizeMajor + "," + horzRange.sizeMinor);
		lines.add("crosshair_hor_ranges {");
		for(int i=0; i<horzRange.indicators.size(); i++) {
			HIndicator indicator = horzRange.indicators.get(i);
			int mil = indicator.getMil();
			int label = indicator.isMajor() ? Math.abs(indicator.getMil()) : 0;
			lines.add("  range:p2 = " + mil + "," + label);	
		}
		lines.add("}");
		lines.add("");
		
		
		// ballistic range indicators
		if(!data.getElements(ElementType.BALLISTIC_RANGE_INDICATORS).isEmpty()) {
			ElementBallRangeIndicator ballRange = (ElementBallRangeIndicator)data.getElements(ElementType.BALLISTIC_RANGE_INDICATORS).get(0);
			lines.add("// ballistic range indicators");
			lines.add("drawUpward:b = " + (ballRange.drawUpward ? "yes" : "no") );
			lines.add("distancePos:p2 = " + ballRange.position.x + "," + ballRange.position.y);
			if(ballRange.move) {
				lines.add("move:b = " + (ballRange.move ? "yes" : "no") );
			}
			if(ballRange.scaleMode == ScaleMode.RADIAL) {
				lines.add("radial:b = " + (ballRange.scaleMode == ScaleMode.RADIAL ? "yes" : "no"));
			}
			if(ballRange.circleMode) {
				lines.add("circleMode:b = " + (ballRange.circleMode ? "yes" : "no"));
			}
			lines.add("crosshairDistHorSizeMain:p2 = " + ballRange.size.x + "," + ballRange.size.y);
			lines.add("textPos:p2 = " + ballRange.textPos.x + "," + ballRange.textPos.y);
			lines.add("textAlign:i = " + ballRange.textAlign.id);
			lines.add("textShift:r = " + ballRange.textShift);
			lines.add("drawAdditionalLines:b = " + (ballRange.drawAddLines ? "yes" : "no") );
			lines.add("crosshairDistHorSizeAdditional:p2 = " + ballRange.sizeAddLine.x + "," + ballRange.sizeAddLine.y);
			if(ballRange.scaleMode == ScaleMode.RADIAL) {
				lines.add("radialStretch:r = " + ballRange.radialStretch);
				lines.add("radialAngle:r = " + ballRange.radialAngle);
				lines.add("radialRadius:p2 = " + ballRange.radialRadius + "," + (ballRange.radiusUseMils ? "1" : "0") );
			}
			lines.add("drawDistanceCorrection:b = " + (ballRange.drawCorrLabel ? "yes" : "no") );
			if(ballRange.drawCorrLabel) {
				lines.add("distanceCorrectionPos:p2 = " + ballRange.posCorrLabel.x + "," + ballRange.posCorrLabel.y);
			}
			lines.add("");
			
			lines.add("crosshair_distances {");
			for(int i=0; i<ballRange.indicators.size(); i++) {
				BIndicator indicator = ballRange.indicators.get(i);
				int dist = indicator.getDistance();
				boolean major = indicator.isMajor();
				int label = major ? Math.abs(dist/100) : 0;
				double extend = indicator.getExtend();
				Vector2d textOff = new Vector2d(indicator.getTextX(), indicator.getTextY());
				lines.add("    distance { distance:p3="+dist + "," + label + "," + extend + "; textPos:p2=" + textOff.x + "," + textOff.y + "; }");
			}
			lines.add("}");
			lines.add("");
		}
		
		
		
		// shell ballistics blocks
		if(!data.getElements(ElementType.SHELL_BALLISTICS_BLOCK).isEmpty()) {
			lines.add("// shell ballistics blocks");
			lines.add("ballistics {");
			
			for(com.ruegnerlukas.wtsights.data.sight.elements.Element element : data.getElements(ElementType.SHELL_BALLISTICS_BLOCK)) {

				ElementShellBlock shellBlock = (ElementShellBlock)element;
				
				lines.add("  //-- " + shellBlock.name + " (" + shellBlock.dataAmmo.ammo.name + ")");
				lines.add("  bullet {");
				
				lines.add("    bulletType:t = \"" + shellBlock.dataAmmo.ammo.type + "\"");
				lines.add("    speed:r = " + shellBlock.dataAmmo.ammo.speed);
				lines.add("    triggerGroup:t = \"" + shellBlock.triggerGroup + "\"");
				lines.add("    thousandth:b = " + "no");
				lines.add("    drawUpward:b = " + (shellBlock.drawUpward ? "yes" : "no") );
				lines.add("    distancePos:p2 = " + shellBlock.position.x + "," + shellBlock.position.y);
				lines.add("    move:b = " + (shellBlock.move ? "yes" : "no") );
				if(shellBlock.scaleMode == ScaleMode.RADIAL) {
					lines.add("    radial:b = " + (shellBlock.scaleMode == ScaleMode.RADIAL ? "yes" : "no") );
				}
				if(shellBlock.circleMode) {
					lines.add("    circleMode:b = " + (shellBlock.circleMode ? "yes" : "no") );
				}
				lines.add("    crosshairDistHorSizeMain:p2 = " + shellBlock.size.x + "," + shellBlock.size.y);
				lines.add("    textPos:p2 = " + shellBlock.textPos.x + "," + shellBlock.textPos.y);
				lines.add("    textAlign:i = " + shellBlock.textAlign.id);
				lines.add("    textShift:r = " + shellBlock.textShift);
				
				lines.add("    drawAdditionalLines:b = " + (shellBlock.drawAddLines ? "yes" : "no") );
				lines.add("    crosshairDistHorSizeAdditional:p2 = " + shellBlock.sizeAddLine.x + "," + shellBlock.sizeAddLine.y);
				
				if(shellBlock.scaleMode == ScaleMode.RADIAL) {
					lines.add("    radialStretch:r = " + shellBlock.radialStretch);
					lines.add("    radialAngle:r = " + shellBlock.radialAngle);
					lines.add("    radialRadius:p2 = " + shellBlock.radialRadius + "," + (shellBlock.radiusUseMils ? "1" : "0" ));
				}
				lines.add("    crosshair_distances {");
				
				for(int i=0; i<shellBlock.indicators.size(); i++) {
					BIndicator indicator = shellBlock.indicators.get(i);
					
					int dist = indicator.getDistance();
					boolean major = indicator.isMajor();
					int label = major ? Math.abs(dist/100) : 0;
					double extend = indicator.getExtend();
					Vector2d textOff = new Vector2d(indicator.getTextX(), indicator.getTextY());
					lines.add("        distance { distance:p3="+dist + "," + label + "," + extend + "; textPos:p2=" + textOff.x + "," + textOff.y + "; }");
				}
				lines.add("    }");
				lines.add("  }");
			}
			
			lines.add("}");
			lines.add("");
		}
		
		
		// custom elements
		
		// lines
		if(!data.getElements(ElementType.CUSTOM_LINE).isEmpty()) {
			lines.add("// lines");
			lines.add("drawLines {");
			for(com.ruegnerlukas.wtsights.data.sight.elements.Element element : data.getElements(ElementType.CUSTOM_LINE)) {
				ElementCustomLine lineObj = (ElementCustomLine)element;
				lines.add("  //-- " + lineObj.name);
				lines.add("  line {");
				lines.add("    thousandth:b = " + (lineObj.useThousandth ? "yes" : "no") );
				if(lineObj.movement == Movement.MOVE) {
					lines.add("    move:b = " + (lineObj.movement == Movement.STATIC ? "no" : "yes") );
				}
				if(lineObj.movement == Movement.MOVE_RADIAL) {
					lines.add("    moveRadial:b = " + (lineObj.movement == Movement.MOVE_RADIAL ? "yes" : "no") );
					lines.add("    radialAngle:r = " + lineObj.angle);
					lines.add("    radialCenter:p2 = " + lineObj.radCenter.x + "," + lineObj.radCenter.y);
					lines.add("    radialMoveSpeed:r = " + lineObj.speed);
					if(!lineObj.autoCenter) {
						lines.add("    center:p2 = " + lineObj.center.x + "," + lineObj.center.y);
					}
				}
				lines.add("    line:p4 = " + lineObj.start.x + "," + lineObj.start.y + ", " + lineObj.end.x + "," + lineObj.end.y);
				lines.add("  }");
			}
			lines.add("}");
		}
		
		// text
		if(!data.getElements(ElementType.CUSTOM_TEXT).isEmpty()) {
			lines.add("// text");
			lines.add("drawTexts {");
			for(com.ruegnerlukas.wtsights.data.sight.elements.Element element : data.getElements(ElementType.CUSTOM_TEXT)) {
				ElementCustomText textObj = (ElementCustomText)element;
				lines.add("  //-- " +  textObj.name);
				lines.add("  text {");
				lines.add("    text:t = " + "\""+textObj.text + "\"");
				lines.add("    thousandth:b = " + (textObj.useThousandth ? "yes" : "no") );
				if(textObj.movement == Movement.MOVE) {
					lines.add("    move:b = " + (textObj.movement == Movement.STATIC ? "no" : "yes") );
				}
				if(textObj.movement == Movement.MOVE_RADIAL) {
					lines.add("    moveRadial:b = " + (textObj.movement == Movement.MOVE_RADIAL ? "yes" : "no") );
					lines.add("    radialAngle:r = " + textObj.angle);
					lines.add("    radialCenter:p2 = " + textObj.radCenter.x + "," + textObj.radCenter.y);
					lines.add("    radialMoveSpeed:r = " + textObj.speed);
					if(!textObj.autoCenter) {
						lines.add("    center:p2 = " + textObj.center.x + "," + textObj.center.y);
					}
				}
				lines.add("    pos:p2 = " + textObj.position.x + "," + textObj.position.y);
				lines.add("    align:i = " + textObj.align.id);
				lines.add("    size:r = " + textObj.size);
				lines.add("  }");
			}
			lines.add("}");
		}
		
		// circles
		if(!data.getElements(ElementType.CUSTOM_CIRCLE).isEmpty()) {
			lines.add("// circles");
			lines.add("drawCircles {");
			for(com.ruegnerlukas.wtsights.data.sight.elements.Element element : data.getElements(ElementType.CUSTOM_CIRCLE)) {
				ElementCustomCircle circleObj = (ElementCustomCircle)element;
				lines.add("  //-- " + circleObj.name);
				lines.add("  circle {");
				lines.add("    thousandth:b = " + (circleObj.useThousandth ? "yes" : "no") );
				if(circleObj.movement == Movement.MOVE) {
					lines.add("    move:b = " + (circleObj.movement == Movement.STATIC ? "no" : "yes") );
				}
				if(circleObj.movement == Movement.MOVE_RADIAL) {
					lines.add("    moveRadial:b = " + (circleObj.movement == Movement.MOVE_RADIAL ? "yes" : "no") );
					lines.add("    radialAngle:r = " + circleObj.angle);
					lines.add("    radialCenter:p2 = " + circleObj.radCenter.x + "," + circleObj.radCenter.y);
					lines.add("    radialMoveSpeed:r = " + circleObj.speed);
					if(!circleObj.autoCenter) {
						lines.add("    center:p2 = " + circleObj.center.x + "," + circleObj.center.y);
					}
				}
				lines.add("    segment:p2 = " + circleObj.segment.x + "," + circleObj.segment.y);
				lines.add("    pos:p2 = " + circleObj.position.x + "," + circleObj.position.y);
				lines.add("    diameter:r = " + circleObj.diameter);
				lines.add("    size:r = " + circleObj.size);
				lines.add("  }");
			}
			lines.add("}");
		}
		
		
		// quads
		if(!data.getElements(ElementType.CUSTOM_QUAD).isEmpty()) {
			lines.add("// quads");
			lines.add("drawQuads {");
			for(com.ruegnerlukas.wtsights.data.sight.elements.Element element : data.getElements(ElementType.CUSTOM_QUAD)) {
				ElementCustomQuad quadObj = (ElementCustomQuad)element;
				lines.add("  //-- " + quadObj.name);
				lines.add("  quad {");
				lines.add("    thousandth:b = " + (quadObj.useThousandth ? "yes" : "no") );
				if(quadObj.movement == Movement.MOVE) {
					lines.add("    move:b = " + (quadObj.movement == Movement.STATIC ? "no" : "yes") );
				}
				if(quadObj.movement == Movement.MOVE_RADIAL) {
					lines.add("    moveRadial:b = " + (quadObj.movement == Movement.MOVE_RADIAL ? "yes" : "no") );
					lines.add("    radialAngle:r = " + quadObj.angle);
					lines.add("    radialCenter:p2 = " + quadObj.radCenter.x + "," + quadObj.radCenter.y);
					lines.add("    radialMoveSpeed:r = " + quadObj.speed);
					if(!quadObj.autoCenter) {
						lines.add("    center:p2 = " + quadObj.center.x + "," + quadObj.center.y);
					}
				}
				lines.add("    tl:p2 = " + quadObj.pos1.x + "," + quadObj.pos1.y);
				lines.add("    tr:p2 = " + quadObj.pos2.x + "," + quadObj.pos2.y);
				lines.add("    br:p2 = " + quadObj.pos3.x + "," + quadObj.pos3.y);
				lines.add("    bl:p2 = " + quadObj.pos4.x + "," + quadObj.pos4.y);
				lines.add("  }");
			}
			lines.add("}");
		}
		
		
		try {
			Files.write(Paths.get(outputFile.getAbsolutePath()), lines, Charset.forName("UTF-8"));
			Logger.get().info("Sight file saved: " + outputFile.getAbsolutePath());
			return true;
		} catch (IOException e) {
			Logger.get().error("An error occured while saving sight");
			Logger.get().error(e);
			return false;
		}
		
		
	}
	
	
	
}
