import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

/**
 * 
 * @author Adrian McMenamin
 *
 */
class SecondPassHandler extends DefaultHandler {

	def verb
	def oFile
	def width
	def height
	def inst
	def writer
	def svg
	Long xFact
	Long yFact
	Long max
	Long min
	Long instTrack
	def percentile
	def factor = 1
	def instMap = [:]
	def loadMap = [:]
	def storeMap = [:]
	def heapMap = [:]
	def instRange
	def range
	def travel = 0
	def pageSize = 0
	def boostSize //margins around the graph
	def gridMarks = 4
	def biggest
	FirstPassHandler fPH

	/**
	 * Calculate and draw the memory reference map
	 * 
	 * @param verb verbose output
	 * @param handler contains basic information about lackeyml file
	 * @param width width of graph in pixels
	 * @param height height of graph in pixels
	 * @param inst plot instructions
	 * @param oFile path to output file
	 * @param percentile percentile to begin plot with
	 * @param range range of percentiles to plot
	 * @param pageSize page size to use
	 * @param gridMarks grid lines to draw
	 * @param boost margin size in pixels
	 */
	SecondPassHandler(def verb, def handler, def width, def height,
	def inst, def oFile, def percentile, def range, def pageSize,
	def gridMarks, def boost) {
		super()
		this.verb = verb
		fPH = handler
		this.width = width
		this.height = height
		this.inst = inst
		this.oFile = oFile
		this.percentile = percentile
		this.pageSize = pageSize
		this.range = range
		this.gridMarks = gridMarks
		boostSize = boost

		writer = new FileWriter(oFile)
		svg = new MarkupBuilder(writer)

		min = fPH.minHeapAddr
		max = fPH.maxHeapAddr;
		if (inst) {
			if (fPH.minInstructionAddr < min)
				min = fPH.minInstructionAddr
			if (fPH.maxInstructionAddr > max)
				max = fPH.maxInstructionAddr
		}
		if (pageSize) {
			max = max >> pageSize
			min = min >> pageSize
		}
		Long memRange = max - min
		instRange = fPH.totalInstructions
		yFact = (int)(memRange/height)
		if (percentile) {
			factor = 100/range
			yFact = (int) yFact/factor
		}
		xFact = (int)(instRange/width)
		instTrack = 0;
	}

	/**
	 * Write out SVG header and draw axes and grid
	 */
	void startDocument() {
		if (verb) println "Writing SVG header"
		def cWidth = width + 2 * boostSize
		def cHeight = height + 2 * boostSize
		writer.write(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
		writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" ")
		writer.write("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")
		writer.write(
				"<svg width=\"${cWidth}px\" height=\"${cHeight}px\" version=\"1.1\" ")
		writer.write("xmlns=\"http://www.w3.org/2000/svg\">\n")
		svg.rect(x:0, y:0, width:cWidth, height:cHeight, fill:"white"){}
		def rPer = percentile - 1
		def rangeStr
		if (pageSize) {
			rangeStr = "Page size: ${2**pageSize}"
			if (percentile)
				rangeStr += ": $rPer to ${rPer + range}% memory"
		} else {
			rangeStr = "From $rPer - ${rPer + range}"
			if (rPer == 0)
				rangeStr = "From $min to $max"
		}
		svg.text(x:boostSize, y:cHeight,
			style: "font-family: Helvetica; font-size: 10; fill: black",
			rangeStr){}

		if (verb) println "Drawing axes"
		svg.line(x1:boostSize - 10, y1:5 + cHeight - boostSize,
				x2:cWidth - boostSize + 5, y2:5 + cHeight - boostSize,
				stroke:"black", "stroke-width":10){}
		svg.line(x1:boostSize - 5, y1:5 + cHeight - boostSize,
				x2:boostSize - 5, y2:boostSize, stroke:"black", "stroke-width":10){}

		(0 .. gridMarks).each { i ->
			svg.line(x1:(int)(boostSize + width * i/gridMarks),
					y1:15 + cHeight - boostSize,
					x2:(int)(boostSize + width * i/gridMarks),
					y2: boostSize,
					stroke:"lightgrey", "stroke-width":1){}
			svg.text(x:(int)(boostSize - 5 + width * i/gridMarks),
					y:20 + cHeight - boostSize,
					style: "font-family: Helvetica; font-size:10; fill: maroon",
					((int) instRange * i / gridMarks))
			svg.line(x1:boostSize - 20,
					y1:(int)(height * i/gridMarks + boostSize),
					x2:cWidth - boostSize,
					y2:(int)(height * i/gridMarks + boostSize),
					stroke:"lightgrey", "stroke-width":1){}
			Long memRange = max - min
			if (percentile){
				def nMin = min + memRange * ((percentile - 1) / 100)
				def nMax = nMin + memRange * (range / 100)
				def nRange = nMax - nMin;
				svg.text(x:boostSize - 70,
						y: (int)(5 + height * i/gridMarks + boostSize),
						style: "font-family: Helvetica; font-size:10; fill: maroon",
						Long.toString((Long)(nMax - nRange * (i/gridMarks)), 16))
			}
			else 
				svg.text(x:boostSize - 70,
						y: (int)(5 + height * i/gridMarks + boostSize),
						style: "font-family: Helvetica; font-size:10; fill: maroon",
						Long.toString((Long)(max - memRange * (i/gridMarks)), 16))
		}
		def memString = "PAGES"
		if (!pageSize)
			memString = "MEMORY"
		svg.text(x:boostSize/4, y: height / 2,
				transform:"rotate(90, ${boostSize/4}, ${height/2})",
				style: "font-family: Helvetica; font-size:10; fill:red",
				memString)

		def instPerPixel = (int)instRange/width
		svg.text(x:boostSize, y: height + boostSize * 1.5,
				style: "font-family:Helvetica; font-size:10; fill:red",
				"INSTRUCTIONS ($instPerPixel per pixel)")
	}

