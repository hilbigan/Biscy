package eu.flybig.biscy

/**
 * @author Aaron Hilbig
 * 28.12.2018
 */
fun main(args: Array<String>) {
    val compiler = Compiler("literals.bitsy", options = CompilerOptions())
    compiler.start()
}