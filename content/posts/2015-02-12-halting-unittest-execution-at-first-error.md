---
title: "Halting Python unittest Execution on First Error"
date: 2015-02-12T06:20:37-06:00
tags: 
  - "python"
  - "unittest"
---

We all know the importance of unit tests. Especially in a dynamic language like
Python. Occasionally you have a set of unit tests that are failing in a
cascading fashion where the first error case causes subsequent tests to fail
(these tests are likely no longer unit tests, but that's a different
 discussion). To help isolate the offending test case in a see of failures you
can set the `unittest.TestCase` class to halt after the first error by
overriding the `run` method as follows.

```python
class HaltingTestCase(unittest.TestCase):

    def run(self, result=None):
        """ Stop after first error """
        if not result.errors:
            super(HaltingTestCase, self).run(result)
```

In this block of code, if we do not have any errors we call the super class to
continue running tests. If we have an error execution stops after this method
call. This allows you to pinpoint the first error case, fix it, and continue on
fixing subsequent tests.
