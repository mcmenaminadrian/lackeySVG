import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

class FourthPassHandler extends DefaultHandler {

	def theta
	def faults = 0
	def firstPassHandler
	def totalInstructions
	def mapWS
	def instCount = 0
	def nextCount = 0
	def pageShift
	def sizes = []
	
	FourthPassHandler(def firstPassHandler, def theta, def pageShift) {
		super()
		this.firstPassHandler = firstPassHandler
		this.theta = theta
		this.pageShift = pageShift
		if (pageShift < 1 || pageShift > 64)
			pageShift = 12 //4k is the default
		totalInstructions = firstPassHandler.totalInstructions
		mapWS = new LinkedHashMap(128, 0.7, true)
	}
	
	void cleanWS()
	{
		def cut = instCount - theta
		Iterator it = mapWS.entrySet().iterator()
		while (it.hasNext()){
			Map.Entry page = (Map.Entry)it.next(); 
			nextCount = page.getValue()
			if (nextCount < cut)
				it.remove()
			else 
				break
		}
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

			if (instCount > theta && instCount >= nextCount)
				cleanWS()
			sizes << mapWS.size()
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = (Long.decode(attrs.getValue('address')) >> pageShift)
			if (!mapWS[address])
				faults++
			mapWS[address] = instCount
			break
		}
	}
		
	void endDocument()
	{
		println "Run for theta of $theta instructions completed:"
		println "Faults: $faults, g(): ${firstPassHandler.totalInstructions/faults}"
		BigInteger szSum = 0
		sizes.each{szSum += it}
		def samples = sizes.size()
		def avSize = szSum/samples
		println "Average working set size was $avSize"
	}
	
}

