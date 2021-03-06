# Biscy (toy compiler project)
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

### Short doc

| Token | Description | 
| --- | --- |
| IDENTIFIER | can contain underscores, any letters. No numbers. | 
INTEGER   |  1, -1, 0x1, 0b1, ...  
COMMENT   |  { Comment }  
"ASM"    |   "inline assembly section". Use variables by inserting them with prepended '$'. (e.g. "addi $var x0 0")  
$         |  see above  
BEGIN     |  Starts a program and the "main"-block at the same time  
END       |  Ends any block, function, loop, if-stmt, or the program.  
IFP       |  if expr positive. Syntax: IFP <expr> ... END  
IFZ       |  if expr zero  
IFN       |  if expr negative  
IFNZ      |  if expr not zero  
ELSE      |  starts else block. Syntax: IFZ 1 ... ELSE ... END  
LOOP      |  starts an infinite loop  
BREAK      | breaks out of an loop  
CONTINUE   | skips to loop header  
WRITE      | WRITE <variable to write> <address to write to (register or literal)>  
LOAD      |  LOAD  <variable to load into> <address to read from (register or literal)>  
DEF       |  starts a function. Syntax: DEF identifier ... END  
CALL      |  calls a function. Syntax: CALL identifier  
RETURN    |  return from a function  
PUSH      |  push variable to stack  
POP       |  pop variable from stack 
FREE      |  frees the memory allocated for a variable (currently -> frees register), effectively making it invalid.  
    
Supported operators:

| Name | Char |
| --- | --- |
LPAREN   |   ("("),  
RPAREN   |   (")"),  
AND      |   ("&"),  
OR       |   ("|"),  
XOR      |   ("^"),  
SHL       |  ("<<"),  
SHR       |  (">>"),  
SHRA      |  (">>>"),  
PLUS      |  ("+"),  
MINUS     |  ("-"),  
MULTIPLY  |  ("*"),  


### Additional Features - Demo
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
[OUTPUT] 	addi x9 x6 -1
[OUTPUT] 	bne x9 x0 END_IF_1
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
[OUTPUT] 	add x9 x7 x0
[OUTPUT] 	lw x7 0(sp)
[OUTPUT] 	addi sp sp 4
[OUTPUT] 	add x7 x7 x9
[OUTPUT] jalr x0 0(x1)
[OUTPUT] 
[OUTPUT] fib_iterative:
[OUTPUT] 	addi x6 x6 1
[OUTPUT] 	li x7 1
[OUTPUT] 	LOOP_0:
[OUTPUT] 		addi x9 x6 -1
[OUTPUT] 		bgt x9 x0 END_IF_2
[OUTPUT] 			beq x0 x0 END_LOOP_0
[OUTPUT] 		END_IF_2:
[OUTPUT] 		add x9 x7 x10
[OUTPUT] 		add x10 x7 x0
[OUTPUT] 		add x7 x9 x0
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
[VARS] x8	[temp1]
=== End Var Dump ===
[WARNING] All uppercase identifier N is not a keyword!
[WARNING] Variable temp is declared inside a function ('fib_recursive', line 32). Remember to FREE this to save space!
[WARNING] Variable next_fib is declared inside a function ('fib_iterative', line 49). Remember to FREE this to save space!
```
