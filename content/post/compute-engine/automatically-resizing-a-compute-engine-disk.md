---
title: "Automatically Resizing a Compute Engine Disk"
date: 2014-11-11T19:21:45Z
tags: 
  - "compute engine"
  - "disk"
aliases:
  - "posts/2014-11-11-automatically-resizing-a-compute-engine-disk/"
---

A recurring issue when working with [Compute
Engine](https://cloud.google.com/compute/) is that newly created Instances have
only 10GB of free space available. To take advantage of the full disk size you
need to manually partition and resize it. This article shows one method of
accomplishing this task.

<!--more-->

To correctly partition the disk we need to find the start sector. We can find this using `fdisk` with the `-l`
option to list the full disk output.

```bash
> sudo fdisk -l

Disk /dev/sda: 10.7 GB, 10737418240 bytes
4 heads, 32 sectors/track, 163840 cylinders, total 20971520 sectors
Units = sectors of 1 * 512 = 512 bytes
Sector size (logical/physical): 512 bytes / 4096 bytes
I/O size (minimum/optimal): 4096 bytes / 4096 bytes
Disk identifier: 0x0009c3f5

   Device Boot      Start         End      Blocks   Id  System
/dev/sda1   *        4096    20971519    10483712   83  Linux
```

Only one disk exists and its size is 10.7 GB. We need to resize this disk
ourselves to make it fully useable. The last line of the `fdisk` output lists the start
sector. We can extract it using a combination of `grep` and `awk`.

```bash
# fisk -l: get the full disk output
# grep ^/dev/sda1: filter the line for the boot disk
# awk -F" " '{ print $3 }': get the third token where the separator is space
start_sector=$(sudo fdisk -l | grep ^/dev/sda1 |  awk -F" "  '{ print $3 }')
```

Now that we have the start sector we can run through the sequence of commands
required for `fdisk` to create a new partition of the full disk size.

```bash
cat <<EOF | sudo fdisk -c -u /dev/sda
d
n
p
1
$start_sector

w
EOF
```

The `-c` and `-u` option make `fdisk` behave the same on both CentOS and Debian.
`d` deletes the first (default) partition. `n` creates a new partition. `p`
selects the new partition as a primary. `1` is the partition number.
`$start_sector` is the value we extracted from `fdisk -l`. The blank line
specifies the default end sector. Finally, `w` writes our changes.

We need to reboot our machine for these changes to take effect.

```bash
sudo reboot
```

Once our machine comes back to life we need to resize our disk to the full
partition size. This is done withe the `resize2fs` command.

```bash
resize2fs /dev/sda1
```

Putting this all together, we can write a script that will automatically resize
a Compute Engine disk. Using this as a startup script will make any Compute
Engine instance you create have a fully sized disk available for use.


```bash
STARTUP_VERSION=1
PARTITION_MARK=/var/startup.partition.$STARTUP_VERSION
RESIZE_MARK=/var/startup.resize.$STARTUP_VERSION

# Repartition the disk to full size
function partition() {
  start_sector=$(sudo fdisk -l | grep ^/dev/sda1 |  awk -F" "  '{ print $3 }')

  cat <<EOF | sudo fdisk -c -u /dev/sda
d
n
p
1
$start_sector

w
EOF

  # We've made the changes to the partition table, they haven't taken effect; we need to reboot.
  touch $PARTITION_MARK

  sudo reboot
  exit
}

# Resize the filesystem
function resize() {
  resize2fs /dev/sda1
  touch $RESIZE_MARK
}

if [ ! -f $PARTITION_MARK ]; then
  partition
fi

if [ ! -f $RESIZE_MARK ]; then
  resize
fi
```

