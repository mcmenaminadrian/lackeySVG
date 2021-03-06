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
class SixthPassHandler extends DefaultHandler {

	def theta
	def faults = 0
	def firstPassHandler
	def totalInstructions
	def mapWS
	def instCount = 0
	def nextCount = 0
	def pageShift
	def aveSize = 0
	def lastFault = 0
	
	/**
	 * Calculate fault rate for fixed max working set (FIFO style)
	 * @param firstPassHandler holds basic data about lackeyml file
	 * @param theta maximum size of page cache/working set
	 * @param pageShift bit shift for page size
	 */
	SixthPassHandler(def firstPassHandler, def theta, def pageShift) {
		super()
		this.firstPassHandler = firstPassHandler
		this.theta = theta
		this.pageShift = pageShift
		if (pageShift < 1 || pageShift > 64)
			pageShift = 12 //4k is the default
		totalInstructions = firstPassHandler.totalInstructions
		mapWS = new LinkedHashMap(512, 0.7, false)
	}
	
	/**
	 * Remove FIFO page
	 */
	void cleanWS()
	{
		//simply chop the oldest page
		mapWS.remove(mapWS.min{it.value}.key)
	}
	
	/**
	 * SAX startElement
	 */
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		
		switch (qName) {
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instCount += siz
			def address = (Long.decode(attrs.getValue('address')) 
				>> pageShift)
			if (!mapWS[address]){
				faults++
				if (instCount - lastFault)
					aveSize = (double) ((aveSize * lastFault) +
						(mapWS.size() * (instCount - 
						lastFault)))/instCount
				lastFault = instCount
				mapWS[address] = instCount
				if (mapWS.size() > theta)
					cleanWS()
			}
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = (Long.decode(attrs.getValue('address')) 
				>> pageShift)
			if (!mapWS[address]){
				faults++
				if (instCount - lastFault)
					aveSize = (double) ((aveSize * lastFault) +
						(mapWS.size() * (instCount - 
						lastFault)))/instCount
				lastFault = instCount
				
				mapWS[address] = instCount
				if (mapWS.size() > theta)
					cleanWS()
			}
			break
		}
	}
		
	/**
	 * SAX endDocument
	 */
	void endDocument()
	{
		println "Run for theta of $theta max size working set completed:"
		println "Faults: $faults, g(): ${firstPassHandler.totalInstructions/faults}"
		println "Ave. working set size $aveSize"
	}
	
}
