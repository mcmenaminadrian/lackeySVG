import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import java.util.concurrent.*


class LackeySVGraph {
	

	LackeySVGraph(def width, def height, def inst, def fPath, def verb,
		def oF, def percentile, def range, def pageSize, def gridMarks,
		def workingSetInst, def threads, def boost)
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
		if (pageSize)
			println "Using page size granularity of ${2**pageSize} bytes"
		if (percentile)
			println "Starting from $percentile with range $range%"

		def pool = Executors.newFixedThreadPool(threads)
		
			
		def memClosure = {
			def handler2 = new SecondPassHandler(verb, handler, width, height,
			inst, oF, percentile, range, pageSize, gridMarks, boost)
			def saxReader = SAXParserFactory.newInstance().
				newSAXParser().XMLReader	
			saxReader.setContentHandler(handler2)
			saxReader.parse(new InputSource(new FileInputStream(fPath)))
			println "Second pass complete"
		}
		
		def wsClosure = {
			def handler3 = new ThirdPassHandler(verb, handler, workingSetInst,
				width, height, gridMarks, boost)
			def saxReader = SAXParserFactory.newInstance().
				newSAXParser().XMLReader
			saxReader.setContentHandler(handler3)
			saxReader.parse(new InputSource(new FileInputStream(fPath)))
		}
		
		pool.submit(memClosure as Callable)
		pool.submit(wsClosure as Callable)

		def thetaMap = [:]
		def stepTheta = (int) handler.totalInstructions/width
		
		Closure stepClosure = {
			def steps = it
			
			Thread.start pass
		}
		(stepTheta .. handler.totalInstructions).step(stepTheta){
			def steps = it
			Closure pass = {
				if (verb)
					println "Setting theta to $steps"
				def handler4 = new FourthPassHandler(handler, steps, 12)
				def saxReader = SAXParserFactory.newInstance().newSAXParser().XMLReader
				saxReader.setContentHandler(handler4)
				saxReader.parse(new InputSource(new FileInputStream(fPath)))
				thetaMap[steps] = handler.totalInstructions / handler4.faults
			}
			pool.submit(pass as Callable)
		}
		pool.shutdown()
		pool.awaitTermination 5, TimeUnit.DAYS
			
		def graphTheta = new GraphTheta(thetaMap, width, height,
			handler.totalInstructions, gridMarks, boost)
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
svgCli.p(longOpt:'percentile', args:1, 'lowest percentile to graph')
svgCli.r(longOpt:'range', args:1, '(percentile) default is 10')
svgCli.g(longOpt:'pageshift', args:1, 'page size in power of 2 - 4KB = 12')
svgCli.m(longOpt:'gridmarks', args: 1, 'grid marks on graph - default 4')
svgCli.s(longOpt:'workingset', args: 1, 'instructions per working set')
svgCli.t(longOpt:'threadpool', args: 1, 'size of thread pool (default 3)')
svgCli.b(longOpt:'margins', args: 1, 'margin size on graphs (default 100px)')

def oAss = svgCli.parse(args)
if (oAss.u || args.size() == 0) {
	svgCli.usage()
}
else {

	def width = 800
	def height = 600
	def percentile = 0
	def range = 1
	def pageSize = 0
	def inst = false
	def verb = false
	def gridMarks = 4
	def wSSize = 100000
	def oFile = "${new Date().time.toString()}.svg"
	def threads = 3
	def boost = 100
	if (oAss.w)
		width = Integer.parseInt(oAss.w)
	if (oAss.h)
		height = Integer.parseInt(oAss.h)
	if (oAss.i)
		inst = true
	if (oAss.v)
		verb = true
	if (oAss.t) {
		threads = Integer.parseInt(oAss.t)
		if (threads < 1)
			threads = 3
	}
	if (oAss.b) {
		boost = Integer.parseInt(oAss.b)
		if (boost < 0)
			boost = 100
	}
	if (oAss.p) {
		def tPer = Integer.parseInt(oAss.p)
		if (tPer > 0 && tPer <= 100)
			percentile = tPer
		if (oAss.r) {
			def tRange = Integer.parseInt(oAss.r)
			if (tRange >= 1 && tRange <= (101 - percentile))
				range = tRange 
		}
	}
	if (oAss.m)
		gridMarks = Integer.parseInt(oAss.m)
	if (oAss.g) 
		pageSize = Integer.parseInt(oAss.g)
	if (oAss.s)
		wSSize = Integer.parseInt(oAss.s)

	def lSVG = new LackeySVGraph(width, height, inst, args[args.size() - 1],
			verb, oFile, percentile, range, pageSize, gridMarks, wSSize,
			threads, boost)
}
