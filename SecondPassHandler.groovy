import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

class SecondPassHandler extends DefaultHandler {

	def verb
	def oFile
	def width
	def height
	def inst
	def writer
	def svg
	def xFact
	def yFact
	def max
	def min
	def instTrack
	def instMap = [:]
	def heapMap = [:]
	FirstPassHandler fPH
	
	SecondPassHandler(def verb, def handler, def width, def height,
		def inst, def oFile)
	{
		super()
		this.verb = verb
		fPH = handler
		this.width = width
		this.height = height
		this.inst = inst
		this.oFile = oFile
		
		writer = new FileWriter(oFile)
		svg = new MarkupBuilder(writer)
		
		if (inst) {
			min = (fPH.minInstructionAddr < fPH.minHeapAddr)?
				fPH.minInstructionAddr:fPH.minHeapAddr
			max = (fPH.maxInstructionAddr > fPH.maxHeapAddr)?
				fPH.maxInstructionAddr:fPH.maxHeapAddr
		}
		else {
			min = fPH.minHeapAddr
			max = fPH.maxHeapAddr
		}
		def memRange = max - min
		
		def instRange = fPH.totalInstructions
		yFact = (int)(memRange/height)
		xFact = (int)(instRange/width)
		instTrack = 0
	}
	
	void startDocument() {
		if (verb) println "Writing SVG header"
		writer.write(
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
		writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" ")
		writer.write("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")
		writer.write(
			"<svg width=\"$width\" height=\"$height\" version=\"1.1\" ")
		writer.write("xmlns=\"http://www.w3.org/2000/svg\">\n")
		
		if (verb) println "Drawing axes"
		svg.line(x1:0, y1:0, x2:width, y2:0,
			style:'stroke:RGB(0,0,0);stroke-width:2'){}
		svg.line(x1:0, y1:0, x2:0, y2:height,
			style:'stroke:RGB(0,0,0);stroke-width:2'){}
	}
	
	void endDocument()
	{
		println "Mapping complete, now drawing points"
		if (instr) {
			instMap.each{k, v ->
				svg.rect(x:k[0], y:k[1], width:1, height:1,
					style:"stroke-width=1;stroke:rgb(255,0,0)"){}
			}
		}
		heapMap.each {k, v ->
			svg.rect(x:k[0], y:k[1], width:1, height:1,
				style:"stroke-width=1;stroke:rgb(0,0,255)"){}
		}
		writer.write("</svg>")
	}
	
	void startElement(String ns, String localName, String qName, 
		Attributes attrs)
	{
		switch(qName) {
			
			case 'lackeyml':
			if (verb)
				println "Beginning plot"
			break
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instTrack += siz
			if (!inst)
				break
			def address = Long.decode(attrs.getValue('address'))
			def xPoint = (int)(instTrack/xFact)
			def yPoint = (int)((address - min)/yFact)
			instMap[[xPoint, yPoint]] = true
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = Long.decode(attrs.getValue('address'))
			def xPoint = (int)(instTrack/xFact)
			def yPoint = (int)((address - min)/yFact)
			heapMap[[xPoint, yPoint]] = true
			break	
		}
	}
}
