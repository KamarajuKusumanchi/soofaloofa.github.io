---
title: "Saving canvas data to an image file with JavaScript and PHP"
date: 2011-07-27T10:08:42Z
tags:
  - "javascript"
  - "php"
  - "canvas"
aliases:
  - "posts/2011-07-27-saving-canvas-data-to-an-image-file-with-javascript-and-php/"
---

Saving HTML canvas element data to an image in a user friendly manner is a tricky problem. Let's look at one way to solve it.

<!--more-->

## First Attempt

We could always open our canvas in a new browser tab (or window) with the `toDataURL` JavaScript method.

    window.location.href = canvas.toDataURL("image/png");

Unfortunately this requires the user to use the file menu or the right-click button to save the image from the newly opened browser tab. I wouldn’t call this user friendly.

## Second Attempt

After some investigation I came across Nihilogic’s [`Canvas2Image`][1] JavaScript package. This package presents a Dialog Box to the user allowing them to save the image. This would solve my problem except the downloaded filename has the format 8iqALWM5.part . If my mom encountered a filename like that she wouldn’t know what to do with it. Still not user friendly.

 [1]: http://www.nihilogic.dk/labs/canvas2image/

## Final Attempt

What to do? Enter PHP.

[Permadi][2] presents a technique using PHP and AJAX that is exactly what I need. After some tweaking here’s what I came up with.

 [2]: http://www.permadi.com/blog/2010/10/html5-saving-canvas-image-data-using-php-and-ajax/

## save.php

The first PHP file saves the passed in canvas data to the server at a random location determined by the md5(uniqid())  method.

    $data = $_POST['data'];
    $file = md5(uniqid()) . '.png';
    
    // remove "data:image/png;base64,"
    $uri =  substr($data,strpos($data,",") 1);
    
    // save to file
    file_put_contents($file, base64_decode($uri));
    
    // return the filename
    echo json_encode($file);

We would call this via JQuery with the $.post method, filling the data parameter using the `toDataURL` method.

    $.post("save.php", {data: canvas.toDataURL("image/png")})

## download.php

Now we can use PHP to force the download of the saved image data. You can read more about this in the [PHP Manual][3].

 [3]: http://php.net/manual/en/function.readfile.php

    $file = trim($_GET['path']);
    
    // force user to download the image
    if (file_exists($file)) {
        header('Content-Description: File Transfer');
        header('Content-Type: image/png');
        header('Content-Disposition: attachment; filename='.basename($file));
        header('Content-Transfer-Encoding: binary');
        header('Expires: 0');
        header('Cache-Control: must-revalidate, post-check=0, pre-check=0');
        header('Pragma: public');
        header('Content-Length: ' . filesize($file));
        ob_clean();
        flush();
        readfile($file);
        exit;
    }
    else {
        echo "$file not found";
    }

We can use the return value of our first AJAX request as input to `download.php` to provide the filename.

    $("#save").click(function () {
        $.post("save.php", {data: G_cv.toDataURL("image/png")}, function (file) {
            window.location.href =  "download.php?path="  file});
        });

Now when the `Save` link is clicked a dialog box will be presented to the user asking them to save their image.

User friendly?
