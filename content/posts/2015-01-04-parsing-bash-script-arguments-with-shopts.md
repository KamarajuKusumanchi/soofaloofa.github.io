---
title: "Parsing bash script options with getopts"
date: 2015-01-04T12:31:51
tags: 
  - "bash"
  - "getopts"
---

A common task in shell scripting is to parse command line arguments to your
script. Bash provides the `getopts` built-in function to do just that. This
tutorial explains how to use the `getopts` built-in function to parse arguments and options to a bash script. 

<!--more-->

The `getopts` function takes three parameters. The first is a specification of
which options are valid, listed as a sequence of letters. For example, the
string `'ht'` signifies that the options `-a` and `-l` are valid.

The second argument to `getopts` is a variable that will be populated with the
option or argument to be processed next. In the following loop, `opt` will hold
the value of the current option that has been parsed by `getopts`. 

```bash
while getopts ":ht" opt; do
  case ${opt} in
    h ) # process option a
      ;;
    t ) # process option l
      ;;
    \? ) echo "Usage: cmd [-h] [-t]
      ;;
  esac
done
```

This example shows a few additional features of `getopts`. First, if an invalid
option is provided, the option variable is assigned the value `?`. You can catch
this case and provide an appropriate usage message to the user. Second, this
behaviour is only true when you prepend the list of valid options with `:` to
disable the default error handling of invalid options. It is recommended to
always disable the default error handling in your scripts. 

The third argument to `getopts` is the list of arguments and options to be
processed. When not provided, this defaults to the arguments and options
provided to the application (`$@`). You can provide this third argument to use
`getopts` to parse any list of arguments and options you provide.

## Shifting processed options

The variable `OPTIND` holds the number of options parsed by the last call to
`getopts`. It is common practice to call the `shift` command at the end of your
processing loop to remove options that have already been handled from `$@`.

```bash
shift $((OPTIND -1))
```

## Parsing options with arguments

Options that themselves have arguments are signified with a `:`. The argument to
an option is placed in the variable `OPTARG`. In the following example, the
option `t` takes an argument. When the argument is provided, we copy its value
to the variable `target`. If no argument is provided `getopts` will set `opt` to
`:`. We can recognize this error condition by catching the `:` case and printing
an appropriate error message.

```bash
while getopts ":t:" opt; do
  case ${opt} in 
    t )
      target=$OPTARG
      ;;
    \? )
      echo "Invalid option: $OPTARG" 1>&2
      ;;
    : )
      echo "Invalid option: $OPTARG requires an argument" 1>&2
      ;;
  esac
done
shift $((OPTIND -1))
```

## An extended example -- parsing nested arguments and options

Let's walk through an extended example of processing a command that takes
options, has a sub-command, and whose sub-command takes an additional option
that has an argument. This is a mouthful so let's break it down using an
example. Let's say we are writing our own version of the [`pip`
command](https://pip.pypa.io/en/latest/). In this version you can call `pip`
with the `-h` option to display a help message.

```bash
> pip -h
Usage: 
    pip -h                      Display this help message.
    pip install                 Install a Python package.
```

We can use `getopts` to parse the `-h` option with the following `while` loop.
In it we catch invalid options with `\?` and `shift` all arguments that have
been processed with `shift $((OPTIND -1))`.

```bash
while getopts ":h" opt; do
  case ${opt} in
    h )
      echo "Usage:"
      echo "    pip -h                      Display this help message."
      echo "    pip install                 Install a Python package."
      exit 0
      ;;
    \? )
      echo "Invalid Option: -$OPTARG" 1>&2
      exit 1
      ;;
  esac
done
shift $((OPTIND -1))
```

Now let's add the sub-command `install` to our script.  `install` takes as an
argument the Python package to install.

```bash
> pip install urllib3
```

`install` also takes an option, `-t`. `-t` takes as an argument the location to
install the package to relative to the current directory.

```bash
> pip install urllib3 -t ./src/lib
```

To process this line we must find the sub-command to execute. This value is the
first argument to our script. 

```
subcommand=$1
shift # Remove `pip` from the argument list
```

Now we can process the sub-command `install`. In our example, the option `-t` is
actually an option that follows the package argument so we begin by removing
`install` from the argument list and processing the remainder of the line.

```bash
case "$subcommand" in
  install)
    package=$1
    shift # Remove `install` from the argument list
    ;;
esac
```

After shifting the argument list we can process the remaining arguments as if
they are of the form `package -t src/lib`. The `-t` option takes an argument
itself. This argument will be stored in the variable `OPTARG` and we save it to
the variable `target` for further work.

```bash
case "$subcommand" in
  install)
    package=$1
    shift # Remove `install` from the argument list

  while getopts ":t:" opt; do
    case ${opt} in
      t )
        target=$OPTARG
        ;;
      \? )
        echo "Invalid Option: -$OPTARG" 1>&2
        exit 1
        ;;
      : )
        echo "Invalid Option: -$OPTARG requires an argument" 1>&2
        exit 1
        ;;
    esac
  done
  shift $((OPTIND -1))
  ;;
esac
```

Putting this all together, we end up with the following script that parses
arguments to our version of `pip` and its sub-command `install`.


```bash
package=""  # Default to empty package
target=""  # Default to empty target

# Parse options to the `pip` command
while getopts ":h" opt; do
  case ${opt} in
    h )
      echo "Usage:"
      echo "    pip -h                      Display this help message."
      echo "    pip install <package>       Install <package>."
      exit 0
      ;;
   \? )
     echo "Invalid Option: -$OPTARG" 1>&2
     exit 1
     ;;
  esac
done
shift $((OPTIND -1))

subcommand=$1; shift  # Remove 'pip' from the argument list
case "$subcommand" in
  # Parse options to the install sub command
  install)
    package=$1; shift  # Remove 'install' from the argument list

    # Process package options
    while getopts ":t:" opt; do
      case ${opt} in
        t )
          target=$OPTARG
          ;;
        \? )
          echo "Invalid Option: -$OPTARG" 1>&2
          exit 1
          ;;
        : )
          echo "Invalid Option: -$OPTARG requires an argument" 1>&2
          exit 1
          ;;
      esac
    done
    shift $((OPTIND -1))
    ;;
esac
```

After processing the above sequence of commands, the variable `package` will
hold the package to install and the variable `target` will hold the target to
install the package to. You can use this as a template for processing any set of
arguments and options to your scripts.
