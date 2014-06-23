---
title: "Constants in JavaScript" 
date: 2011-04-20T14:11:28Z
tags:
  - "javascript"
---

How are constants declared in JavaScript?

You could use the const keyword like so.

    const pi = 3.14;
    document.writeln("pi is roughly"   pi   ".");

But watch out!  The MDN’s [JavaScript Reference][1] says that

 [1]: https://developer.mozilla.org/en/JavaScript/Reference/Statements/const

> `const` is a **Mozilla-specific extension**.

Constants are actually not part of the JavaScript specification and we’ll have to roll our own solution.  We could use the [module  pattern][2] and create accessor functions to our constant data like this.

 [2]: http://www.yuiblog.com/blog/2007/06/12/module-pattern/

    var CONSTANTS = (function() {
      var private = {
        'PI' : '3.14',
      };
    
      return {
        get: function(name) { return private[name]; }
      };
    })();
    
    alert('PI: '   CONSTANTS.get('PI'));

This prevents write access to our data and allows read access through a bit of extra syntax.  Another method is to simply use a convention such as ALL CAPS to define constants. Most programmers know this convention and will respect it.

    var PI = 3.14;

The solution I use is to place all constants in an object literal.

    var CONSTANTS = {
      pi : 3.14
    };

This strikes me as a good compromise between the ALL CAPS convention and the over engineered module pattern solution.  What do you think?
