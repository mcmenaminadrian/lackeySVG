import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*

/**
 * 
 * @author Adrian McMenamin
 *
 */
class FirstPassHandler extends DefaultHandler {
	//handle the events from first pass through the XML file
	//called by SAX processor
	long totalInstructions = 0
	long minInstructionAddr = 0xFFFFFFFF
	long maxInstructionAddr = 0
	long minHeapAddr = 0xFFFFFFFF
	long maxHeapAddr = 0
	long maxSize = 0
	def verb
	def command
	def pageMap = [:]
	def pageShift
	
	/**
	 * Perform essential analysis of lackeyml file 
	 * @param verb verbose output
	 * @param pageShift bit shift for page size
	 */
	FirstPassHandler(def verb, def pageShift)
	{
		super()
		this.verb = verb
		if (pageShift < 1)
			this.pageShift = 12
		else
			this.pageShift = pageShift

	}
	/**
	 * Prints to standard output when verbose output selected
	 * @param str string to output
	 */
	void printout(def str)
	{
		if (verb)
			println(str)
	}
	
	/**
	 * SAX startElement - processes information about lackeyml
	 */
	void startElement(String ns, String localName, String qName, 
		Attributes attrs) {
			
		switch (qName) {
	
			case 'threadml':
			case 'lackeyml':
			case 'application':
			case 'thread':
			break;
				
			case 'instruction':
			long address
			def strAddr
			def siz
			def strSize
			strAddr = attrs.getValue('address');
			if (strAddr)
				address = Long.parseLong(strAddr, 16)
			else
				break
			strSize = attrs.getValue('size')
			if (strSize)
				siz = Long.parseLong(strSize, 16)
			else
				break
			if (siz > maxSize)
				maxSize = siz
			if (address < minInstructionAddr)
				minInstructionAddr = address
			if (address + siz > maxInstructionAddr)
				maxInstructionAddr = address + siz
			totalInstructions += siz
			printout "Instruction at $address of size $siz"
			def pgAddr = ((Long) address) >> pageShift
			pageMap[pgAddr] = true
			break
				
			case 'modify':
			case 'load':
			case 'store':
			BigInteger address
			def strSize
			def siz
			def strAddr = attrs.getValue('address')
			if (strAddr)
				address = Long.parseLong(strAddr, 16)
			else
				break
			strSize = attrs.getValue('size')
			if (strSize)
				siz = Long.parseLong(strSize, 16)
			else
				break
			if (siz > maxSize)
				maxSize = siz
			if (address < minHeapAddr)
				minHeapAddr = address
			if (address + siz > maxHeapAddr)
				maxHeapAddr = address + siz
			printout "$qName at $address of size $siz"
			def pgAddr = ((Long) address) >> pageShift
			pageMap[pgAddr] = true
			break
				
			default:
			println "Unrecognised element of type $qName"
		}
	}								
}
