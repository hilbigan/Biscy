package eu.flybig.biscy

import java.io.File

class Compiler(filepath: String,
               private val onFinished: () -> Unit = {},
               private val options: CompilerOptions
) {

    private val parser: Parser
    private val tokenizer: Tokenizer

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

}

data class CompilerOptions(
    val printVarDump: Boolean = true,
    val outputVerbose: Boolean = true,
    val keepComments: Boolean = true
) {
    val outPrefix = if(outputVerbose) "[OUTPUT] " else ""
    val indent = outputVerbose
}