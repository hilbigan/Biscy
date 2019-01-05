package eu.flybig.biscy

import eu.flybig.biscy.TokenType.*

class ExpressionBuilder(val evalReg: Int, val generator: Generator, val variables: Variables) : Evaluable {

    var parts = mutableListOf<Evaluable>()

    fun resolve(ifstmt: Boolean = false): Int {
        val initialSize = parts.size
        val result = eval()
        when(result){
            is IntegerToken -> generator.direct("li x$evalReg ${result.value.toInt()}")
            is VariableToken -> if(variables.getRegister(result.value) != evalReg && !ifstmt){
                generator.direct("add x$evalReg x${variables.getRegister(result.value)} x0")
            } else if(ifstmt){
                return variables.getRegister(result.value)
            }
        }
        return evalReg
    }

    fun eval(treg: Int = evalReg): Token {
        parts = parts.map {
            if(it is ExpressionBuilder){
                it.eval(variables.acquireTemporary())
            } else {
                it
            }
        }.toMutableList()

        fun orderOfOperations(it: Evaluable): Boolean = it is KeywordToken && (it.type in listOf(MULTIPLY, DIVIDE, MODULUS, SHL, SHR, AND, SHRA))

        while(parts.size > 3){
            if(parts.any(::orderOfOperations)){
                val expr = ExpressionBuilder(variables.acquireTemporary(), generator, variables)
                val idx = parts.indexOf(parts.first(::orderOfOperations))

                expr.add(parts[idx - 1])
                expr.add(parts[idx])
                expr.add(parts[idx + 1])

                parts = parts.filterIndexed { index, _ -> index !in listOf(idx - 1, idx, idx + 1) }.toMutableList()

                parts.add(idx - 1, expr.eval())
            } else {
                val takeThree = parts[0] !is KeywordToken
                val expr = ExpressionBuilder(variables.acquireTemporary(), generator, variables)
                expr.add(parts[0])
                expr.add(parts[1])
                if (takeThree)
                    expr.add(parts[2])
                parts = parts.drop(if (takeThree) 3 else 2).toMutableList()

                parts.add(0, expr.eval())
            }
        }

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
                        TokenType.AND -> return IntegerToken(x and y)
                        TokenType.OR -> return IntegerToken(x or y)
                        TokenType.XOR -> return IntegerToken(x xor y)
                        TokenType.SHL -> return IntegerToken(x shl y)
                        TokenType.SHR -> return IntegerToken(x ushr y)
                        TokenType.SHRA -> return IntegerToken(x shr y)
                    }
                } catch (e: NumberFormatException){
                    fail("Could not parse an integer literal.")
                }
            } else if(parts[0] is IntegerToken){
                try {
                    val x = (parts[0] as IntegerToken).value.toInt()

                    if(x == 0 && ((parts[1] as Token).type == TokenType.PLUS || (parts[1] as Token).type == TokenType.MINUS || (parts[1] as Token).type == TokenType.OR || (parts[1] as Token).type == TokenType.XOR)){
                        return (parts[2] as VariableToken)
                    } else if(x == 1 && (parts[1] as Token).type == TokenType.MULTIPLY){
                        return (parts[2] as VariableToken)
                    } else if(x == 0 && ((parts[1] as Token).type in listOf(SHL, SHR, SHRA, AND, MULTIPLY))){
                        return IntegerToken(0)
                    } else {
                        val reg = variables.getRegister((parts[2] as VariableToken).value)
                        when((parts[1] as Token).type){
                            PLUS -> {
                                generator.direct("addi x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            MINUS -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("sub x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            MULTIPLY -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("mul x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            AND -> {
                                generator.direct("andi x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            OR -> {
                                generator.direct("ori x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            XOR -> {
                                generator.direct("xori x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            SHL -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("sll x$treg x$temp $reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            SHR -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("srl x$treg x$temp $reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            SHRA -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("sra x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            DIVIDE, MODULUS -> {
                                fail("Division/Modulus not implemented")
                            }
                        }
                    }
                } catch (e: NumberFormatException){
                    fail("Could not parse an integer literal.")
                }
            } else if(parts[2] is IntegerToken){
                try {
                    val x = (parts[2] as IntegerToken).value.toInt()

                    //plus minus or xor shl shr shra
                    if(x == 0 && ((parts[1] as Token).type in listOf(PLUS, MINUS, OR, XOR, SHL, SHR, SHRA))){
                        return (parts[0] as VariableToken)
                    } else if(x == 0 && ((parts[1] as Token).type == TokenType.MULTIPLY || (parts[1] as Token).type == AND)){
                        return IntegerToken(0)
                    } else if(x == 0 && (parts[1] as Token).type == TokenType.DIVIDE){
                        fail("Divide by zero")
                        return IntegerToken(-1) //unreachable
                    } else if(x == 1 && ((parts[1] as Token).type == TokenType.MULTIPLY || (parts[1] as Token).type == TokenType.DIVIDE)){
                        return (parts[0] as VariableToken)
                    } else {
                        val reg = variables.getRegister((parts[0] as VariableToken).value)
                        when((parts[1] as Token).type){
                            PLUS -> {
                                generator.direct("addi x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            MINUS -> {
                                generator.direct("addi x$treg x$reg -$x")
                                return simpleResult(treg, variables)
                            }
                            MULTIPLY -> {
                                val temp = variables.acquireTemporary()
                                generator.direct("li x$temp $x")
                                generator.direct("mul x$treg x$temp x$reg")
                                variables.free(temp)
                                return simpleResult(treg, variables)
                            }
                            AND -> {
                                generator.direct("andi x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            OR -> {
                                generator.direct("ori x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            XOR -> {
                                generator.direct("xori x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            SHL -> {
                                generator.direct("slli x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            SHR -> {
                                generator.direct("srli x$treg x$reg $x")
                                return simpleResult(treg, variables)
                            }
                            SHRA -> {
                                generator.direct("srai x$treg x$reg x$x")
                                return simpleResult(treg, variables)
                            }
                            DIVIDE, MODULUS -> {
                                fail("Division/Modulus not implemented")
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
                PLUS -> {
                    generator.direct("add x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                MINUS -> {
                    generator.direct("sub x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                MULTIPLY -> {
                    generator.direct("mul x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                AND -> {
                    generator.direct("and x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                OR -> {
                    generator.direct("or x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                XOR -> {
                    generator.direct("xor x$treg x$src0 $src1")
                    return simpleResult(treg, variables)
                }
                SHL -> {
                    generator.direct("sll x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                SHR -> {
                    generator.direct("srl x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                SHRA -> {
                    generator.direct("sra x$treg x$src0 x$src1")
                    return simpleResult(treg, variables)
                }
                DIVIDE, MODULUS -> {
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
                TokenType.IDENTIFIER -> {
                    return (parts[0] as VariableToken)
                }
                else -> fail("Invalid single token: \"${(parts[0] as Token).value}\"")
            }
        }

        fail("Expression not parsable: $this")
        return IntegerToken(-1) //unreachable
    }

    fun add(value: Evaluable){
        parts.add(value)
    }

    override fun toString(): String {
        return "" + evalReg + " <- " + (parts.toString())
    }

    fun fail(msg: String){
        fail("ExpressionBuilder: $msg")
    }
}

/*class AddOp(dest: String, src1: String, src2: String, immediate: Boolean) :
    SimpleOperation(dest, src1, src2, immediate) {

    override fun generate(): String = "add${imm()} $dest $src1 $src2"

    override fun isResolved(): Boolean = false

    override fun unpack(): Token = error("Cannot resolve this operation to a add at compile time.")

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