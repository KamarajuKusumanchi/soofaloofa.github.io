---
title: "Downloading files from Google Cloud Storage with webapp2"
date: 2015-01-27T06:07:12-06:00
tags: 
  - "app engine"
  - "webapp2"
  - "cloud storage"
---

I've been working on a simple App Engine application that offers upload and
download functionality to and from Google Cloud Storage. When it came time to
actually download the content I needed to write a webapp2 `RequestHandler` that
will retrieve the file from Cloud Storage and return it to the client.

<!--more-->

The trick to this is to set the proper content type in your response header. In
the example below I used the [Cloud Storage Client Library][client] to open and
read the file, then set the response appropriately.

```python
import webapp2
import cloudstorage

class FileDownloadHandler(webapp2.RequestHandler):

  def get(self, filename):
    self.response.headers['Content-Type'] = 'application/x-gzip'
    self.response.headers['Content-Disposition'] = 'attachment; filename=%s' % filename

    filename = '/bucket/' + filename
    gcs_file = cloudstorage.open(filename)
    data = gcs_file.read()
    gcs_file.close()

    self.response.write(data)
```

[client]: (https://cloud.google.com/appengine/docs/python/googlecloudstorageclient/)
