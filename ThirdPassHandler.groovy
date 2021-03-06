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
class ThirdPassHandler extends DefaultHandler {
	
	def verb
	def fPHandler
	def wSetInst
	def width
	def height
	def pixelUpdateRange
	def mapWS = [:]
	def instCount = 0
	def instLast = 0
	def pageShift = 12
	def boostSize
	def wsPoints = []
	def maxWS = 0
	def svg
	def writer
	def gridMarks
	def range

	/**
	 * Calculate and plot the working set size as a function of the
	 * number of instructions executed 
	 * @param verb verbose output
	 * @param fPHandler holds basic data about lackeyml file
	 * @param wSetInst maximum number of instructions to hold page in WS
	 * @param width width of graph in pixels
	 * @param height height of graph in pixels
	 * @param gridMarks grid lines to be drawn
	 * @param boost width of margins in pixels
	 * @param pShift page shift in powers of 2
	 */
	ThirdPassHandler(def verb, def fPHandler, def wSetInst,
		def width, def height, def gridMarks, def boost, def pShift)
	{
		super()
		this.verb = verb
		this.fPHandler = fPHandler
		this.wSetInst = wSetInst
		this.width = width
		this.height = height
		this.gridMarks = gridMarks
		boostSize = boost
		this.pageShift = pShift
		
		//adjust instruct set size if needed
		range = fPHandler.totalInstructions
		if (wSetInst < range/width) {
			println(
				"Instruction set too small, resetting to ${(int)range/width}")
			this.wSetInst = (int) range/width
		}
		pixelUpdateRange = range/width
		def oFile = "WS${new Date().time.toString()}.svg"
		println ("Plotting $oFile")
		writer = new FileWriter (oFile)
		svg = new MarkupBuilder(writer)
		
	}
	
	/**
	 * SAX startDocument - write out SVG header
	 */
	void startDocument()
	{
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
	}
	
	/**
	 * Add and remove pages from Map of working set
	 * @return updated Map of working set
	 */
	Map addWSPoint()
	{
		def wsSize = mapWS.size()
		if (wsSize > maxWS)
			maxWS = wsSize
		wsPoints << wsSize
		instLast = 0
		return mapWS.findAll{
			it.value > instCount - wSetInst
		}
	}
	
	/**
	 * SAX startElement - process lackeyml file
	 */
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		switch (qName) {
			
			case 'lackeyml':
			println "Beginning working set plotting"
			break
			
			case 'application':
			def command = attrs.getValue('command')
			def yDraw = (int) boostSize - 10
			svg.text(x:width, y: yDraw,
					style:"font-family:Helvetica; font-size:10; fill: black",
					"$command"){}
			break
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instCount += siz
			instLast += siz
			def address = Long.decode(attrs.getValue('address')) >> pageShift
			mapWS[address] = instCount
			if (instLast >= pixelUpdateRange) 
				mapWS = addWSPoint()
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = Long.decode(attrs.getValue('address')) >> pageShift
			mapWS[address] = instLast
			break
		}
	}
		
	/**
	 * calculations complete - now plot graph
	 */
	void endDocument()
	{
		println "Drawing working set graph"
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
				((int) range * i / gridMarks))
			
			svg.line(x1:boostSize - 20,
				y1:(int)(height * i/gridMarks + boostSize),
				x2:width + boostSize,
				y2:(int)(height * i/gridMarks + boostSize),
				stroke:"lightgrey", "stroke-width":1){}
	
			svg.text(x:boostSize - 60,
				y: (int)(5 + height * i/gridMarks + boostSize),
				style: "font-family: Helvetica; font-size:10; fill: maroon",
				(Long.toString((int)(maxWS - maxWS * i/gridMarks), 10)))
		}
		def lastX = boostSize
		def lastY = boostSize + height
		wsPoints.eachWithIndex {val, i ->
			def yFact = height/maxWS
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
			"Pages in working set")
		def strInst = "Instructions: ${(int)range/width} per pixel. Working "
		strInst += "set memory: accessed in last $wSetInst instructions."
		svg.text(x:boostSize, y: height + boostSize * 1.5,
				style: "font-family:Helvetica; font-size:10; fill:red",
				strInst)
		writer.write("\n</svg>")
		writer.close()
	}
}
