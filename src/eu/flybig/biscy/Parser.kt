package eu.flybig.biscy

import com.sun.org.apache.xml.internal.utils.StringBufferPool.free
import eu.flybig.biscy.TokenType.*
import kotlin.math.exp
import kotlin.test.currentStackTrace

class Parser(val tokenizer: Tokenizer, val options: CompilerOptions){

    val generator = Generator(options.indent, options.outPrefix)
    val variables = Variables()

    init {
        tokenizer.advance()
    }

    fun parse(){
        match(BEGIN)
        block()
        match(END)
        while(tokenizer.current !is EOFToken){
            optional(ROUTINE){
                routine()
            }
        }
        if(tokenizer.current is EOFToken)
            match(WHITESPACE, noAdvance = true)
        generator.endOfFile()
    }

    fun routine(){
        match(ROUTINE)
        if(ctype == IDENTIFIER){
            generator.startOfRoutine(tokenizer.current.value)
        }
        match(IDENTIFIER)
        block()
        match(END)
        generator.endOfRoutine()
    }

    fun call(){
        match(CALL)
        if(ctype == IDENTIFIER){
            generator.call(tokenizer.current.value)
        }
        match(IDENTIFIER)
    }

    fun free(){
        match(FREE)
        if(ctype == IDENTIFIER){
            if(!variables.isDeclared(tokenizer.current.value)){
                fail("Unknown identifier ${tokenizer.current.value} (cannot free undeclared variable)")
            }
            variables.free(variables.getRegister(tokenizer.current.value))
        }
        match(IDENTIFIER)
    }

    fun block(allowElse: Boolean = false){
        while(ctype != END && (!allowElse || ctype != ELSE)){
            var empty = true
            optional(LOOP){
                loop()
                empty = false
            }
            optional(BREAK){
                doBreak()
                empty = false
            }
            optional(CONTINUE){
                doContinue()
                empty = false
            }
            optional(IDENTIFIER){
                assignment()
                empty = false
            }
            optional(WRITE){
                memory(load = false)
                empty = false
            }
            optional(LOAD){
                memory(load = true)
                empty = false
            }
            optional(RETURN){
                match(RETURN)
                generator.`return`()
                empty = false
            }
            optional(CALL){
                call()
                empty = false
            }
            optional(FREE){
                free()
                empty = false
            }
            optional(ASM){
                inlineAssembly()
                empty = false
            }
            optional(STACK){
                stack(false)
                empty = false
            }
            optional(UNSTACK){
                stack(true)
                empty = false
            }
            optional(IFZ, IFN, IFP){
                ifstmt()
                empty = false
            }
            if(empty && ctype != END && (!allowElse || ctype != ELSE)){
                if(tokenizer.current is EOFToken) fail("Unexpected EOF")
                fail("Invalid token: $ctype (\"${tokenizer.current.value}\")")
            }
        }
    }

    fun stack(unstack: Boolean = false){
        if(unstack) match(UNSTACK)
        else match(STACK)

        val vars = mutableListOf<String>()
        while(ctype != END && tokenizer.current !is EOFToken){
            vars += tokenizer.current.value
            match(IDENTIFIER)
        }
        match(END)

        if(vars.any{!variables.isDeclared(it)} && options.outputVerbose){
            vars.filter { !variables.isDeclared(it) }.forEach {
                warn("Pushing previously undeclared variable \"$it\" to stack")
            }
        }

        generator.stack(unstack, vars, variables)
    }

    private fun inlineAssembly() {
        tokenizer.inlineAssemblyMode = true
        match(ASM)
        var string = ""
        while(ctype != ASM && tokenizer.current !is EOFToken){
            var dollar = false
            optional(DOLLAR){
                dollar = true
                match(DOLLAR)
                if(ctype == IDENTIFIER && variables.isDeclared(tokenizer.current.value)){
                    string += ("x" + variables.getRegister(tokenizer.current.value) + " ")
                    advanceToken()
                } else {
                    if(ctype != IDENTIFIER)
                        fail("Expected identifier after \"$\" in inline assembly section!")
                    else
                        fail("Undeclared variable \"${tokenizer.current.value}\"")
                }
            }
            if(dollar) continue
            string += (tokenizer.current.value) + " "
            advanceToken()
        }
        tokenizer.inlineAssemblyMode = false
        generator.direct(string)
        match(ASM)
    }

    fun loop(){
        match(LOOP)
        generator.beginLoop()

        block()

        match(END)
        generator.endLoop()
    }

    fun ifstmt(){

        val ifType = ctype

        when (ctype) {
            IFZ -> match(IFZ)
            IFN -> match(IFN)
            IFP -> match(IFP)
        }

        //val temp = variables.acquireTemporary()
        val temp = expr(variables.acquireTemporary()).resolve(true)

        generator.beginIf(ifType, temp)

        block(allowElse = true)

        if(ctype == ELSE){
            generator.elseIf()
        } else {
            generator.endIf(false)
        }

        optional(ELSE){
            match(ELSE)
            block()

            generator.endIf(true)
        }

        match(END)
    }

