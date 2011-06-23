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
	def boostSize = 100 //margins around the graph
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
		if (pageSize) {
			svg.text(x:boostSize, y:cHeight,
				style: "font-family: Helvetica; font-size: 10; fill: black",
				"Page size ${2**pageSize}, $rPer - ${rPer + range}% memory"){}
		}
		else {
			svg.text(x:boostSize, y:cHeight,
				style: "font-family: Helvetica; font-size: 10; fill: black",
				 "From $rPer - ${rPer + range}"){} 
		}


		if (verb) println "Drawing axes"
		svg.line(x1:boostSize - 5, y1:5 + cHeight - boostSize,
			x2:cWidth - boostSize + 5, y2:5 + cHeight - boostSize,
			stroke:"black", "stroke-width":10){}
		svg.line(x1:boostSize - 5, y1:5 + cHeight - boostSize,
			x2:boostSize - 5, y2:boostSize, stroke:"black", "stroke-width":10){}
			
		(0 .. 4).each { i ->
			svg.line(x1:(int)(boostSize + width * i/4),
					y1:15 + cHeight - boostSize,
					x2:(int)(boostSize + width * i/4),
					y2: boostSize,
					stroke:"maroon", "stroke-width":1){}
			svg.text(x:(int)(boostSize - 5 + width * i/4),
				y:20 + cHeight - boostSize,
				style: "font-family: Helvetica; font-size:10; fill: maroon",
				((int) instRange * i / 4))
			svg.line(x1:boostSize - 15,
					y1:(int)(height * i/4 + boostSize),
					x2:cWidth - boostSize,
					y2:(int)(height * i/4 + boostSize),
					stroke:"maroon", "stroke-width":3){} 
			svg.text(x:boostSize - 40,
				y: (int)(5 + height * i/4 + boostSize),
				style: "font-family: Helvetica; font-size:10; fill: maroon",
				(Long.toString(max - (int) ((max - min) * i/4), 16))) 
		}
	}

	void endDocument() {
		println "]"
		println "Mapping complete, now drawing points"
		if (!percentile) {
			if (inst) {
				instMap.each{
					k, v ->
					svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
						fill:"none", stroke:"red", "stroke-width":1){}
				}
			}
			storeMap.each {
				k, v ->
				svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
					fill:"none", stroke:"yellow", "stroke-width":1){}
			}
			loadMap.each {
				k, v ->
				svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
					fill:"none", stroke:"blue", "stroke-width":1){}
			}
			heapMap.each {
				k, v ->
				svg.circle(cx:k[0] + boostSize, cy:k[1] + boostSize, r:1,
					fill:"none", stroke:"green", "stroke-width":1){}
			}
		} else {
			def miny = height * (factor * (percentile - 1))
			def maxy = miny + height
			if (inst) {
				instMap.each{
					k, v ->
					if (k[1] in miny .. maxy) {
						def replot = k[1] - miny + boostSize
						svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
							fill:"none", stroke:"red", "stroke-width":1){}
					}
				}
			}
			storeMap.each {
				k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny + boostSize
					svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
						fill:"none", stroke:"yellow", "stroke-width":1){}
				}
			}
			loadMap.each {
				k, v ->
				if (k[1] in miny .. maxy) {
					def replot = k[1] - miny + boostSize
					svg.circle(cx:k[0] + boostSize, cy:replot, r:1,
						fill:"none", stroke:"blue", "stroke-width":1){}
				}
			}
			heapMap.each {
				k, v ->
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
			def yDraw = (int) height/10
			svg.text(x:width, y: yDraw,  
				style:"font-family:Helvetica; font-size:10; fill: black",
				"$command"){}
			def yInt = (int) height/20
			yDraw += yInt
			def margin = width + boostSize
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

			if (instTrack > ((int)(instRange * travel)/100)){
				print ">"
				travel++
			}
			if (inst) {
				def address = Long.decode(
					attrs.getValue('address')) >> pageSize
				def xPoint = (int)(instTrack/xFact)
				def yPoint = height - (int)((address - min)/yFact)
				instMap[[xPoint, yPoint]] = true
			}
			break

			case 'store':
			def address = Long.decode(attrs.getValue('address')) >> pageSize
			def xPoint = (int)(instTrack/xFact)
			def yPoint = height - (int)((address - min)/yFact)
			storeMap[[xPoint, yPoint]] = true
			break

			case 'load':
			def address = Long.decode(attrs.getValue('address')) >> pageSize
			def xPoint = (int)(instTrack/xFact)
			def yPoint = height - (int)((address - min)/yFact)
			loadMap[[xPoint, yPoint]] = true
			break

			case 'modify':
			def address = Long.decode(attrs.getValue('address')) >> pageSize
			def xPoint = (int)(instTrack/xFact)
			def yPoint = height - (int)((address - min)/yFact)
			heapMap[[xPoint, yPoint]] = true
			break
		}
	}
}
