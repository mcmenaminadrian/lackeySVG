import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

class FifthPassHandler extends DefaultHandler {

	def theta
	def faults = 0
	def firstPassHandler
	def totalInstructions
	def mapWS
	def instCount = 0
	def nextCount = 0
	def pageShift
	def purged
	
	FifthPassHandler(def firstPassHandler, def theta, def pageShift) {
		super()
		this.firstPassHandler = firstPassHandler
		this.theta = theta
		this.pageShift = pageShift
		if (pageShift < 1 || pageShift > 64)
			pageShift = 12 //4k is the default
		totalInstructions = firstPassHandler.totalInstructions
		mapWS = new LinkedHashMap(128, 0.7, true)
		purged = false
	}
	
	void cleanWS()
	{
		//simply chop the oldest page
		Iterator it = mapWS.entrySet().iterator()
		it.remove()
	}
	
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		
		switch (qName) {
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instCount += siz
			def address = (Long.decode(attrs.getValue('address')) >> pageShift)
			if (!mapWS[address]) 
				faults++
			
			mapWS[address] = instCount

			if (mapWS.size() > theta)
				cleanWS()
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = (Long.decode(attrs.getValue('address')) >> pageShift)
			if (!mapWS[address])
				faults++
			mapWS[address] = instCount
			if (mapWS.size() > theta)
				cleanWS()
			break
		}
	}
		
	void endDocument()
	{
		println "Run for theta of $theta size working set completed:"
		println "Faults: $faults, g(): ${firstPassHandler.totalInstructions/faults}"
	}
	
}

