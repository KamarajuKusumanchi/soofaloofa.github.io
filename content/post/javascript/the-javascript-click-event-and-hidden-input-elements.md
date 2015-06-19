---
title: "The JavaScript click event and hidden input elements" 
date: 2011-06-02T16:42:48Z
tags:
  - "javascript"
aliases:
  - "posts/2011-06-02-the-javascript-click-event-and-hidden-input-elements/"
---

I was recently working with the HTML canvas element and wanted to attach an event to the canvas that would fire the click event of a file input element. I would then hide the input element so that the canvas element was the only way to browse for files. 

<!--more-->

My first attempt was simple enough.

    document.getElementById('canvas').onclick = function(e) {
        document.getElementById('filepicker').click();
    };

Now it’s time to hide the filepicker input element. Setting the elements style to display: none worked fine with Firefox but Chrome does not allow click events to be fired from hidden HTML elements. The solution is to use a bit of CSS to effectively hide the element. I came across this method over at [quirksmode][1] where they use it to style input boxes with CSS. I adapted it to my needs and came up with this solution.

 [1]: http://www.quirksmode.org/dom/inputfile.html

With CSS we set the opacity to `0` rendering the element invisible. The different declarations are for cross-browser compatibility.

    .fakehidden {
        -moz-opacity: 0;
        filter:alpha(opacity: 0);
        opacity: 0;
    }

Setting the input elements class `tofakehidden` effectively hides the element from the user yet click events can be fired as usual.
