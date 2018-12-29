package eu.flybig.biscy

import java.io.File
import java.io.PushbackReader
import java.io.Reader

class FileReader(file: File) : PushbackReader(file.bufferedReader()) {

    var linesRead = 0
    var fileName = file.name

    fun next(): Char {
        val ret = this.read().toChar()
        if(ret == '\n') linesRead++
        return ret
    }

    fun peek(): Char {
        val ret = this.read()
        this.unread(ret)
        return ret.toChar()
    }

    fun takeUntil(matching: (Char) -> Boolean) : String {
        var buffer = ""
        var nextChar = this.next()
        while(!matching.invoke(nextChar)){
            buffer += nextChar
            nextChar = this.next()
        }
        buffer += nextChar
        return buffer
    }

    fun take(matching: (Char) -> Boolean) : String {
        var buffer = ""
        var nextChar = this.peek()
        while(matching.invoke(nextChar)){
            buffer += this.next()
            nextChar = this.peek()
        }
        return buffer
    }

}