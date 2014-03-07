---
layout: post
title: "Dissecting jQuery's Fade Animation"
author: Kevin Sookocheff
date: 2011/05/18
tags:
  - Technology
---

Fade animations are a standard tool in any jQuery developer’s toolbox. But how do they really work? Let’s create a small function that encapsulates solely the fade functionality and find out.

Let’s start with the jQuery `$` idiom.

    function $(id) {
        var $ = document.getElementById(id);
        return $;
    }

This function takes an HTML id as a parameter and returns the element associated with this id. We can call this function with a string to return the particular element we need.

    $('img');

Let’s add a function to our element.

    function $(id) {
        var $ = document.getElementById(id);
    
        $.fade = function () {
            // perform fade here
        };
    
        return $;
    }

We can call the function by appending `.fade` to our element.

    $('img').fade();

Now let’s set up the rest of the function by adding a parameter for the length of the delay and storing a reference to `this`. In our context `this` refers to the element we are calling fade from. (For a detailed explanation of how `this` is set see the [JavaScript, JavaScript][1] article [here][2]). Finally, we set the display style of our element to `block` to ensure our animation is visible.

 [1]: http://javascriptweblog.wordpress.com
 [2]: http://javascriptweblog.wordpress.com/.../understanding-javascripts-this/

    $.fade = function (delay) {
        var _this = this;
    
        _this.style.display = 'block';
    
        // perform fade here
    };

We perform the fade by setting the opacity of our element. For this purpose let’s define a new function for setting the opacity of an element `elm` to `v`. We divide by 100 to define our fade in terms of a percentage.

    var opacityTo = function (elm, v) {
        elm.style.opacity = v/100;
    };

We can add a call to this function to our fade method.

    $.fade = function (delay) {
        var _this = this;
    
        _this.style.display = 'block';
    
        // set the opacity to 50%
        opacityTo(_this, 50);
    };

Now the trick is to call this function to fade the element over a period of time. We do this by using the `setTimeout(callbk, delay)` function to set the opacity to different values at particular moments in time. The method signature defines a callbk parameter as a function to call when delay time has passed. If we call this function 100 times throughout our animation we will have a smooth and fluid result.

First we set up the `for` loop to iterate from 1 to 100 and then call an anonymous function sending the index of our loop as a parameter to the function. Our anonymous function then calls `setTimeout` periodically (100 times in total).

