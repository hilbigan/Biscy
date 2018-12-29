package eu.flybig.biscy

class ExpressionBuilder(val evalReg: Int, val generator: Generator, val variables: Variables) {

    var parts = mutableListOf<Any>()

    fun resolve(){
        val initialSize = parts.size
        val result = eval()
        when(result){
            is IntegerToken -> generator.direct("li x$evalReg ${result.value.toInt()}")
            is VariableToken -> if(initialSize == 1) generator.direct("add x$evalReg x${variables.getRegister(result.value)} x0")
        }
    }

    fun eval(treg: Int = evalReg): Token {
        parts = parts.map {
            if(it is ExpressionBuilder){
                it.eval(variables.acquireTemporary())
            } else {
                it
            }
        }.toMutableList()

        if(parts.size == 3){
            if(parts[1] !is KeywordToken){
                fail("${parts[1]} was expected to be a (key-) token")
                return IntegerToken(-1) //unreachable
            }
            // Mindestens ein Token ist Literal
            if(parts[0] is IntegerToken && parts[2] is IntegerToken){

                try {
                    val x = (parts[0] as IntegerToken).value.toInt()
                    val y = (parts[2] as IntegerToken).value.toInt()

                    when((parts[1] as Token).type){
                        TokenType.PLUS -> return IntegerToken(x + y)
                        TokenType.MINUS -> return IntegerToken(x - y)
                        TokenType.MULTIPLY -> return IntegerToken(x * y)
                        TokenType.DIVIDE -> return IntegerToken(x / y)
                    }
                } catch (e: NumberFormatException){
                    fail("Could not parse an integer literal.")
                }
            } else if(parts[0] is IntegerToken){
                try {
                    val x = (parts[0] as IntegerToken).value.toInt()

                    if(x == 0 && ((parts[1] as Token).type == TokenType.PLUS || (parts[1] as Token).type == TokenType.MINUS)){
                        return (parts[2] as VariableToken)
                    } else if(x == 1 && (parts[1] as Token).type == TokenType.MULTIPLY){
                        return (parts[2] as VariableToken)
                    } else {
                        var reg = variables.getRegister((parts[2] as VariableToken).value)
                        when((parts[1] as Token).type){
                            TokenType.PLUS -> {
                                generator.direct("addi x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            TokenType.MINUS -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("sub x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            TokenType.MULTIPLY -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("mul x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            TokenType.DIVIDE -> {
                                fail("Division not implemented")
                            }
                        }
                    }
                } catch (e: NumberFormatException){
                    fail("Could not parse an integer literal.")
                }
            } else if(parts[2] is IntegerToken){
                try {
                    val x = (parts[2] as IntegerToken).value.toInt()

                    if(x == 0 && ((parts[1] as Token).type == TokenType.PLUS || (parts[1] as Token).type == TokenType.MINUS)){
                        return (parts[0] as VariableToken)
                    } else if(x == 0 && (parts[1] as Token).type == TokenType.MULTIPLY){
                        return IntegerToken(0)
                    } else if(x == 0 && (parts[1] as Token).type == TokenType.DIVIDE){
                        fail("Divide by zero")
                        return IntegerToken(-1) //unreachable
                    } else if(x == 1 && ((parts[1] as Token).type == TokenType.MULTIPLY || (parts[1] as Token).type == TokenType.DIVIDE)){
                        return (parts[0] as VariableToken)
                    } else {
                        var reg = variables.getRegister((parts[0] as VariableToken).value)
                        when((parts[1] as Token).type){
                            TokenType.PLUS -> {
                                generator.direct("addi x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            TokenType.MINUS -> {
                                generator.direct("addi x$treg x$reg -$x")
                                return simpleResult(treg, variables)
                            }
                            TokenType.MULTIPLY -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("mul x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            TokenType.DIVIDE -> {
                                fail("Division not implemented")
                            }
                        }
                    }
                } catch (e: NumberFormatException){
                    fail("Could not parse an integer literal.")
                }
            }
            // Ende Literale

            val op = (parts[1] as Token).type
            val src0 = variables.getRegister((parts[0] as VariableToken).value)
            val src1 = variables.getRegister((parts[2] as VariableToken).value)
            when(op){
                TokenType.PLUS -> {
                    generator.direct("add x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                TokenType.MINUS -> {
                    generator.direct("sub x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                TokenType.MULTIPLY -> {
                    generator.direct("mul x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                TokenType.DIVIDE -> {
                    fail("Division not implemented")
                }
            }

        } else if(parts.size == 2){
            if(parts[0] !is KeywordToken){
                fail("${parts[0]} was expected to be a (key-) token")
                return IntegerToken(-1) //unreachable
            }
            if(parts[1] is IntegerToken){
                try {
                    val x = (parts[1] as IntegerToken).value.toInt()
                    when((parts[0] as KeywordToken).type){
                        TokenType.PLUS -> return IntegerToken(x)
                        TokenType.MINUS -> return IntegerToken(-x)
                        else -> {
                            fail("Illegal token ${(parts[0] as IntegerToken).value}")
                            return IntegerToken(-1) //unreachable
                        }
                    }
                } catch (e: NumberFormatException){
                    fail("Could not parse integer literal \"${(parts[1] as IntegerToken).value}\".")
                }
            } else if(parts[1] is VariableToken){
                when((parts[0] as KeywordToken).type){
                    TokenType.PLUS -> return (parts[1] as VariableToken)
                    TokenType.MINUS -> {
                        var reg = variables.getRegister((parts[1] as VariableToken).value)
                        generator.direct("xori x$treg x$reg -1")
                        return simpleResult(treg, variables)
                    }
                    else -> {
                        fail("Illegal token ${(parts[0] as IntegerToken).value}")
                        return IntegerToken(-1) //unreachable
                    }
                }
            } else {
                fail("Unexpected token: ${parts[1]}. Expected integer literal or identifier.")
                return IntegerToken(-1) //unreachable
            }
        } else if(parts.size == 1){
            when((parts[0] as Token).type){
                TokenType.INTEGER -> {
                    try {
                        val x = (parts[0] as IntegerToken).value.toInt()
                        return IntegerToken(x.toString())
                    } catch(e: NumberFormatException){
                        fail("Could not parse integer literal \"${(parts[0] as IntegerToken).value}\".")
                    }
                }
                TokenType.VARIABLE -> {
                    return (parts[0] as VariableToken)
                }
                else -> fail("Invalid single token: \"${(parts[0] as Token).value}\"")
            }
        }

        fail("Expression not parsable: $this")
        return IntegerToken(-1) //unreachable
    }

    fun value(value: Any){
        parts.add(value)
    }

    override fun toString(): String {
        return "" + evalReg + " <- " + (parts.toString())
    }

    fun fail(msg: String){
        System.err.println("[ERROR] ExpressionBuilder: $msg")
        System.err.println("in " + this.toString())
        System.exit(1)
    }
}

/*class AddOp(dest: String, src1: String, src2: String, immediate: Boolean) :
    SimpleOperation(dest, src1, src2, immediate) {

    override fun generate(): String = "add${imm()} $dest $src1 $src2"

    override fun isResolved(): Boolean = false

    override fun unpack(): Token = error("Cannot resolve this operation to a value at compile time.")

}*/

fun simpleResult(treg: Int, variables: Variables) : VariableToken {
    return VariableToken(variables.getIdentifier(treg))
}

class VariableResult(val vari: VariableToken) : ExpressionResult {

    override fun unpack(): VariableToken = vari

    override fun isResolved(): Boolean = true

}

class ValueResult(val result: Int) : ExpressionResult {

    override fun unpack(): IntegerToken = IntegerToken(result.toString())

    override fun isResolved(): Boolean = true

}

/*abstract class SimpleOperation(val dest: String, val src1: String, val src2: String, val immediate: Boolean = false) : ExpressionResult {

    fun imm(): String = if(immediate) "i" else ""

    abstract fun generate(): String
}*/

interface ExpressionResult {
    fun isResolved(): Boolean
    fun unpack(): Token
}