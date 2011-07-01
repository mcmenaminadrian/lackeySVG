import groovy.xml.MarkupBuilder

class GraphTheta {

	GraphTheta(def thetaMap, def width, def height, def totalInst, def gridMarks, def boostSize)
	{
		println "Drawing lifetime function"
		thetaMap.sort()
		def gs = thetaMap.keySet()
		def thetas = thetaMap.values()
		def maxG = gs.max()
		def minG = gs.min()
		def maxT = thetas.max()
		def minT = thetas.min()
		def rangeT = maxT - minT 
		def rangeG = maxG - minG
		def writer = new FileWriter ("THETA${new Date().time.toString()}.svg")
		def svg = new MarkupBuilder(writer)
		//header etc
		def cWidth = width + 2 * boostSize
		def cHeight = height + 2 * boostSize
		writer.write(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
		writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" ")
		writer.write("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")
		writer.write(
				"<svg width=\"${cWidth}px\" height=\"${cHeight}px\" version=\"1.1\" ")
		writer.write("xmlns=\"http://www.w3.org/2000/svg\">\n")
		//draw axes
		svg.line(x1:boostSize - 5, y1:height + boostSize + 5,
			x2: boostSize + width, y2:height + boostSize + 5,
			stroke:"black", "stroke-width":10 )
		svg.line(x1:boostSize - 5, y1: height + boostSize + 5,
			x2: boostSize - 5, y2: boostSize,
			stroke:"black", "stroke-width":10)
		(0 .. gridMarks).each { i ->
			svg.line(x1:(int)(boostSize + width * i/gridMarks),
				y1:15 + height + boostSize,
				x2:(int)(boostSize + width * i/gridMarks),
				y2: boostSize,
				stroke:"lightgrey", "stroke-width":1){}
			
			svg.text(x:(int)(boostSize - 5 + width * i/gridMarks),
				y:20 + height + boostSize,
				style: "font-family: Helvetica; font-size:10; fill: maroon",
				((int) rangeT * i / gridMarks + minT))
			
			svg.line(x1:boostSize - 20,
				y1:(int)(height * i/gridMarks + boostSize),
				x2:width + boostSize,
				y2:(int)(height * i/gridMarks + boostSize),
				stroke:"lightgrey", "stroke-width":1){}
	
			svg.text(x:boostSize - 60,
				y: (int)(5 + height * i/gridMarks + boostSize),
				style: "font-family: Helvetica; font-size:10; fill: maroon",
				(Long.toString((int)(rangeG * i/gridMarks + minG), 10)))
		}
		
		
		def lastX = boostSize
		def lastY = boostSize + height
		def yFact = height/maxG
		thetaMap.eachWithIndex {key, val, i ->
			def yPoint = boostSize + (int) (height - val * yFact)
			svg.line(x1: lastX, y1: lastY,
				x2:i + 1 + boostSize, y2:yPoint,
				style:"fill:none; stroke:red; stroke-width:1;")
			lastX = i + 1 + boostSize
			lastY = yPoint;
		}
		svg.text(x:boostSize/4, y: height / 2,
			transform:"rotate(270, ${boostSize/4}, ${height/2})",
			style: "font-family: Helvetica; font-size:10; fill:red",
			"Instructions between faults")
		def strInst = "Working set: Size measured in maximum age "
		strInst += " as measured in instructions ."
		svg.text(x:boostSize, y: height + boostSize * 1.5,
				style: "font-family:Helvetica; font-size:10; fill:red",
				strInst)
		writer.write("\n</svg>")
		writer.close()
	
	} 
}
