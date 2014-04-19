---
layout: post
title: How to Display HTML5 Notifications
author: Kevin Sookocheff
date: 2011/05/06 10:10:10
tags:
  - html
  - notifications
  - chrome
  - javascript
---

Chrome recently introduced support for HTML Notifications.

Following the brief tutorial at [HTML5 Rocks][1] I was able to implement this in a matter of minutes and I couldn’t be happier with the results.

 [1]: http://www.html5rocks.com/tutorials/notifications/quick/

The first step towards implementation is to check if the Notifications API is implemented by the browser. We do this by checking for the existence of the webkitNotifications object.

    // check for notifications support
    if (!window.webkitNotifications) {
        alert('Your browser does not support the Notifications API');
    }

As a means of preventing unwanted notifications we request permission from the user before displaying anything. The method `webkitNotifications.checkPermission()` will return zero if permission has previously been granted. If it returns non-zero we can request permission with the method `webkitNotifications.requestPermission()`.

    // request permission to use notifications
      if (window.webkitNotifications) {
        if (window.webkitNotifications.checkPermission() > 0) {
                window.webkitNotifications.requestPermission();
        }
    }

Now that we have requested permission we can display our notification. We use the function `webkitNotifications.createNotification()` to create a Notification object. This function takes three parameters: an icon to display, a title and text. We then call the `show()` method on our Notification object to display our notification.

    // display a notification
    if (window.webkitNotifications && window.webkitNotifications.checkPermission() == 0) {
        window.webkitNotifications.createNotification(
            'apple-touch-icon.png',
            'Awesome!',
            "You've completed a Pomodoro!").show();
    }

The Notifications specification is currently in Draft format and has been submitted to the HTML working group for standardization. You can see the current spec through the [Chromium documentation][2].

 [2]: http://www.chromium.org/developers/design-documents/desktop-notifications/api-specification
