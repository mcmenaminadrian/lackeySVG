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
	def decile
	def instMap = [:]
	def heapMap = [:]
	def travel = 0
	FirstPassHandler fPH
	
	SecondPassHandler(def verb, def handler, def width, def height,
		def inst, def oFile, def decile)
	{
		super()
		this.verb = verb
		fPH = handler
		this.width = width
		this.height = height
		this.inst = inst
		this.oFile = oFile
		this.decile = decile
		
		writer = new FileWriter(oFile)
		svg = new MarkupBuilder(writer)
		
		min = fPH.minHeapAddr
		max = fPH.maxHeapAddr
		if (inst) {
			if (fPH.minInstructionAddr < min)
				min = fPH.minInstructionAddr
			if (fPH.maxInstructionAddr > max)
				max = fPH.maxInstructionAddr
		}
		def memRange = max - min
		if (decile != 0) {
			def decRange = (int) memRange/10
			def decMin = min + decRange * (decile - 1)
			def decMax = decMin + decRange
			min = decMin
			max = decMax
			memRange = decRange
		}
		
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
			"<svg width=\"${width + 1}px\" height=\"${height + 1}px\" version=\"1.1\" ")
		writer.write("xmlns=\"http://www.w3.org/2000/svg\">\n")
		
		if (verb) println "Drawing axes"
		svg.line(x1:0, y1:height, x2:width, y2:height,
			stroke:"yellow", "stroke-width":5){}
		svg.line(x1:0, y1:height, x2:0, y2:0,
			stroke:"yellow", "stroke-width":5){}
	}
	
	void endDocument()
	{
		println "]"
		println "Mapping complete, now drawing points"
		if (inst) {
			instMap.each{k, v ->
				svg.circle(cx:k[0], cy:k[1], r:1,
					fill:"none", stroke:"red", "stroke-width":1){}
			}
		}
		heapMap.each {k, v ->
			svg.circle(cx:k[0], cy:k[1], r:1,
				fill:"none", stroke:"green", "stroke-width":1){}
		}
		writer.write("\n</svg>")
		writer.close()
	}
	
	void startElement(String ns, String localName, String qName, 
		Attributes attrs)
	{
		switch(qName) {
			
			case 'lackeyml':
			if (verb)
				println "Beginning plot"
			print "["
			break
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instTrack += siz
			if (((int) instTrack/40) > travel){
				print ">"
				travel++
			}
			if (inst) {
				def address = Long.decode(attrs.getValue('address'))
				if (address in min .. max) {
					def xPoint = (int)(instTrack/xFact)
					def yPoint = height - (int)((address - min)/yFact)
					instMap[[xPoint, yPoint]] = true
				}
			}
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = Long.decode(attrs.getValue('address'))
			if (address in min .. max) {
				def xPoint = (int)(instTrack/xFact)
				def yPoint = height - (int)((address - min)/yFact)
				heapMap[[xPoint, yPoint]] = true
			}
			break	
		}
	}
}
