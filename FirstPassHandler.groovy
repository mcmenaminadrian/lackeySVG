import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*


class FirstPassHandler extends DefaultHandler {
	//handle the events from first pass through the XML file
	//called by SAX processor
	def totalInstructions = 0
	def minInstructionAddr = 0xFFFFFFFF
	def maxInstructionAddr = 0
	def minHeapAddr = 0xFFFFFFFF
	def maxHeapAddr = 0
	def verb
	def command
	
	FirstPassHandler(def verb)
	{
		super()
		this.verb = verb
	}
	
	void printout(def str)
	{
		if (verb)
			println(str)
	}
	
	void startElement(String ns, String localName, String qName, 
			Attributes attrs) {
			
			switch (qName) {
				
				case 'lackeyxml':
				break;
				
				case 'application':
				command = attrs.getValue('command')
				printout("Application is $command")
				break
				
				case 'instruction':
				def address = Long.decode(attrs.getValue('address'))
				def siz = Long.decode(attrs.getValue('size'))
				if (address < minInstructionAddr)
					minInstructionAddr = address
				if (address + siz > maxInstructionAddr)
					maxInstructionAddr = address + siz
				totalInstructions += siz
				printout "Instruction at $address of size $siz"
				break
				
				case 'modify':
				case 'load':
				case 'store':
				BigInteger address = Long.decode(attrs.getValue('address'))
				def siz = Long.decode(attrs.getValue('size'))
				if (address < minHeapAddr)
					minHeapAddr = address
				if (address + siz > maxHeapAddr)
					maxHeapAddr = address + siz
				printout "$qName at $address of size $siz"
				break
				
				default:
				println "Unrecognised element of type $qName"
			}
	}
	
								
}
