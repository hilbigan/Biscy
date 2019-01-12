package eu.flybig.biscy

const val USABLE_REGISTERS = 26

//TODO support > 29 variables
const val BASE_ADDRESS = 0x1C00_0000

class Variables {

    //index -> register no.
    //name -> identifier
    private val variables = Array<String?>(USABLE_REGISTERS) { null }
    private var temps = 0

    fun getRegister(ident: String): Int {
        return indexToRegister(if(variables.contains(ident)){
            variables.indexOf(ident)
        } else {
            val idx = if(ident.startsWith("[temp")){
                variables.indexOf(variables.findLast { it == null })
            } else variables.indexOf(variables.find { it == null })
            if(idx == -1){
                fail("Too many variables! Currently only $USABLE_REGISTERS variables are supported!")
            }
            variables[idx] = ident
            idx
        })
    }

    fun getIdentifier(register: Int): String {
        if(variables[registerToIndex(register)] != null){
            return variables[registerToIndex(register)]!!
        } else {
            fail("Unused register ${registerToIndex(register)}")
            return "" //unreachable
        }
    }

    fun isTemporary(register: Int): Boolean {
        return getIdentifier(register).startsWith("[temp")
    }

    fun acquireTemporary(): Int {
        return getRegister("[temp${temps++}]")
    }

    fun free(register: Int){
        variables[registerToIndex(register)] = null
    }

    private fun indexToRegister(idx: Int): Int {
        return idx + 5
    }

    private fun registerToIndex(reg: Int): Int {
        return reg - 5
    }

    fun isDeclared(ident: String): Boolean {
        return variables.contains(ident)
    }

    fun dump() {
        println("=== Begin Var Dump ===")
        variables.filter { it != null }.forEach {
            println("[VARS] x${getRegister(it!!)}\t$it")
        }
        println("=== End Var Dump ===")
    }
}