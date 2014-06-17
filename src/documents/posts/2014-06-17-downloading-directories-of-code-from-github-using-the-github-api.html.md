---
layout: post
standalone: true
title: "Downloading directories of code from Github using the Github API"
author: Kevin Sookocheff
date: 2014/06/17 06:14:23
description: <t render="markdown">At [VendAsta](http://www.vendAsta.com) we frequently share libraries of code between projects. To make it easier to share this code here is a small package manager that downloads code within a directory from Github to be copied in to your current project. It is a quick and dirty alternative to cloning an entire repository, grabbing the set of files you want and placing them in your project.</t>
tags: 
  - github
  - packaging
---

At [VendAsta](http://www.vendAsta.com) we frequently share libraries of code
between projects. To make it easier to share this code I've developed a small
package manager that downloads code within a directory from Github to be copied
in to your current project. It's a quick and dirty alternative to cloning an
entire repository, grabbing the set of files you want and placing them in your
project.

We'll use the [PyGithub](https://github.com/jacquev6/PyGithub) Python library to
interact with the Github API.

##  Logging in to Github

The first step is to log in to Github using our credentials. To do this we
instantiate a new Github object given our username and password and access the
associated user by calling `get_user`.

```python
from github import Github

github = Github('soofaloofa', 'password')
user = github.get_user()
```

This is equivalent to making a [basic authentication
request](https://developer.github.com/v3/#authentication) to get the currently
[authenticated
user](https://developer.github.com/v3/users/#get-the-authenticated-user) and
storing the result in a local representation.

```bash
curl -u soofaloofa https://api.github.com/user
```

##  Accessing a repository

Now that we have a user we can get a repository for that user by name. To get
the repository for this website we make a request to [get a repo by
owner](https://developer.github.com/v3/repos/#get).

```python
repository = user.get_repo('soofaloofa.github.io')
```

## Downloading a single file

To download a single file from a repository we make a call to [get the contents
of a file](https://developer.github.com/v3/repos/contents/#get-contents).

```python
file_content = repository.get_contents('README.md')
```

## Referencing commits

We have all the building blocks to download a resource from Github. The next
step is to download a resource referenced by a specific commit. The Github API
expects SHA values to reference a commit. To make this a bit more user friendly
we can write a function that will search for a SHA given a git tag or branch
name.

```python
def get_sha_for_tag(repository, tag):
    """
    Returns a commit PyGithub object for the specified repository and tag.
    """
    branches = repository.get_branches()
    matched_branches = [match for match in branches if match.name == tag]
    if matched_branches:
        return matched_branches[0].commit.sha

    tags = repository.get_tags()
    matched_tags = [match for match in tags if match.name == tag]
    if not matched_tags:
        raise ValueError('No Tag or Branch exists with that name')
    return matched_tags[0].commit.sha
```

Now we can pass this SHA to the `get_contents` function to get a file for that
specific commit.

```python
sha = get_sha_for_tag(repository, 'develop')
file_content = repository.get_contents('README.md', ref=sha)
```

## Putting it all together

By putting a bit more polish on this we can easily download entire directories
of code that reference a single tag or branch and copy them to our local
environment. The basic workflow is:

1. Choose a repository.
2. Choose a branch or tag.
3. Choose a directory.
4. Iteratively download all the files in that directory.

Let's make that happen.

For this code I'll assume that the Github user belongs to a single organization
and that this organization is sharing code between repositories.

```python
from github import Github
import getpass

username = raw_input("Github username: ")
password = getpass.getpass("Github password: ")

github = Github(username, password)
organization = github.get_user().get_orgs()[0]

repository_name = raw_input("Github repository: ")
repository = organization.get_repo(repository_name)

branch_or_tag_to_download = raw_input("Branch or tag to download: ")
sha = get_sha_for_tag(repository, branch_or_tag_to_download)

directory_to_download = raw_input("Directory to download: ")
download_directory(repository, sha, directory_to_download)
```

This piece of code is fairly simple and relies on a couple of helper functions:
`get_sha_for_tag` and `download_directory`. `get_sha_for_tag` will return the
SHA commit hash given a branch or tag and `download_directory` will recursively
download the files in the given directory.

```python
def get_sha_for_tag(repository, tag):
    """
    Returns a commit PyGithub object for the specified repository and tag.
    """
    branches = repository.get_branches()
    matched_branches = [match for match in branches if match.name == tag]
    if matched_branches:
        return matched_branches[0].commit.sha

    tags = repository.get_tags()
    matched_tags = [match for match in tags if match.name == tag]
    if not matched_tags:
        raise ValueError('No Tag or Branch exists with that name')
    return matched_tags[0].commit.sha


def download_directory(repository, sha, server_path):
    """
    Download all contents at server_path with commit tag sha in 
    the repository.
    """
    contents = repository.get_dir_contents(server_path, ref=sha)

    for content in contents:
        print "Processing %s" % content.path
        if content.type == 'dir':
            download_directory(repository, sha, content.path)
        else:
            try:
                path = content.path
                file_content = repository.get_contents(path, ref=sha)
                file_data = base64.b64decode(file_content.content)
                file_out = open(content.name, "w")
                file_out.write(file_data)
                file_out.close()
            except (GithubException, IOError) as exc:
                logging.error('Error processing %s: %s', content.path, exc)
```

We've been using a variation of this simple script to share code between Github
repositories and appreciate it's flexibility and ease of use. Let me know if you
find it useful!
