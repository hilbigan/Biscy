package eu.flybig.biscy

import eu.flybig.biscy.TokenType.*
import java.util.*

class Generator(val indent: Boolean = true, val outPrefix: String) {

    var loops = 0
    var loopIdentStack = Stack<String>()

    var ifs = 0
    var ifIdentStack = Stack<String>()

    var routines: MutableSet<String> = mutableSetOf()
    var calls: MutableSet<String> = mutableSetOf()
    var inRoutine = false
    var generateEndLabel = false


    fun beginLoop(){
        val loopIdent = "LOOP_" + (loops++)
        fout("$loopIdent:")
        loopIdentStack.push(loopIdent)
    }

    fun endLoop(){
        val loopIdent = loopIdentStack.pop()
        fout("beq x0 x0 $loopIdent")
        fout("END_$loopIdent:")
    }

    fun breakLoop(){
        if(loopIdentStack.empty()){
            fail("Unexpected BREAK: Nothing to break out of")
        }
        fout("beq x0 x0 END_${loopIdentStack.peek()}")
    }

    fun continueLoop(){
        if(loopIdentStack.empty()){
            fail("Unexpected CONTINUE: Not in loop")
        }
        fout("beq x0 x0 ${loopIdentStack.peek()}")
    }

    fun `return`(){
        if(!inRoutine){
            fout("jal x0 END")
            generateEndLabel = true
        } else {
            fout("jalr x0 0(x1)")
        }
    }

    fun beginIf(ifType: TokenType, exprReg: Int){
        val ident = "IF_" + (ifs++)
        when(ifType){
            IFZ -> fout("bne x$exprReg x0 END_$ident")
            IFP -> fout("blt x$exprReg x0 END_$ident")
            IFN -> fout("bgt x$exprReg x0 END_$ident")
        }
        ifIdentStack.push(ident)
    }

    fun elseIf(){
        val ident = ifIdentStack.pop()
        fout("beq x0 x0 END_ELSE_$ident")
        fout("END_$ident:")
        ifIdentStack.push(ident)
    }

    fun endIf(elseBranch: Boolean){
        val ident = ifIdentStack.pop()
        if(elseBranch) fout("END_ELSE_$ident:")
        else fout("END_$ident:")
    }

    fun direct(str: String){
        fout(str)
    }

    private fun fout(msg: String){
        val str = if(indent) {
            val indentation = loopIdentStack.size + ifIdentStack.size + if(inRoutine) 1 else 0
            var ret = ""
            (0 until indentation).map { ret += ("\t") }
            ret += msg
            ret
        } else {
            msg
        }
        out(str)
    }

    fun out(str: String){
        println("$outPrefix$str")
    }

    fun startOfRoutine(name: String) {
        if(name in routines){
            fail("Routine with name $name already declared!")
        } else if(routines.isEmpty()){
            fout("jal x0 END")
            fout("")
        }
        routines.add(name)
        fout("$name:")
        inRoutine = true
    }

    fun endOfRoutine(){
        if(!inRoutine){
            fail("Cannot end non-existing routine!")
        }
        inRoutine = false
        fout("jalr x0 0(x1)")
        fout("")
    }

    fun endOfFile() {
        if(calls.any { it !in routines }){
            val it = calls.first { it !in routines }
            fail("Call to undeclared routine \"$it\"!")
        }
        if(routines.isEmpty() && !generateEndLabel) return
        fout("END:")

        if(routines.any { it !in calls } && compiler?.options?.outputVerbose == true){
            routines.filter { it !in calls }.forEach {
                warn("Routine $it is never called!")
            }
        }
    }

    fun call(name: String) {
        if(inRoutine){
            fout("addi sp sp -4")
            fout("sw x1 0(sp)")
            fout("jal x1 $name")
            fout("lw x1 0(sp)")
            fout("addi sp sp 4")
        } else {
            fout("jal x1 $name")
        }
        calls.add(name)
    }

    fun stack(unstack: Boolean, vars: MutableList<String>, variables: Variables) {
        if(vars.size > 2 shl 9 - 1){
            // wtf
            fail("You're trying to stack too many variables at once!")
        }
        if(!unstack) fout("addi sp sp ${vars.size * -4}")
        vars.forEachIndexed { i, ident ->
            fout((if(unstack) "lw x" else "sw x") + variables.getRegister(ident) + " ${i * 4}(sp)")
        }
        if(unstack) fout("addi sp sp ${vars.size * 4}")
    }

}