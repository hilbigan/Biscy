package eu.flybig.biscy

const val USABLE_REGISTERS = 29

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
            val idx = variables.indexOf(variables.find { it == null })
            if(idx == -1){
                System.err.println("[ERROR] Too many variables! Currently only $USABLE_REGISTERS variables are supported!")
                System.exit(1)
            }
            variables[idx] = ident
            idx
        })
    }

    fun getIdentifier(register: Int): String {
        if(variables[registerToIndex(register)] != null){
            return variables[registerToIndex(register)]!!
        } else {
            System.err.println("[ERROR] Unused register ${registerToIndex(register)}")
            System.exit(1)
            return "" //unreachable
        }
    }

    fun acquireTemporary(): Int {
        return getRegister("[temp${temps++}]")
    }

    fun free(register: Int){
        variables[registerToIndex(register)] = null
    }

    private fun indexToRegister(idx: Int): Int {
        return idx + 2
    }

    private fun registerToIndex(reg: Int): Int {
        return reg - 2
    }

    fun dump() {
        println("=== Begin Var Dump ===")
        variables.filter { it != null }.forEach {
            println("[VARS] x${getRegister(it!!)}\t$it")
        }
        println("=== End Var Dump ===")
    }
}