package eu.flybig.biscy

import java.io.File
import java.io.PushbackReader

/**
 * @author Aaron Hilbig
 * 28.12.2018
 */
fun main(args: Array<String>) {
    val compiler = Compiler("fib.bitsy", options = CompilerOptions())
    compiler.start()
}