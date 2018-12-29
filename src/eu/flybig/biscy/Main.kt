package eu.flybig.biscy

import java.io.File
import java.io.PushbackReader

/**
 * @author Aaron Hilbig
 * 28.12.2018
 */
fun main(args: Array<String>) {
    val tokenizer = Tokenizer(File("fib.bitsy"))

    /*while(tokenizer.hasNext()){
        tokenizer.advance()
        println(tokenizer.current.javaClass.simpleName.toString().padEnd(20) + "" + tokenizer.current.type.toString().padEnd(20)  + " " + tokenizer.current.value.trim())
    }*/
    Parser(tokenizer).parse()
}