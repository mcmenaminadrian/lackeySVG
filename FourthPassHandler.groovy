import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

class FourthPassHandler extends DefaultHandler {

	def theta
	def faults = 0
	def firstPassHandler
	def instRange
	def mapWS = [:]
	def instCount = 0
	def instLast = 0
	def pageShift
	
	FourthPassHandler(def firstPassHandler, def theta, def pageShift) {
		super()
		this.firstPassHandler = firstPassHandler
		this.theta = theta
		this.pageShift = pageShift
		if (pageShift < 1 || pageShift > 64)
			pageShift = 12 //4k is the default
		def min = firstPassHandler.minInstructionAddr
		def max = firstPassHandler.maxInstructionAddr
		instRange = max - min
	}
	
	Map cleanWS()
	{
		return mapWS.findAll{
			it.value > instCount - theta
		}
	}
	
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		
		switch (qName) {
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instCount += siz
			def address = Long.decode(attrs.getValue('address')) >> pageShift
			if (!mapWS[address]) 
				faults++
			mapWS[address] = instCount
			if (instCount >= theta)
				mapWS = cleanWS()
			faulted = false
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = Long.decode(attrs.getValue('address')) >> pageShift
			if (!mapWS[address])
				faults++
			mapWS[address] = instLast
			break
		}
	}
		
	void endDocument()
	{
		println "Run for theta of $theta instructions completed:"
		println "Total faults $faults, with fault rate of ${instRange/faults}"
	}
	
}

