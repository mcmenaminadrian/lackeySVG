
import groovy.xml.MarkupBuilder

/**
 * 
 * @author Adrian McMenamin
 *
 */
class GraphCompTheta {

	/**
	 * Graph the lifetime functions of both the working set and
	 * LRU methods of page caching
	 * @param thetaAveMap data about the working set cache
	 * @param thetaLRUAveMap data about the LRU cache
	 * @param width width in pixels of graph
	 * @param height height in pixels of graph
	 * @param gridMarks number of grid lines
	 * @param boostSize size of margin in pixels
	 */
	GraphCompTheta(def thetaAveMap, def thetaLRUAveMap, 
		def width, def height, def gridMarks, def boostSize)
	{
		println "Drawing lifetime function"
		def thetas
		def gs
		def maxG
		def maxT
		def rangeT
		def rangeG
		def maxWSS = thetaAveMap.values().max()
		def maxLRU = thetaLRUAveMap.values().max()
		if (maxWSS > maxLRU) {
			thetas = thetaAveMap.keySet()
			gs = thetaAveMap.values()
		} else {
			thetas = thetaLRUAveMap.keySet()
			gs = thetaLRUAveMap.values()
		}
		maxG = gs.max()
		maxT = thetas.max()
		rangeT = maxT
		rangeG = maxG
		def writer = new FileWriter ("CMPTHETA${new Date().time.toString()}.svg")
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
				((int) rangeT * i / gridMarks))

			svg.line(x1:boostSize - 20,
				y1:(int)(height * i/gridMarks + boostSize),
				x2:width + boostSize,
				y2:(int)(height * i/gridMarks + boostSize),
				stroke:"lightgrey", "stroke-width":1){}
	
			svg.text(x:boostSize - 60,
				y: (int)(5 + height * i/gridMarks + boostSize),
				style: "font-family: Helvetica; font-size:10; fill: maroon",
				(Long.toString((int)(maxG - rangeG * i/gridMarks), 10)))
		}
		
		def yFact = height/rangeG
		def xFact = width/rangeT
		def lastX = boostSize
		//initial distance between faults is 1
		def lastY = boostSize + (height - yFact)
		thetaAveMap.each{key, val ->
			def yPoint = boostSize + (int) (height - val * yFact)
			svg.line(x1: lastX, y1: lastY,
				x2:(int)(key * xFact) + 1 + boostSize, y2:yPoint,
				style:"fill:none; stroke:red; stroke-width:1;")
			lastX = (int)(key * xFact + 1 + boostSize)
			lastY = yPoint;
		}
		
		lastX = boostSize
		lastY = boostSize + (height - yFact)
		thetas = thetaLRUAveMap.keySet()
		thetaLRUAveMap.each{key, val ->
			def yPoint = boostSize + (int) (height - val * yFact)
			svg.line(x1: lastX, y1: lastY,
				x2:(int)(key * xFact) + 1 + boostSize, y2:yPoint,
				style:"fill:none; stroke:blue; stroke-width:1;")
			lastX = (int)(key * xFact + 1 + boostSize)
			lastY = yPoint;
		}
		
		svg.text(x:boostSize/4, y: height / 2,
			transform:"rotate(270, ${boostSize/4}, ${height/2})",
			style: "font-family: Helvetica; font-size:10; fill:red",
			"Denning's g(theta)")
		def strInst = "(Average - red) Working set size"
		svg.text(x:boostSize, y: height + boostSize * 1.5,
				style: "font-family:Helvetica; font-size:10; fill:red",
				strInst)
		writer.write("\n</svg>")
		writer.close()
	
	}
}

