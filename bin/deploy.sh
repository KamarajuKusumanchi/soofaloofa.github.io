#!/bin/bash

docpad generate --env static
ghp-import -m "Push from source" -n -p -b master out/
