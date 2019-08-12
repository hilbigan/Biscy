package eu.flybig.biscy

import java.io.File

class Compiler(filepath: String,
               private val onFinished: () -> Unit = {},
               val options: CompilerOptions
) {

    val parser: Parser
    val tokenizer: Tokenizer

    val warnings = mutableListOf<String>()

    init {
        tokenizer = Tokenizer(File(filepath), options)
        parser = Parser(tokenizer, options)
    }

    fun start(){
        parser.parse()
        if(options.printVarDump){
            parser.variables.dump()
        }
        onFinished.invoke()
    }

    fun fail(msg: String){
        tokenizer.fail(msg)
    }

    fun warn(msg: String){
        if(msg !in warnings)
            warnings.add(msg)
    }

}

data class CompilerOptions(
    val printVarDump: Boolean = true,
    val outputVerbose: Boolean = true,
    val keepComments: Boolean = true
) {
    val outPrefix = if(outputVerbose) "[OUTPUT] " else ""
    val indent = outputVerbose
}