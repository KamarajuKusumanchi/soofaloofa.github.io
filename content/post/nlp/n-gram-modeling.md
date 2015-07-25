---
title: "Modeling Natural Language with N-Gram Models" 
date: 2015-07-25T06:41:06Z
tags:
  - "n-gram"
  - "prediction"
  - "language modeling"
---

One of the most widely used methods natural language is n-gram modeling. This
article explains what an n-gram model is, how it is computed, and what the
probabilities of an n-gram model tell us.

<!--more-->

## What is an n-gram?


{{< quote "An n-gram is a contiguous sequence of n items from a given sequence of text." "Wikipedia" "https://en.wikipedia.org/wiki/N-gram" >}}

Given a sentence, `s`, we can construct a list of n-grams from `s` by finding
pairs of words that occur next to each other. For example, given the sentence "I
am Sam" you can construct bigrams (n-grams of length 2) by finding consecutive
pairs of words.

```python
>>> s = "I am Sam."
>>> tokens = s.split(" ")
>>> bigrams = [(tokens[i],tokens[i+1]) for i in range(0,len(tokens)-1)]
>>> bigrams
[('I', 'am'), ('am', 'Sam.')]
```

## Calculating n-gram Probability

Given a list of n-grams we can count the number of occurrences of each n-gram;
this count determines the frequency with which an n-gram occurs throughout our
document.

```python
>>> from collections import Counter
>>> count = Counter(bigrams)
>>> count
[(('am', 'Sam.'), 1), (('I', 'am'), 1)]
```

With this small corpus we only count one occurrence of each n-gram. By dividing
these counts by the size of all n-grams in our list we would get a probability
of 0.5 of each n-gram occurring.

Let's look a larger corpus of words and see what the probabilities can tell us.
The following sequence of bigrams was computed from data downloaded from [HC
Corpora](http://www.corpora.heliohost.org/). It lists the 20 most frequently
encountered bigrams out of 97,810,566 bigrams in the entire corpus.

This data represents the most frequently used pairs of words in the corpus along
with the number of times they occur.

```dns
of	the	421560
in	the	380608
to	the	207571
for	the	190683
on	the	184430
to	be	153285
at	the	128980
and	the	114232
in	a	109527
with	the	99141
is	a	99053
for	a	90209
from	the	82223
with	a	78918
will	be	78049
of	a	78009
I	was	76788
I	have	76621
going	to	75088
is	the	70045
```

By consulting our frequency table of bigrams, we can tell that the sentence
`There was heavy rain last night` is much more likely to be grammatically
correct than the sentence `There was large rain last night` by the fact that the
bigram `heavy rain` occurs much more frequently than `large rain` in our corpus.
Said another way, the probability of the bigram `heavy rain` is larger than the
probability of the bigram `large rain`.

## Sentences as probability models

More precisely, we can use n-gram models to derive a probability of the sentence
,`W`, as the joint probability of each individual word in the sentence, `wi`.

```dns
P(W) = P(w1, w2, ..., wn)
```

This can be reduced to a sequence of n-grams using the Chain Rule of
conditional probability.

```dns
P(x1, x2, ..., xn) = P(x1)P(x2|x1)...P(xn|x1,...xn-1)
```

As a concrete example, let's predict the probability of the sentence `There was
heavy rain`.

```dns
P('There was heavy rain') = P('There', 'was', 'heavy', 'rain')
P('There was heavy rain') = P('There')P('was'|'There')P('heavy'|'There was')P('rain'|'There was heavy')
```

Each of the terms on the right hand side of this equation are n-gram
probabilities that we can estimate using the counts of n-grams in our corpus. To
calculate the probability of the entire sentence, we just need to lookup the
probabilities of each component part in the conditional probability.

Unfortunately, this formula does not scale since we cannot compute n-grams of
every length. For example, consider the case where we have solely bigrams in our
model; we have no way of knowing the probability `P('rain'|'There was') from
bigrams.

By using the [Markov Assumption](https://en.wikipedia.org/wiki/Markov_property),
we can simplify our equation by assuming that future states in our model only
depend upon the present state of our model. This assumption means that we can
reduce our conditional probabilities to be approximately equal so that

```dns
P('rain'|'There was heavy') ~ P('rain'|'heavy')
```

More generally, we can estimate the probability of a sentence by the
probabilities of each component part. In the equation that follows, the
probability of the sentence is reduced to the probabilities of the sentence's
individual bigrams.

```dns
P('There was heavy rain') ~ P('There')P('was'|'There')P('heavy'|'was')P('rain'|'heavy')
```

## Applications

What can we use n-gram models for? Given the probabilities of a sentence we can
determine the likelihood of an automated machine translation being correct, we
could predict the next most likely word to occur in a sentence, we could
automatically generate text from speech, automate spelling correction, or
determine the relative sentiment of a piece of text. 
