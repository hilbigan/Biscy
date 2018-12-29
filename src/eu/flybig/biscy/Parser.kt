package eu.flybig.biscy

import eu.flybig.biscy.TokenType.*

class Parser(val tokenizer: Tokenizer, val options: CompilerOptions){

    val generator = Generator(options.indent, options.outPrefix)
    val variables = Variables()

    init {
        tokenizer.advance()
    }

    fun parse(){
        match(BEGIN)
        block()
        match(END, noAdvance = true)
    }

    fun block(allowElse: Boolean = false){
        while(ctype != END && (!allowElse || ctype != ELSE)){
            optional(LOOP){
                loop()
            }
            optional(BREAK){
                doBreak()
            }
            optional(VARIABLE){
                assignment()
            }
            optional(WRITE){
                memory(load = false)
            }
            optional(LOAD){
                memory(load = true)
            }
            /*optional(PLUS, MINUS, LPAREN, VARIABLE, INTEGER){
                val eval = expr(5)
                println(eval.toString())
                println("eval:")
                variables.getRegister("result")
                val result = eval.eval()
                println(result.unpack().value)
            }*/
            optional(IFZ, IFN, IFP){
                ifstmt()
            }
        }
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

        val temp = variables.acquireTemporary()
        expr(temp).resolve()

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
        match(VARIABLE)
        if(ctype == INTEGER){
            val trg = tokenizer.current.value.toInt()
            val temp = if(trg != 0) variables.acquireTemporary() else 0
            match(INTEGER)

            if(temp != 0) generator.direct("li x$temp $trg")
            generator.direct("${if(load) "lw" else "sw"} x${variables.getRegister(src)} 0(x$temp)")
        } else if(ctype == VARIABLE){
            val trg = tokenizer.current.value
            match(VARIABLE)
            generator.direct("${if(load) "lw" else "sw"} x${variables.getRegister(src)} 0(x${variables.getRegister(trg)})")
        } else {
            fail("Invalid target address: Must be variable or integer literal")
        }
    }

    fun assignment(){
        val reg = variables.getRegister((tokenizer.current as VariableToken).value)
        match(VARIABLE)
        match(ASSIGNMENT)
        expr(reg).resolve()
    }

    fun doBreak(){
        match(BREAK)
        generator.breakLoop()
    }

    fun expr(evalReg: Int): ExpressionBuilder {
        val exprBuilder = ExpressionBuilder(evalReg, generator, variables)

        term(exprBuilder)

        zeroOrMore(PLUS, MINUS){
            addop(exprBuilder)
            term(exprBuilder)
        }

        return exprBuilder
    }

    fun term(exprBuilder: ExpressionBuilder){
        signedFactor(exprBuilder)
        zeroOrMore(DIVIDE, MULTIPLY, MODULUS) {
            mulop(exprBuilder)
            factor(exprBuilder)
        }
    }

    fun addop(exprBuilder: ExpressionBuilder){
        exprBuilder.value(tokenizer.current)
        when(ctype){
            PLUS -> {
                match(PLUS)
            }
            MINUS -> {
                match(MINUS)
            }
            else -> fail("Expected sum operator (+ -), but got \"${tokenizer.current.value}\"")
        }
    }

    fun mulop(exprBuilder: ExpressionBuilder){
        exprBuilder.value(tokenizer.current)
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
            else -> fail("Expected mul operator (* / %), but got \"${tokenizer.current.value}\"")
        }
    }

    fun signedFactor(exprBuilder: ExpressionBuilder){
        optional(PLUS, MINUS) { addop(exprBuilder) }
        factor(exprBuilder)
    }

    fun factor(exprBuilder: ExpressionBuilder){
        when(ctype){
            INTEGER -> {
                exprBuilder.value(tokenizer.current)
                match(INTEGER)
            }
            VARIABLE -> {
                exprBuilder.value(tokenizer.current)
                match(VARIABLE)
            }
            LPAREN -> {
                match(LPAREN)
                exprBuilder.value(expr(exprBuilder.evalReg))
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
        while(tokenizer.current.type == WHITESPACE || tokenizer.current.type == COMMENT)
            tokenizer.advance()
    }

    private fun match(expected: TokenType, noAdvance: Boolean = false){
        expect(tokenizer.current.type == expected, "Expected \"${expected.keyword}\" token, but got \"${tokenizer.current.value}\"")
        if(!noAdvance)
            advanceToken()
    }

    private fun expect(condition: Boolean, errorMsg: String){
        if(!condition){
            fail(errorMsg)
        }
    }

    private fun fail(msg: String){
        tokenizer.fail(msg)
    }

}