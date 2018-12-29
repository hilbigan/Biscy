package eu.flybig.biscy

const val USABLE_REGISTERS = 29
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
            variables[idx] = ident
            idx
        })
    }

    fun getIdentifier(register: Int): String {
        if(variables[registerToIndex(register)] != null){
            return variables[registerToIndex(register)]!!
        } else {
            error("Unused register ${registerToIndex(register)}") //TODO replace with fail
        }
    }

    fun acquireTemporary(): Int {
        return getRegister("[temp${temps++}]")
    }

    fun free(register: Int){
        variables[registerToIndex(register)] = null
    }

    /*fun releaseAllTemporary(){
        val temp = variables.filter { !it.startsWith("[temp") }.toMutableList()
        variables.clear()
        variables.addAll(temp)
    }*/

    private fun isInMemory(ident: String): Boolean {
        return variables.indexOf(ident) >= USABLE_REGISTERS
    }

    private fun indexToRegister(idx: Int): Int {
        return idx + 2
    }

    private fun registerToIndex(reg: Int): Int {
        return reg - 2
    }

    private fun indexToMemory(idx: Int): Int {
        return BASE_ADDRESS + idx * 4
    }
}