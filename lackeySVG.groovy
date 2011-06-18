import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*


class LackeySVGraph {

	LackeySVGraph(def width, def height, def inst, def fPath, def verb){
		if (verb) println "Opening $fPath"
		def handler = new FirstPassHandler(verb)
		def reader = SAXParserFactory.newInstance().newSAXParser().XMLReader
		reader.setContentHandler(handler)
		reader.parse(new InputSource(new FileInputStream(fPath)))
		
		println "First pass completed"
		println "Instruction range is:"
		println "${handler.minInstructionAddr} - ${handler.maxInstructionAddr}"
		println "Instruction count is ${handler.totalInstructions}"
		println "Memory range is:"
		println "${handler.minHeapAddr} - ${handler.maxHeapAddr}"
		
	}
}


def svgCli = new CliBuilder
	(usage: 'lackeySVG [options] <lackeyml file>')
svgCli.w(longOpt:'width', 'width of SVG ouput - default 800')
svgCli.h(longOpt:'height', 'height of SVG output - default 600')
svgCli.i(longOpt:'instructions', 'graph instructions - default false')
svgCli.u(longOpt:'usage', 'prints this information')
svgCli.v(longOpt:'verbose', 'prints verbose information - default false')

def oAss = svgCli.parse(args)
if (oAss.u || args.size() == 0) {
	svgCli.usage()
}
else {

	def width = 800
	def height = 600
	def inst = false
	def verb = false
	if (oAss.w)
		width = oAss.w
	if (oAss.h)
		height = oAss.h
	if (oAss.i)
		inst = true
	if (oAss.v)
		verb = true

	def lSVG = new LackeySVGraph(width, height, inst, args[args.size() - 1],
			verb)
}