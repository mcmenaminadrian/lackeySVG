import org.xml.sax.Attributes;


import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import groovy.xml.MarkupBuilder

/**
 * Processes lackeyml information for variable working set size
 * @author Adrian McMenamin
 *
 */
class FourthPassHandler extends DefaultHandler {

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
	 * Parse the lackeyml file mimicing a variable working set size
	 * @param firstPassHandler holds basic information about lackeyml file
	 * @param theta time (instructions executed) over which to hold WS
	 * @param pageShift bit shift for page size
	 */
	FourthPassHandler(def firstPassHandler, def theta, def pageShift) {
		super()
		this.firstPassHandler = firstPassHandler
		this.theta = theta
		this.pageShift = pageShift
		if (pageShift < 1 || pageShift > 64)
			pageShift = 12 //4k is the default
		totalInstructions = firstPassHandler.totalInstructions
		mapWS = new LinkedHashMap(1024, 0.7, true)
	}
	
	/**
	 * Purge pages from working set when they are too old
	 */
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
	
	/**
	 * SAX startElement - checks if referenced page is in working set
	 */
	void startElement(String ns, String localName, String qName,
		Attributes attrs) {
		
		switch (qName) {
			
			case 'instruction':
			def siz = Long.decode(attrs.getValue('size'))
			instCount += siz
			def address = (Long.decode(attrs.getValue('address')) >> pageShift)
			if (!mapWS[address]) { 
				faults++
				if (instCount - lastFault)
					aveSize = ((aveSize * lastFault) + 
						(mapWS.size() * (instCount - lastFault)))/instCount
				lastFault = instCount
			}
			
			mapWS[address] = instCount

			if (instCount > theta && instCount >= nextCount) {
				if (instCount - lastFault)
					aveSize = (double)((aveSize * lastFault) +
						(mapWS.size() * (instCount - lastFault)))/instCount
				cleanWS()
				lastFault = instCount
			}
			break
			
			case 'store':
			case 'load':
			case 'modify':
			def address = (Long.decode(attrs.getValue('address')) >> pageShift)
			if (!mapWS[address]) {
				faults++
				if (instCount - lastFault)
					aveSize = (double)((aveSize * lastFault) +
						(mapWS.size() * (instCount - lastFault)))/instCount
				lastFault = instCount
			}
			mapWS[address] = instCount
			break
		}
	}
		
	/**
	 * SAX endDocument - smooth exit and output some information
	 */
	void endDocument()
	{
		if (instCount - lastFault)
			aveSize = (double)((aveSize * lastFault) +
				(mapWS.size() * (instCount - lastFault)))/instCount
		println "Run for theta of $theta instructions completed:"
		println "Faults: $faults, g():${firstPassHandler.totalInstructions/faults}"
	}
	
}

