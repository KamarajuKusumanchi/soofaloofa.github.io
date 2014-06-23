#!/bin/bash

hugo --baseUrl="http://sookocheff.com"
ghp-import -m "Push from source" -n -p -b master public/
