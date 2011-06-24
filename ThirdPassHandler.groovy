import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

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
	def boostSize = 100
	def wsPoints = []
	def maxWS = 0
	def svg
	def writer

	ThirdPassHandler(def verb, def fPHandler, def wSetInst,
		def width, def height)
	{
		super()
		this.verb = verb
		this.fPHandler = fPHandler
		this.wSetInst = wSetInst
		this.width = width
		this.height = height
		
		//adjust instruct set size if needed
		minInst = fPH.minInstructionAddr
		maxInst = fPH.maxInstructionAddr
		range = maxInst - minInst
		if (wSetInst < range/width) {
			println
				"Instruction range too small, resetting to ${(int)range/width}"
			this.wSetInst = (int) range/width
		}
		pixelUpdateRange = range/width
		def oFile = "WS${new Date().time.toString()}.svg"
		println ("Plotting $oFile")
		writer = new FileWriter (oFile)
		svg = new MarkupBuilder(writer)
		
	}
	
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
	
	void addWSPoint()
	{
		def wsSize = mapWS.size()
		if (wsSize > maxWS)
			maxWS = wsSize
		wsPoints << wsSize
		mapWS = mapWS.findAll{
			it.value > instCount - wSetInst
		}
		instLast = 0
	}
	
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		switch (qName) {
			
			case 'lackeyml':
			print "["
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
			if (instLast > pixelUpdateRange)
				print "x"
				addWSPoint()
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = Long.decode(attrs.getValue('address')) >> pageShift
			mapWS[address] = instLast
			break
		}
	}
		
	void endDocument()
	{
		wsPoints.eachWithIndex {val, i ->
			svg.circle(cx:i + boostSize, cy:val + boostSize, r:1,
				fill:"none", stroke:"black", "stroke-width":1)
		}
		writer.write("\n<svg>")
	}
}
