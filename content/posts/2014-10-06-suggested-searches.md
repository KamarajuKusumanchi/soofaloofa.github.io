---
title: "Suggested Searches with Google App Engine"
date: 2014-10-06T05:52:29Z
description: "At VendAsta we have a few APIs that are backed by search documents built using the App Engine Search API. These APIs are queried using a search string entered in a text box. One way to improve the user experience of this text box is to offer the user suggestions of popular searches to use as their query. This article describes how to achieve this."
tags: 
  - "search"
  - "app engine"
---

At [VendAsta](http://www.vendasta.com) we have a few APIs that are backed by
search documents built using the [App Engine Search
API](https://cloud.google.com/appengine/docs/python/search/). These APIs are
queried using a search string entered in a text box. One way to improve the user
experience of this text box is to offer the user suggestions of popular searches
to use as their query. This article describes how to achieve this.

## Finding the most likely search terms

Before presenting suggestions to the user we need to collect the data
determining which searches are popular. This data contains the most likely
choice of search term given a prefix (i.e., an incomplete search term). For
example, given the incomplete search term `ne` we need to return the most
frequent searches that have been made using that prefix.

{{% img 2014-10-06-search-suggestions/search1.png "Incomplete Search." %}}

Suppose the user searches for the term `Netflix`. Given the search term we
increment the frequency of a `(prefix, search term)` tuple for each prefix of
the search term `Netflix`. The end result is a datastore model with entries for
each `(prefix, search term)` tuple.

{{% img 2014-10-06-search-suggestions/search2.png "Netflix Search." %}}

If that term `Netflix` is searched for a second time we increment the frequency
count of each `(prefix, search term)` tuple.

{{% img 2014-10-06-search-suggestions/search3.png "Tuples of Netflix." %}}

Now suppose one person searched for the term `news`. We build up our frequency
table with each `(prefix, search term)` tuple again, using `news` as the search
term.

{{% img 2014-10-06-search-suggestions/search4.png "Tuples of news." %}}

Once we've assembled the data we can go back to our original problem of finding
the most likely searches for a given incomplete search. Given our dataset this
is lookup for each record matching our prefix in the dataset ordered by
frequency.

{{% img 2014-10-06-search-suggestions/search5.png "Ordered table." %}}

## A sample implementation

The following is a sample implementation encapsulating the ideas presented
above.

```python
from google.appengine.ext import ndb


class SearchSuggestionModel(ndb.Model):
    """ Model class for scoring of search frequency. """

    created = ndb.DateTimeProperty(auto_now_add=True)
    updated = ndb.DateTimeProperty(auto_now=True)

    prefix = ndb.StringProperty(required=True)
    search_term = ndb.StringProperty(required=True)
    frequency = ndb.IntegerProperty(required=True, default=0)

    @classmethod
    def build_key(cls, prefix, search_term, pid):
        """ Builds a key in the default namespace. """
        id_ = "%s:%s" % (prefix, search_term)
        return ndb.Key(cls, id_, namespace=pid.upper())

    @classmethod
    def prefix_query(cls, prefix, pid):
        """ Return all models with the matching prefix. Ordered by frequency. """
        return cls.query(cls.prefix == prefix, namespace=pid).order(-cls.frequency)

    @classmethod
    def increment(cls, search_term, partner_id):
        """
        Given a search_term, increment each (prefix, search_term) combination for all prefixes of that search_term
        """
        if not search_term:
            return

        entities = []

        for index, _ in enumerate(search_term):
            prefix = search_term[0:index]
            if prefix:
                key = cls.build_key(prefix, search_term, partner_id)
                entity = key.get()
                if entity:
                    entity.frequency = entity.frequency + 1
                else:
                    # Put new entity
                    entity = cls(key=key, prefix=prefix, search_term=search_term, frequency=1)

                entities.append(entity)

        ndb.put_multi(entities)
```
