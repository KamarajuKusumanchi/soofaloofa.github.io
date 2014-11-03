---
title: "Extracting the Start Sector of a Disk with fdisk"
date: 2014-11-03T05:49:31Z
tags: 
  - "fdisk"
---

fdisk is a wonderful little utility for managing partitions. I recently had to
script a series of fdisk commands for resizing a partition and needed to extract
the start sector from the existing disk to do so. I ended up using this
combination of `grep` and `awk` to do the job.

```bash
start_sector=$(sudo fdisk -l | grep ^/dev/sda1 |  awk -F" "  '{ print $3 }')
```

This line executes `fdisk` with the `-l` option to list the current disks. Then
runs `grep` to find the current boot disk. Finally, `awk` retrieves the
third token of the string where the token seperator is empty space.

Having the start sector I was able to script the fdisk sequence by piping the
necessary sequence of resize commands into fdisk.

```bash
  cat <<EOF | sudo fdisk -c -u /dev/sda
d
n
p
1
$start_sector

w
```

