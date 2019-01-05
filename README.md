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

### Usage
```java -jar release/biscy.jar <input-file.bitsy> [flags]```

Flags:
* `v`: verbose output, warnings
* `k`: Keep comments
* `d`: Dump variable/register information

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

### Fibonacci - Recursive and iterative
```
BEGIN
    N = 6 { this fibonacci number will be calculated }

    n = N
    CALL fib_recursive
    WRITE result 0

    n = N
    CALL fib_iterative
    WRITE result 4
END

DEF fib_recursive
    IFZ n
        result = 1
        RETURN
    END
    IFZ n - 1
        result = 1
        RETURN
    END

    result = 0
    PUSH n END
    n = n - 1
    CALL fib_recursive
    POP n END

    PUSH result END
    n = n - 2
    CALL fib_recursive
    temp = result
    POP result END
    result = result + temp
END

DEF fib_iterative
    n = n + 1
    result = 1

    LOOP
        IFN n - 1
            BREAK
        END

        next_fib = result + last_fib
        last_fib = result
        result = next_fib

        n = n - 1
    END
END
```
Full compiler output (kvd):
```
[OUTPUT] # this fibonacci number will be calculated
[OUTPUT] li x5 6
[OUTPUT] add x6 x5 x0
[OUTPUT] jal x1 fib_recursive
[OUTPUT] sw x7 0(x0)
[OUTPUT] add x6 x5 x0
[OUTPUT] jal x1 fib_iterative
[OUTPUT] li x8 4
[OUTPUT] sw x7 0(x8)
[OUTPUT] jal x0 END
[OUTPUT]
[OUTPUT] fib_recursive:
[OUTPUT] 	bne x6 x0 END_IF_0
[OUTPUT] 		li x7 1
[OUTPUT] 		jalr x0 0(x1)
[OUTPUT] 	END_IF_0:
[OUTPUT] 	addi x10 x6 -1
[OUTPUT] 	bne x10 x0 END_IF_1
[OUTPUT] 		li x7 1
[OUTPUT] 		jalr x0 0(x1)
[OUTPUT] 	END_IF_1:
[OUTPUT] 	li x7 0
[OUTPUT] 	addi sp sp -4
[OUTPUT] 	sw x6 0(sp)
[OUTPUT] 	addi x6 x6 -1
[OUTPUT] 	addi sp sp -4
[OUTPUT] 	sw x1 0(sp)
[OUTPUT] 	jal x1 fib_recursive
[OUTPUT] 	lw x1 0(sp)
[OUTPUT] 	addi sp sp 4
[OUTPUT] 	lw x6 0(sp)
[OUTPUT] 	addi sp sp 4
[OUTPUT] 	addi sp sp -4
[OUTPUT] 	sw x7 0(sp)
[OUTPUT] 	addi x6 x6 -2
[OUTPUT] 	addi sp sp -4
[OUTPUT] 	sw x1 0(sp)
[OUTPUT] 	jal x1 fib_recursive
[OUTPUT] 	lw x1 0(sp)
[OUTPUT] 	addi sp sp 4
[OUTPUT] 	add x11 x7 x0
[OUTPUT] 	lw x7 0(sp)
[OUTPUT] 	addi sp sp 4
[OUTPUT] 	add x7 x7 x11
[OUTPUT] jalr x0 0(x1)
[OUTPUT]
[OUTPUT] fib_iterative:
[OUTPUT] 	addi x6 x6 1
[OUTPUT] 	li x7 1
[OUTPUT] 	LOOP_0:
[OUTPUT] 		addi x12 x6 -1
[OUTPUT] 		bgt x12 x0 END_IF_2
[OUTPUT] 			beq x0 x0 END_LOOP_0
[OUTPUT] 		END_IF_2:
[OUTPUT] 		add x13 x7 x14
[OUTPUT] 		add x14 x7 x0
[OUTPUT] 		add x7 x13 x0
[OUTPUT] 		addi x6 x6 -1
[OUTPUT] 	beq x0 x0 LOOP_0
[OUTPUT] 	END_LOOP_0:
[OUTPUT] jalr x0 0(x1)
[OUTPUT]
[OUTPUT] END:
=== Begin Var Dump ===
[VARS] x5	N
[VARS] x6	n
[VARS] x7	result
[VARS] x8	[temp0]
[VARS] x9	[temp1]
[VARS] x10	[temp2]
[VARS] x11	temp
[VARS] x12	[temp3]
[VARS] x13	next_fib
[VARS] x14	last_fib
=== End Var Dump ===
[WARNING] All uppercase identifier N is not a keyword!
[WARNING] All uppercase identifier N is not a keyword!
[WARNING] All uppercase identifier N is not a keyword!
```