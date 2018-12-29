package eu.flybig.biscy

import eu.flybig.biscy.TokenType.*
import java.util.*

class Generator(val indent: Boolean = true) {

    var loops = 0
    var loopIdentStack = Stack<String>()

    var ifs = 0
    var ifIdentStack = Stack<String>()

    fun beginLoop(){
        val loopIdent = "LOOP_" + (loops++)
        fout("$loopIdent:")
        loopIdentStack.push(loopIdent)
    }

    fun endLoop(){
        val loopIdent = loopIdentStack.pop()
        fout("END_$loopIdent: beq x0 x0 $loopIdent")
    }

    fun breakLoop(){
        fout("beq x0 x0 END_${loopIdentStack.peek()} #break")
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
            val indentation = loopIdentStack.size + ifIdentStack.size
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
        println("[OUTPUT] $str")
    }

}