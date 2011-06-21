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
	FirstPassHandler fPH

	SecondPassHandler(def verb, def handler, def width, def height,
	def inst, def oFile, def percentile, def range, def pageSize) {
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
		if (pageSize) {
			max = max >> pageSize
			min = min >> pageSize
		}
		def memRange = max - min
		instRange = fPH.totalInstructions
		yFact = (int)(memRange/height)
		if (percentile) {
			factor = 100/range
			yFact = (int) yFact/factor
		}
		xFact = (int)(instRange/width)
		instTrack = 0
	}

	void startDocument() {
		if (verb) println "Writing SVG header"
		width = width + 200
		height = height + 100
		writer.write(
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
		writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" ")
		writer.write("\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")
		writer.write(
		"<svg width=\"${width + 1}px\" height=\"${height + 1}px\" version=\"1.1\" ")
		writer.write("xmlns=\"http://www.w3.org/2000/svg\">\n")
		def rPer = percentile - 1
		if (pageSize) {
			svg.text(x:0, y:height,
				style: "font-family: Helvetica; font-size: 10; fill: black") 
			{ 
				"Page size $pageSize, $rPer - ${percentile + range}"
			}
		}
		else {
			svg.text(x:0, y:height,
				style: "font-family: Helvetica; font-size: 10; fill: black")
			{ 
				 "From $rPer - ${rPer + range}" 
			}
		}


		if (verb) println "Drawing axes"
		svg.line(x1:0, y1:height, x2:width, y2:height,
				stroke:"yellow", "stroke-width":5){}
		svg.line(x1:0, y1:height, x2:0, y2:0,
					stroke:"yellow", "stroke-width":5){}
	}

	void endDocument() {
		println "]"
		println "Mapping complete, now drawing points"
		if (!percentile) {
			if (inst) {
				instMap.each{
					k, v ->
					svg.circle(cx:k[0], cy:k[1], r:1,
						fill:"none", stroke:"red", "stroke-width":1){}
				}
			}
			storeMap.each {
				k, v ->
				svg.circle(cx:k[0], cy:k[1], r:1,
					fill:"none", stroke:"pink", "stroke-width":1){}
			}
			loadMap.each {
				k, v ->
				svg.circle(cx:k[0], cy:k[1], r:1,
					fill:"none", stroke:"orange", "stroke-width":1){}
			}
			heapMap.each {
				k, v ->
				svg.circle(cx:k[0], cy:k[1], r:1,
					fill:"none", stroke:"green", "stroke-width":1){}
			}
		} else {
			def miny = height * (factor * (percentile - 1))
			def maxy = miny + height
			if (inst) {
				instMap.each{
					k, v ->
					if (k[1] in miny .. maxy) {
						def replot = k[1] - miny
						svg.circle(cx:k[0], cy:replot, r:1,
							fill:"none", stroke:"red", "stroke-width":1){}
					}
				}
			}
			storeMap.each {
				k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny
					svg.circle(cx:k[0], cy:replot, r:1,
						fill:"none", stroke:"pink", "stroke-width":1){}
					}
				}
			loadMap.each {
				k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny
					svg.circle(cx:k[0], cy:replot, r:1,
						fill:"none", stroke:"orange", "stroke-width":1){}
				}
			}
			heapMap.each {
				k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny
					svg.circle(cx:k[0], cy:replot, r:1,
						fill:"none", stroke:"green", "stroke-width":1){}
				}
			}
		}
		writer.write("\n</svg>")
		writer.close()
	}

	void startElement(String ns, String localName, String qName,
				Attributes attrs) {
		switch(qName) {

			case 'lackeyml':
			if (verb)
				println "Beginning plot"
			print "["
			break

			case 'application':
			svg.text(x:width-100, y: (int)height/10,  
				style:"font-family:Helvetica; font-size:10; fill: black"){
				attrs.getValue('command')
			}
			break
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instTrack += siz

			if (instTrack > ((int)(instRange * travel)/100)){
				print ">"
				travel++
			}
			if (inst) {
				def address = Long.decode(attrs.getValue('address'))
				if (pageSize) address = address >> pageSize
				def xPoint = (int)(instTrack/xFact)
				def yPoint = height - (int)((address - min)/yFact)
				instMap[[xPoint, yPoint]] = true
			}
			break

			case 'store':
			def address = Long.decode(attrs.getValue('address'))
			if (pageSize) address = address >> pageSize
			def xPoint = (int)(instTrack/xFact)
			def yPoint = height - (int)((address - min)/yFact)
			storeMap[[xPoint, yPoint]] = true
			break

			case 'load':
			def address = Long.decode(attrs.getValue('address'))
			if (pageSize) address = address >> pageSize
			def xPoint = (int)(instTrack/xFact)
			def yPoint = height - (int)((address - min)/yFact)
			loadMap[[xPoint, yPoint]] = true
			break

			case 'modify':
			def address = Long.decode(attrs.getValue('address'))
			if (pageSize) address = address >> pageSize
			def xPoint = (int)(instTrack/xFact)
			def yPoint = height - (int)((address - min)/yFact)
			heapMap[[xPoint, yPoint]] = true
			break
		}
	}
}
