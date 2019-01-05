# Biscy
Bitsy to RISC-V compiler ([Bitsy Specification](https://github.com/apbendi/bitsyspec/blob/master/BITSY.md)).  
`PRINT` and `READ` are not supported, instead `WRITE` and `LOAD` have been added:
```
WRITE <variable to write> <address to write to (register or literal)>
LOAD  <variable to load into> <address to read from (register or literal)>
```
Additionally, support for binary (`0b1010`) and hexadecimal (`0xa`) integer literals were added.  
Division and modulus operations are not supported. Division that can be evaluated at compile time is supported (no guarantee), so `x = 42 / 2` should work.  
More than 29 variables are not supported, but you can push variables on the stack (read on).

### Additional Features
```
BEGIN
    x = 42
    { call routines }
    CALL identifier
    { inline assembly - $ident to use variables }
    "addi $x $x 1"
    { FREE to clear a variable }
    FREE x { x no longer used from this point on }

    y = 1337
    z = (1337 << 2) | 7331
    PUSH y z END { push multiple variables onto the stack }
    y = -1
    z = -1
    POP y z END { restore their previous values (order matters!) }

    IFZ y - 1337
        { will be executed }
    END

END

{ Routines: }
DEF identifier
    { ... }
END
```


### Usage
```java -jar release/biscy.jar <input-file.bitsy> [flags]```  

Flags:  
* `v`: verbose output, warnings
* `k`: Keep comments
* `d`: Dump variable/register information