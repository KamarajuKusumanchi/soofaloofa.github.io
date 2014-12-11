---
title: "The Bash String Operators"
date: 2014-12-11T19:54:12Z
tags: 
  - "bash"
---

A common task in *bash* programming is to manipulate portions of a string and
return the result. *bash* provides rich support for these manipulations via
string operators. The syntax is not always intuitive so I wanted to use this
blog post to serve as a permanent reminder of the operators.

The string operators are signified with the `${}` notation. The operations can be
grouped in to a few classes. Each heading in this article describes a class of
operation.

## Substring Extraction

---

### Extract from a position

```bash
${string:position}
```

Extraction returns a substring of `string` starting at `position` and ending at the end of `string`. `string` is treated as an array of characters starting at 0.

```bash
> string="hello world"
> echo ${string:1}
ello world
> echo ${string:6}
world
```

---

### Extract from a position with a length

```bash
${string:position:length}
```

Adding a length returns a substring only as long as the `length` parameter.

```bash
> string="hello world"
> echo ${string:1:2}
el
> echo ${string:6:3}
wor
```

## Substring Removal

---

### Remove shortest starting match

```bash
${variable#pattern}
```

If `variable` *starts* with `pattern`, delete the *shortest* part that matches the pattern.

```bash
> string="hello world, hello jim"
> echo ${string#*hello}
world, hello jim
```

---

### Remove longest starting match

```bash
${variable##pattern}
```

If `variable` *starts* with `pattern`, delete the *longest* match from `variable` and return the rest.

```bash
> string="hello world, hello jim"
> echo ${string##*hello}
jim
```

---

### Remove shortest ending match

```bash
${variable%pattern}
```

If `variable` ends with `pattern`, delete the longest match from the end of `variable` and return the rest.

```bash
> string="hello world, hello jim"
> echo ${string%hello*}
hello world,
```

---

### Remove longest ending match

```bash
${variable%%pattern}
```

If `variable` ends with `pattern`, delete the longest match from the end of `variable` and return the rest.

```bash
> string="hello world, hello jim"
> echo ${string%%hello*}

```

## Substring Replacement

---

### Replace first occurrence of word

```bash
${variable/pattern/string}
```

Find the first occurrence of `pattern` in `variable` and replace it with `string`. If `string` is null, `pattern` is deleted from `variable`. If `pattern` starts with `#`, the match must occur at the beginning of `variable`. If `pattern` starts with `%`, the match must occur at the end of the `variable`.

```bash
> string="hello world, hello jim"
> echo ${string/hello/goodbye}
goodbye world, hello jim
```

---

### Replace all occurrences of word

```bash
${variable//pattern/string}
```

Same as above but finds **all** occurrences of `pattern` in `variable` and replace them with `string`. If `string` is null, `pattern` is deleted from `variable`.

```bash
> string="hello world, hello jim"
> echo ${string//hello/goodbye}
goodbye world, goodbye jim
```
