package eu.flybig.biscy

import java.io.File

var compiler: Compiler? = null

/**
 * @author Aaron Hilbig
 * 28.12.2018
 */
fun main(args: Array<String>) {
    if(args.isEmpty()){
        println("Not enough arguments.")
        printUsage()
        System.exit(1)
    } else {
        if(args[0] in listOf("?","h","help","-help")){
            printUsage()
            System.exit(0)
        }
        if(!File(args[0]).exists()){
            println("File does not exist.")
            System.exit(1)
        }

        val flags = if(args.size > 1){
            args[1]
        } else ""

        val options = CompilerOptions(
            keepComments = 'k' in flags,
            outputVerbose = 'v' in flags,
            printVarDump = 'd' in flags
        )

        compiler = Compiler(args[0], options = options)
        compiler!!.start()

        if(options.outputVerbose){
            compiler!!.warnings.forEach {
                println("[WARNING] $it")
            }
        }
    }
}

fun fail(msg: String){
    if(compiler != null){
        compiler!!.fail(msg)
    } else {
        error(msg)
    }
}

fun warn(msg: String){
    if(compiler != null){
        compiler!!.warn(msg)
    } else {
        println(msg)
    }
}

fun printUsage(){
    println("Usage: java -jar biscy.jar <input-file.bitsy> [flags]")
    println("Flags:")
    println("\tv\tverbose output")
    println("\tk\tKeep comments")
    println("\td\tDump variable/register information")
}