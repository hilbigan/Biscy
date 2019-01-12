package eu.flybig.biscy

import java.io.File
import java.lang.Character.isDigit
import java.lang.Character.isWhitespace
import javax.lang.model.SourceVersion.isIdentifier


private const val identifierChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
private const val hexChars = "0123456789ABCDEFabcdef"
private const val binChars = "01"
private const val COMMENT_START = '{'
private const val COMMENT_END = '}'
private val keywords = TokenType.values().filter {
    !it.keyword.isNullOrBlank()
}.map {
    it.keyword!!
}

class Tokenizer(file: File, val options: CompilerOptions){

    var current: Token = CommentToken("illegal") //invalid initial add
        private set(value) {
            field = value
        }

    var inlineAssemblyMode = false

    private val reader: FileReader = FileReader(file)
    private var linesRead = 0

    fun hasNext(): Boolean {
        return reader.ready() && reader.peek() != 65535.toChar()
    }

    fun advance(){
        if(!hasNext()){
            //fail("Unexpected EOF")
            current = EOFToken()
            return
        }
        current = takeNext()
    }

    private fun takeNext(): Token {
        val x = reader.peek()
        when {
            x.isDigit() -> {
                val start = reader.next()
                if(start == '0' && !reader.peek().isWhitespace()){
                    var next = reader.peek()
                    if(next == 'b'){
                        next = reader.next()
                        val raw = reader.take(::isBinLiteral)
                        //if(!reader.peek().isWhitespace()) fail("Illegal character in binary literal: ${reader.peek()}")
                        if(raw.isBlank()) fail("Invalid integer literal: ${"" + start + next}")
                        return IntegerToken(raw.toInt(2).toString())
                    } else if(next == 'x'){
                        next = reader.next()
                        val raw = reader.take(::isHexLiteral)
                        //if(!reader.peek().isWhitespace()) fail("Illegal character in hexadecimal literal: ${reader.peek()}")
                        if(raw.isBlank()) fail("Invalid integer literal: ${"" + start + next}")
                        return IntegerToken(raw.toInt(16).toString())
                    } else {
                        val raw = reader.take(::isDigit)
                        return IntegerToken("" + start + raw)
                    }

                } else return IntegerToken(start + reader.take(::isDigit))
            }
            x.isWhitespace() -> return WhitespaceToken(reader.take(::isWhitespace))
            x == COMMENT_START -> return CommentToken(reader.takeUntil { it == COMMENT_END })
            x.toString() in keywords -> {
                val ident = reader.next().toString()
                return KeywordToken(TokenType.getForKeyword(ident), ident)
            }
            isIdentifier(x) -> {
                val ident = reader.take(::isIdentifier)
                return if(ident in keywords){
                    if(ident == "PRINT" || ident == "READ"){
                        fail("PRINT and READ keywords are not supported.")
                    }
                    KeywordToken(TokenType.getForKeyword(ident), ident)
                } else {
                    if(ident.toUpperCase() == ident && options.outputVerbose){
                        warn("All uppercase identifier $ident is not a keyword!")
                    }
                    VariableToken(ident)
                }
            }
             x in "<>" -> {
                 val shift = reader.take { it in "<>" }
                 if(shift in keywords){
                     return KeywordToken(TokenType.getForKeyword(shift), shift)
                 } else {
                     fail("Illegal Character: $x (${x.toInt()})")
                     return CommentToken("illegal") //unreachable
                 }
             }
            else -> {
                if(inlineAssemblyMode){
                    warn("Illegal character $x was ignored in inline assembly section!")
                    return KeywordToken(TokenType.IDENTIFIER, "${reader.next()}")
                }
                fail("Illegal Character: $x (${x.toInt()})")
                return CommentToken("illegal") //unreachable
            }
        }
    }

    private fun isHexLiteral(char: Char): Boolean {
        return char in hexChars
    }

    private fun isBinLiteral(char: Char): Boolean {
        return char in binChars
    }

    private fun isIdentifier(char: Char): Boolean {
        return char in identifierChars
    }

    fun fail(msg: String){
        System.err.println("[ERROR] $msg")
        System.err.println("at ${reader.fileName} line ${reader.linesRead + 1}")
        System.exit(1)
    }

}

interface Evaluable

abstract class Token(val type: TokenType, val value: String) : Evaluable

class VariableToken(value: String) : Token(TokenType.IDENTIFIER, value)

class KeywordToken(type: TokenType, value: String) : Token(type, value)

class IntegerToken(value: String) : Token(TokenType.INTEGER, value) {

    constructor(value: Int) : this(value.toString())

}

class EOFToken : Token(TokenType.WHITESPACE, "EOF")

class WhitespaceToken(value: String) : Token(TokenType.WHITESPACE, value)

class CommentToken(value: String) : Token(TokenType.COMMENT, value)

enum class TokenType(val keyword: String?) {
    WHITESPACE  (null),
    IDENTIFIER  (null),
    INTEGER     (null),
    COMMENT     (null),
    ASM         ("\""),
    DOLLAR      ("$"),
    LPAREN      ("("),
    RPAREN      (")"),
    AND         ("&"),
    OR          ("|"),
    XOR         ("^"),
    SHL         ("<<"),
    SHR         (">>"),
    SHRA        (">>>"),
    PLUS        ("+"),
    MINUS       ("-"),
    MULTIPLY    ("*"),
    DIVIDE      ("/"), //unsupported
    MODULUS     ("%"), //unsupported
    ASSIGNMENT  ("="),
    BEGIN       ("BEGIN"),
    END         ("END"),
    IFP         ("IFP"),
    IFZ         ("IFZ"),
    IFN         ("IFN"),
    ELSE        ("ELSE"),
    LOOP        ("LOOP"),
    BREAK       ("BREAK"),
    CONTINUE    ("CONTINUE"),
    PRINT       ("PRINT"),
    WRITE       ("WRITE"),
    LOAD        ("LOAD"),
    ROUTINE     ("DEF"),
    CALL        ("CALL"),
    RETURN      ("RETURN"),
    STACK       ("PUSH"),
    UNSTACK     ("POP"),
    FREE        ("FREE"),
    READ        ("READ");

    companion object {
        fun getForKeyword(str: String): TokenType {
            return TokenType.values().first { it.keyword == str }
        }
    }
}