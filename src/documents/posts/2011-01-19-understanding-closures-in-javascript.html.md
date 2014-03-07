---
layout: post
title: Understanding Closures in JavaScript
author: Kevin Sookocheff
date: 2011/01/19
tags:
  - Technology
---

Closures are easy. There I said it. Invest a little bit of time, and you will say it too. Follow along with me as I attempt to explain what closures are and how they are used.

### Scope

The first pre-requisite in fully understanding closures is JavaScript’s implementation of scope. JavaScript’s C-style syntax may lead you to believe that anything between the “curly braces” defines a block where variables defined within that block are private to that region of code. This is false. JavaScript does not define block scope. Any variables defined within a block are available outside that block. With one exception: functions. JavaScript has function scope where variables defined within a function are private to that function. This is very important.

#### Summary

*   JavaScript has function scope.
*   All variables defined within a function are private to that function.

### Inner Functions

Inner functions are functions defined within another function. It’s easier to demonstrate than to explain, so here goes.

    var outer = function () {
      var secrets = 3;
      var inner = function () {
        return secrets;
      }        
    
      return inner;
    }   
    
    output = outer();
    alert(output);

You will get a pop-up window showing that the variable secrets now holds a reference to the function inner.

#### Summary

*   Inner functions are defined within other functions (and this can nest arbitrarily).
*   Inner functions can be returned “fully formed” from outer functions.

### Closures

In one sentence, closure means that an inner function has access to the context of its outer function, even when the outer function no longer exists. OK. Let’s do that example one time more.

    var outer = function () {
      var secrets = 3;
      var inner = function () {
        return secrets;
      }
    
      return inner;
    }
    
    output = outer();
    alert(output());

Notice the difference? This time we are executing the inner function through the statement output(). Now you will get a pop-up window showing that the variable secrets is still available to the inner function, even when the outer function no longer exists.

You’ve just created a closure!

#### Summary

*   Closures are easy.
*   You’ve just scratched the surface.

Now What?

I haven’t even begun to explain what is capable with closures. Not to mention a handful of caveats to watch out for. Hopefully you understand a bit more about closures and are now willing to dive deeper. Check out the following resources for much, much more:

[JavaScript: The Good Parts][1]

[JavaScript Closures][2]

[Private Members in JavaScript][3]

 [1]: http://www.amazon.com/JavaScript-Good-Parts-Douglas-Crockford/dp/0596517742
 [2]: http://jibbering.com/faq/notes/closures/
 [3]: http://www.crockford.com/javascript/private.html
