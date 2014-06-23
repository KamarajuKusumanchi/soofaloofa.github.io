---
title: "Safely remove the header from the WordPress TwentyTen theme"
date: 2011-01-18T10:11:12Z
description: "WordPress' new TwentyTen theme is terrific. Well supported, highly modifiable, and with tons of widgets to get the exact layout you desire right out of the box. However, one thing I didn’t like too much was having a big header image on the page. I’m a bit of a minimalist so I wanted to trim down the theme by removing the banner image and the navigation bar. Here's how I did it."
tags:
  - "wordpress"
---

WordPress’ new TwentyTen theme is terrific. Well supported, highly modifiable, and with tons of widgets to get the exact layout you desire right out of the box. However, one thing I didn’t like too much was having a big header image on the page. I’m a bit of a minimalist so I wanted to trim down the theme by removing the banner image and the navigation bar. The first step in pulling off a hack like this is to find out what the header image actually looks like in HMTL. If you view the source of the page (right click on the page and select View Source) you will see a section starting with the words . This sounds like the right place to start. Looking inside this div you can clearly see an tag for the site banner. Another way to view this information is to use the Inspect element tool in Google Chrome or the Firefox extension Firebug. This will show you the exact HTML for any area you click in the page. Very useful for any sort of web design. So, now we know what we are looking for. We could go straight to the TwentyTen theme’s directory (wp-content/themes/twentyten) and edit header.php, deleting the lines responsible for rendering the banner image. But what would happen when the theme is updated? All of our changes would be overwritten. The best thing to do is to create a child theme based on TwentyTen. You can edit the child theme to your heart’s content while leaving the parent theme untouched. Create a directory in the wp-content/themes directory called minimaltwentyten. This is where we will store the files for our child theme. One file is required in this directory: style.css. Go ahead and create this file and with the contents below.

    /*
    Theme Name: MinimalTwentyTen
    Theme URI: http://
    Description: Child Theme TwentyTen
    Author:
    Author URI: http://
    Template: twentyten
    Version: 0.1
    */  
    
    @import url("../twentyten/style.css");

The first few lines between /* and */ are comments that contain metadata about our child theme. You can go ahead and fill this out with whatever you please. None of it is required with the exception of the line

    Template: twentyten

This line tells WordPress the directory to find our parent theme. Next you see the line

    @import url("../twentyten/style.css");

This line tells WordPress to use the parent themes style. Save the file and go ahead and apply our new theme. You will see that our blog looks exactly the same. This is because we are using the exact same style sheet as our parent theme TwentyTen. Let’s go ahead and make some changes. First, let’s get rid of the banner image. We already know that the div is called branding so we can select that with the #branding tag in CSS and use img to select only the image element under the branding div.

    #branding img {
        display: none;
    }

display:none tells the browser not to display this element of the page. We can do exactly the same thing for the navigation bar with the following code

    #access {
        display: none;
    }

Putting this all together, our final style.css file is

    /*
    Theme Name: MinimalTwentyTen
    Theme URI: http://
    Description: Child Theme TwentyTen
    Author:
    Author URI: http://
    Template: twentyten
    Version: 0.1
    */  
    
    @import url("../twentyten/style.css");  
    
    #branding img {
        display: none;
    }  
    
    #access {
        display: none;
    }

Go ahead and apply this theme and you will have safely removed the header and navigation area from the TwentyTen theme.
