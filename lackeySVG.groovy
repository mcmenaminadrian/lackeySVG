import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*


class LackeySVGraph {

	LackeySVGraph(def width, def height, def inst, def fPath, def verb,
		def oF, def decile)
	{
		println "Opening $fPath"
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
		println "Biggest access is ${handler.maxSize}"
		println "Writing to $oF width: $width height: $height"
		if (inst) println "Recording instruction memory range"
		def handler2 = new SecondPassHandler(verb, handler, width, height,
			inst, oF, decile)
		reader.setContentHandler(handler2)
		reader.parse(new InputSource(new FileInputStream(fPath)))
		
	}
}


def svgCli = new CliBuilder
	(usage: 'lackeySVG [options] <lackeyml file>')
svgCli.w(longOpt:'width', args: 1,
	'width of SVG ouput - default 800')
svgCli.h(longOpt:'height', args: 1,
	 'height of SVG output - default 600')
svgCli.i(longOpt:'instructions', 'graph instructions - default false')
svgCli.u(longOpt:'usage', 'prints this information')
svgCli.v(longOpt:'verbose', 'prints verbose information - default false')
svgCli.d(longOpt:'decile', args:1,
	'only graph specified (1 - 10) decile - default is all')
svgCli.of(longOpt:'outfile', 'name output SVG file')

def oAss = svgCli.parse(args)
if (oAss.u || args.size() == 0) {
	svgCli.usage()
}
else {

	def width = 800
	def height = 600
	def decile = 0
	def inst = false
	def verb = false
	def oFile = "${new Date().time.toString()}.svg"
	if (oAss.w)
		width = Integer.parseInt(oAss.w)
	if (oAss.h)
		height = Integer.parseInt(oAss.h)
	if (oAss.i)
		inst = true
	if (oAss.v)
		verb = true
	if (oAss.of)
		oFile = oAss.of
	if (oAss.d) {
		def tDec = Integer.parseInt(oAss.d)
		if (tDec > 0 && tDec <= 10)
			decile = tDec
	}

	def lSVG = new LackeySVGraph(width, height, inst, args[args.size() - 1],
			verb, oFile, decile)
}