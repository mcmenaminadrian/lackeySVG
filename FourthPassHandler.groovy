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
	def pageShift
	
	FourthPassHandler(def firstPassHandler, def theta, def pageShift) {
		super()
		this.firstPassHandler = firstPassHandler
		this.theta = theta
		this.pageShift = pageShift
		if (pageShift < 1 || pageShift > 64)
			pageShift = 12 //4k is the default
		totalInstructions = firstPassHandler.totalInstructions
		def sortWS = [compare: {a, b ->
			return (a.value).compareTo(b.value)
		} ] as Comparator
		mapWS = new TreeMap(sortWS)
		println mapWS
	}
	
	void cleanWS()
	{ 
		println "Key: ${mapWS.firstEntry().getKey}  Value: ${mapWS.firstEntry.getValue()}"
		while ((mapWS.firstEntry()).getValue() < instCount - theta)
		{
			mapWS.remove((mapWS.firstEntry()).getKey())
		}
		
	}
	
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		
		switch (qName) {
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instCount += siz
			//model Linux LRU so evict even if no fault
			if (instCount > theta)
				cleanWS()
			def address = (Long.decode(attrs.getValue('address')) >> pageShift)
			if (!mapWS.get(address)) {
				faults++
			}
			mapWS.put(address, instCount)
			
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
		println "Total faults $faults, with g() of ${totalInstructions/faults}"
	}
	
}

