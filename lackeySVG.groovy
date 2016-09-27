import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import java.util.concurrent.*


/**
 * 
 * @author Adrian McMenamin, copyright 2011
 *
 */
class LackeySVGraph {

	def MEMPLOT = 0x01
	def WSPLOT = 0x02
	def LIFEPLOT = 0x04
	def LRUPLOT = 0x08

	/**
	 * Build various graphs from a lackeyml file
	 * 
	 * @param width width of the graph in pixels
	 * @param height height of the graph in pixels
	 * @param inst graph instructions
	 * @param fPath path to the lackeyml file being processed
	 * @param verb verbose output
	 * @param oF name of reference map output file
	 * @param percentile percentile of memory to form base of reference map
	 * @param range range of memory to be examined in reference map
	 * @param pageSize bit shift used for pages (eg 12 for 4096KB pages)
	 * @param gridMarks number of grid marks to be used on graphs
	 * @param workingSetInst number of instructions
	 * 			against which to plot working set
	 * @param threads size of thread pool
	 * @param boost size of margin on graphs
	 * @param PLOTS bitmask for graphs to be drawn
	 */
	LackeySVGraph(def width, def height, def inst, def fPath, def verb,
	def oF, def percentile, def range, def pageSize, def gridMarks,
	def workingSetInst, def threads, def boost, def PLOTS) {
		def thetaLRUMap
		def thetaMap
		def thetaAveMap
		def thetaLRUAveMap
		println "Opening $fPath"
		def handler = new FirstPassHandler(verb, pageSize)
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
		if (PLOTS & MEMPLOT)
			println "Writing to $oF width: $width height: $height"
		if (inst) println "Recording instruction memory range"
		if (pageSize)
			println "Using page size granularity of ${2**pageSize} bytes"
		if (percentile)
			println "Starting from $percentile with range $range%"
		def maxPg = (handler.pageMap).size()
		println "Total pages referrenced $maxPg"
		def pool = Executors.newFixedThreadPool(threads)


		def memClosure = {
			def handler2 = new SecondPassHandler(verb, handler, width, height,
					inst, oF, percentile, range, pageSize, gridMarks, boost)
			def saxReader = SAXParserFactory.newInstance().
					newSAXParser().XMLReader
			saxReader.setContentHandler(handler2)
			saxReader.parse(new InputSource(new FileInputStream(fPath)))
			println "Memory use mapping complete"
		}

		def wsClosure = {
			def handler3 = new ThirdPassHandler(verb, handler, workingSetInst,
					width, height, gridMarks, boost)
			def saxReader = SAXParserFactory.newInstance().
					newSAXParser().XMLReader
			saxReader.setContentHandler(handler3)
			saxReader.parse(new InputSource(new FileInputStream(fPath)))
			maxWS = handler3.maxWS
			println "Working set mapping complete"
		}

		if (PLOTS & MEMPLOT)
			pool.submit(memClosure as Callable)
		if (PLOTS & WSPLOT)
			pool.submit(wsClosure as Callable)

		if (PLOTS & LIFEPLOT) {
			println "Plotting life with variable WSS"
			thetaMap = Collections.synchronizedSortedMap(new TreeMap())
			thetaAveMap = Collections.synchronizedSortedMap(new TreeMap())
			int stepTheta =  handler.totalInstructions/width
			(stepTheta .. handler.totalInstructions).step(stepTheta){
				def steps = it
				Closure passWS = {
					if (verb)
						println "Setting theta to $steps"
					def handler4 = new FourthPassHandler(handler, steps,
							12)
					def saxReader =
							SAXParserFactory.newInstance().
								newSAXParser().XMLReader
					saxReader.setContentHandler(handler4)
					saxReader.parse(
							new InputSource(new FileInputStream(fPath)))
					def g = (int)(handler.totalInstructions /
							handler4.faults)
					thetaMap[steps] = g
					thetaAveMap[handler4.aveSize] = g
					println "Ave. working set ${handler4.aveSize}"
				}
				pool.submit(passWS as Callable)
			}
		}

		pool.shutdown()
		pool.awaitTermination 5, TimeUnit.DAYS


		def pool2 = Executors.newFixedThreadPool(threads)

		if (PLOTS & LRUPLOT) {
			thetaLRUMap = Collections.synchronizedSortedMap(new TreeMap())
			thetaLRUAveMap = Collections.synchronizedSortedMap(new TreeMap())
			int memTheta =  maxPg/width
			if (memTheta == 0)
				memTheta = 1
			(memTheta .. maxPg).step(memTheta){
				def mem = it
				Closure passLRU = {
					if (verb)
						println "Setting LRU theta to $mem"
					def handler5 = new FifthPassHandler(handler, mem,
							12)
					def saxLRUReader = SAXParserFactory.newInstance().
							newSAXParser().XMLReader
					saxLRUReader.setContentHandler(handler5)
					saxLRUReader.parse(
							new InputSource(new FileInputStream(fPath)))
					def g = (int)(handler.totalInstructions /
							handler5.faults)
					thetaLRUMap[mem] = g
					thetaLRUAveMap[handler5.aveSize] = g
				}
				pool2.submit(passLRU as Callable)
			}
		}

		pool2.shutdown()
		pool2.awaitTermination 5, TimeUnit.DAYS

		if (PLOTS & LIFEPLOT)
			def graphTheta = new GraphTheta(thetaMap, width, height,
					gridMarks, boost)
		if (PLOTS & LRUPLOT)
			def graphLRUTheta = new GraphLRUTheta(thetaLRUMap, width, height,
					gridMarks, boost)
		if ((PLOTS & LRUPLOT) && (PLOTS & LIFEPLOT))
			def graphCompTheta = new GraphCompTheta(thetaAveMap, 
				thetaLRUAveMap, width, height, gridMarks, boost)
	}
}