    fun memory(load: Boolean){
        match(if(load) LOAD else WRITE)
        val src = tokenizer.current.value
        match(IDENTIFIER)
        if(ctype == INTEGER){
            val trg = tokenizer.current.value.toInt()
            val temp = if(trg != 0) variables.acquireTemporary() else 0
            match(INTEGER)

            if(temp != 0) generator.direct("li x$temp $trg")
            generator.direct("${if(load) "lw" else "sw"} x${variables.getRegister(src)} 0(x$temp)")
        } else if(ctype == IDENTIFIER){
            val trg = tokenizer.current.value
            match(IDENTIFIER)
            generator.direct("${if(load) "lw" else "sw"} x${variables.getRegister(src)} 0(x${variables.getRegister(trg)})")
        } else {
            fail("Invalid target address: Must be variable or integer literal")
        }
    }

    fun assignment(){
        val reg = variables.getRegister((tokenizer.current as VariableToken).value)
        match(IDENTIFIER)
        match(ASSIGNMENT)
        expr(reg).resolve()
    }

    fun doBreak(){
        match(BREAK)
        generator.breakLoop()
    }

    fun doContinue(){
        match(CONTINUE)
        generator.continueLoop()
    }

    fun expr(evalReg: Int): ExpressionBuilder {
        val exprBuilder = ExpressionBuilder(evalReg, generator, variables)

        term(exprBuilder)

        zeroOrMore(PLUS, MINUS, OR){
            addop(exprBuilder)
            term(exprBuilder)
        }

        return exprBuilder
    }

    fun term(exprBuilder: ExpressionBuilder){
        signedFactor(exprBuilder)
        zeroOrMore(DIVIDE, MULTIPLY, MODULUS, XOR, AND, SHL, SHR, SHRA) {
            mulop(exprBuilder)
            factor(exprBuilder)
        }
    }

    fun addop(exprBuilder: ExpressionBuilder){
        exprBuilder.add(tokenizer.current)
        when(ctype){
            PLUS -> {
                match(PLUS)
            }
            MINUS -> {
                match(MINUS)
            }
            OR -> {
                match(OR)
            }
            else -> fail("Expected sum operator (+ -), but got \"${tokenizer.current.value}\"")
        }
    }

    fun mulop(exprBuilder: ExpressionBuilder){
        exprBuilder.add(tokenizer.current)
        when(ctype){
            MULTIPLY -> {
                match(MULTIPLY)
            }
            DIVIDE -> {
                match(DIVIDE)
            }
            MODULUS -> {
                match(MODULUS)
            }
            XOR -> {
                match(XOR)
            }
            AND -> {
                match(AND)
            }
            SHL -> {
                match(SHL)
            }
            SHR -> {
                match(SHR)
            }
            SHRA -> {
                match(SHRA)
            }
            else -> fail("Expected mul operator (* / %), but got \"${tokenizer.current.value}\"")
        }
    }

    fun signedFactor(exprBuilder: ExpressionBuilder){
        optional(PLUS, MINUS) {
            addop(exprBuilder)
        }
        factor(exprBuilder)
    }

    fun factor(exprBuilder: ExpressionBuilder){
        when(ctype){
            INTEGER -> {
                exprBuilder.add(tokenizer.current)
                match(INTEGER)
            }
            IDENTIFIER -> {
                exprBuilder.add(tokenizer.current)
                match(IDENTIFIER)
            }
            LPAREN -> {
                match(LPAREN)
                exprBuilder.add(expr(exprBuilder.evalReg))
                match(RPAREN)
            }
            else -> fail("Expected factor, but got \"${tokenizer.current.value}\"")
        }
    }

    private val ctype: TokenType
        get() = tokenizer.current.type

    private fun zeroOrMore(vararg tokens: TokenType, exec: () -> Unit){
        while(ctype in tokens) {
            exec.invoke()
        }
    }

    private fun optional(vararg tokens: TokenType, exec: () -> Unit){
        if(ctype in tokens){
            exec.invoke()
        }
    }

    private fun advanceToken(){
        tokenizer.advance()
        while((ctype == WHITESPACE || ctype == COMMENT) && tokenizer.current !is EOFToken) {
            tokenizer.advance()
            if(options.keepComments && ctype == COMMENT){
                generator.direct("#" + tokenizer.current.value.substring(1, tokenizer.current.value.length - 2))
            }
        }
    }

    private fun match(expected: TokenType, noAdvance: Boolean = false){
        val desc = if(expected.keyword == null) expected.name else "\"${expected.keyword}\""
        expect(tokenizer.current.type == expected, "Expected $desc token, but got \"${tokenizer.current.value}\"")
        if(!noAdvance)
            advanceToken()
    }

    private fun expect(condition: Boolean, errorMsg: String){
        if(!condition){
            fail(errorMsg)
        }
    }

}