	/**
	 * Calculations are complete, so plot points
	 */
	void endDocument() {
		println "]"
		println "Mapping complete, now drawing points"
		if (!percentile) {
			if (inst) {
				instMap.each{ k, v ->
					svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
							fill:"none", stroke:"red", "stroke-width":1){}
				}
			}
			storeMap.each { k, v ->
				svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
						fill:"none", stroke:"yellow", "stroke-width":1){}
			}
			loadMap.each { k, v ->
				svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
						fill:"none", stroke:"blue", "stroke-width":1){}
			}
			heapMap.each { k, v ->
				svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
						fill:"none", stroke:"green", "stroke-width":1){}
			}
		} else {
			Long miny = height * factor * ((100 -(percentile + range - 1))/100)
			Long maxy = miny + height;
			if (inst) {
				instMap.each{ k, v ->
					if (k[1] in miny .. maxy) {
						def replot = k[1] - miny + boostSize
						svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
								fill:"none", stroke:"red", "stroke-width":1){}
					}
				}
			}
			storeMap.each { k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny + boostSize
					svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
							fill:"none", stroke:"yellow", "stroke-width":1){}
				}
			}
			loadMap.each { k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny + boostSize
					svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
							fill:"none", stroke:"blue", "stroke-width":1){}
				}
			}
			heapMap.each { k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny + boostSize
					svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
							fill:"none", stroke:"green", "stroke-width":1){}
				}
			}
		}
		writer.write("\n</svg>")
		writer.close()
	}

	/**
	 * SAX startElement - calculate pages accessed
	 */
	void startElement(String ns, String localName, String qName,
	Attributes attrs) {
		switch(qName) {

			case 'lackeyml':
				if (verb)
					println "Beginning plot"
				print "["
				break

			case 'application':
				def command = attrs.getValue('command')
				def yDraw = (int) boostSize - 10
				svg.text(x:width, y: yDraw,
						style:"font-family:Helvetica; font-size:10; fill: black",
						"$command"){}
				def yInt = (int) height/20
				yDraw += yInt
				def margin = width + boostSize + 10
				if (inst) {
					svg.rect(x:margin, y: yDraw, width:5, height:5, fill:"red",
							stroke:"black", "stroke-width":1)
					svg.text(x:margin + 10, y:yDraw + 5,
							style:"font-family:Helvetica; font-size:10; fill:black",
							"Instructions")
				}
				yDraw += yInt
				svg.rect(x:margin, y: yDraw, width:5, height:5, fill:"green",
						stroke:"black", "stroke-width":1)
				svg.text(x:margin + 10, y:yDraw + 5,
						style:"font-family:Helvetica; font-size:10; fill:black",
						"Modify")
				yDraw += yInt
				svg.rect(x:margin, y: yDraw, width:5, height:5, fill:"blue",
						stroke:"black", "stroke-width":1)
				svg.text(x:margin + 10, y:yDraw + 5,
						style:"font-family:Helvetica; font-size:10; fill:black",
						"Load")
				yDraw += yInt
				svg.rect(x:margin, y: yDraw, width:5, height:5, fill:"yellow",
						stroke:"black", "stroke-width":1)
				svg.text(x:margin + 10, y:yDraw + 5,
						style:"font-family:Helvetica; font-size:10; fill:black",
						"Store")
				break

			case 'instruction':
				def siz = Long.decode(attrs.getValue('size'))
				instTrack += siz

	/*		FIX ME	if (instTrack > ((int)(instRange * travel)/40)){
					print ">"
					travel++
				} */  
				if (inst) {
					def address = Long.decode(
							attrs.getValue('address')) >> pageSize
					def xPoint = (int)(instTrack/xFact)
					def yPoint = (int)(height * factor - (address - min)/yFact)
					instMap[[xPoint, yPoint]] = true
				}
				break

			case 'store':
				def address = Long.decode(attrs.getValue('address')) >> pageSize
				def xPoint = (int)(instTrack/xFact)
				def yPoint = (int)(height * factor - (address - min)/yFact)
				storeMap[[xPoint, yPoint]] = true
				if (yPoint > biggest) biggest = yPoint
				break

			case 'load':
				def address = Long.decode(attrs.getValue('address')) >> pageSize
				def xPoint = (int)(instTrack/xFact)
				def yPoint = (int)(height * factor - (address - min)/yFact)
				loadMap[[xPoint, yPoint]] = true
				break

			case 'modify':
				def address = Long.decode(attrs.getValue('address')) >> pageSize
				def xPoint = (int)(instTrack/xFact)
				def yPoint = (int)(height * factor - (address - min)/yFact)
				heapMap[[xPoint, yPoint]] = true
				break
		}
	}
}