def svgCli = new CliBuilder
		(usage: 'lackeySVG [options] <lackeyml file>')
svgCli.w(longOpt:'width', args: 1,
		'width of SVG ouput - default 800')
svgCli.h(longOpt:'height', args: 1,
		'height of SVG output - default 600')
svgCli.i(longOpt: 'instructions', 'graph instructions - default false')
svgCli.u(longOpt: 'usage', 'prints this information')
svgCli.v(longOpt: 'verbose', 'prints verbose information - default false')
svgCli.p(longOpt: 'percentile', args:1, 'lowest percentile to graph')
svgCli.r(longOpt: 'range', args:1, '(percentile) default is 10')
svgCli.g(longOpt: 'pageshift', args:1, 'page size in power of 2 - 4KB = 12')
svgCli.m(longOpt: 'gridmarks', args: 1, 'grid marks on graph - default 4')
svgCli.s(longOpt: 'workingset', args: 1, 'instructions per working set')
svgCli.t(longOpt: 'threadpool', args: 1, 'size of thread pool (default 3)')
svgCli.b(longOpt: 'margins', args: 1, 'margin size on graphs (default 100px)')
svgCli.xm(longOpt: 'nomemplot', 'do not plot memory use')
svgCli.xw(longOpt: 'nowsplot', 'do not plot working set')
svgCli.xl(longOpt: 'nolifeplot', 'do not plot ws life time curve')
svgCli.xr(longOpt: 'nolruplot', 'do not plot lru life time curve')

def oAss = svgCli.parse(args)
if (oAss.u || args.size() == 0) {
	svgCli.usage()
}
else {

	def PLOTS = 0xFF
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

	if (oAss.xm)
		PLOTS = PLOTS ^ 0x01
	if (oAss.xw)
		PLOTS = PLOTS ^ 0x02
	if (oAss.xl)
		PLOTS = PLOTS ^ 0x04
	if (oAss.xr)
		PLOTS = PLOTS ^ 0x08

	def lSVG = new LackeySVGraph(width, height, inst, args[args.size() - 1],
			verb, oFile, percentile, range, pageSize, gridMarks, wSSize,
			threads, boost, PLOTS)
}
