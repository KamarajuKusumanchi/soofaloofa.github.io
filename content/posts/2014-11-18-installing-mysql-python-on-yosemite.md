---
title: "Installing MySQL-Python on OS X Yosemite"
date: 2014-11-18T11:15:23Z
tags: 
  - "MySQL"
  - "MariaDB"
  - "Cloud SQL"
---

Installing the MySQL-Python package requires a few steps. In an effort to aid
future Internet travellers, this post will document how to install the
MySQL-Python package on OS X Yosemite.

First, install MariaDB, the drop-in replacement for MySQL. I chose MacPorts for
this task, though Homebrew would work just fine. Second, update your PATH to
include the mariadb executables. Third, install the Python MySQL connector.

```bash
sudo port install mariadb.
PATH=/opt/local/lib/mariadb/bin:$PATH
pip install MySQL-Python
```

That's it! You should be able to `import MySQLdb` in your Python code and
interact with your MariaDB database.
