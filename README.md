# Biscy
Bitsy to RISC-V compiler ([Bitsy Specification](https://github.com/apbendi/bitsyspec/blob/master/BITSY.md)).
`PRINT` and `READ` are not supported, instead `WRITE` and `LOAD` have been added:
```
WRITE <variable to write> <address to write to (register or literal)>
LOAD  <variable to load into> <address to read from (register or literal)>
```
Additionally, support for binary (`0b1010`) and hexadecimal (`0xa`) integer literals were added.
Division and modulus operations are not supported.
More than 29 variables are not supported.

### Usage
```java -jar release/biscy.jar <input-file.bitsy> [flags]```
Flags:
* `v`: verbose output, warnings
* `k`: Keep comments
* `d`: Dump variable/register information

