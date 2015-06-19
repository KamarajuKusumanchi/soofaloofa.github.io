---
title: "How to get Pulse Audio to recognize your Delta 44 (and other ICE1712 cards)" 
date: 2010-09-28T14:23:34Z
tags:
  - "linux"
  - "pulse audio"
  - "delta 44"
aliases:
  - "posts/2010-09-28-how-to-get-pulse-audio-to-recognize-your-delta-44-and-other-ice1712-cards/"
---

I bought a used Delta 44 soundcard with the intention of recording my guitar playing. Pairing this with Rockit 5 powered monitors, a Mackie 802-VLZ3 mixer and the classic Shure SM57 microphone made for a good quality, inexpensive home studio. Using the PCI-based Delta 44 meant that any old computer would work and my idea was to use vanilla Ubuntu and install the Ardour digital audio workstation to push costs even further down. So far so good. Now the only issue is getting everything up and running. Ubuntu is a fairly mature distribution; you can expect a certain level of quality with each release. Unfortunately, this has not been the case with PulseAudio. I won’t go into details; suffice it to say it had a tumultuous upbringing and many of the kinks have since been worked out. Anyways, I quickly discovered that PulseAudio does not recognize the Delta 44. Don’t worry, this is a known problem and a workaround exists. Finding the workaround, however, did take some time.

<!--more-->

By writing this, I hope to spare some time of yours. I’m going to copy the solution almost verbatim from the Launchpad bug report on this issue and hope that it gets a little more visibility than being buried in a long bug report with cross-references to other forum discussions. Make sure you entirely remove any leftovers from previous attempts to fix the problem. First, create the file

    /etc/udev/rules.d/ice1712-pulseaudio-workaround.rules

and enter the following information.

    SUBSYSTEM!="sound", GOTO="ice1712_end"
    ACTION!="change", GOTO="ice1712_end"
    KERNEL!="card*", GOTO="ice1712_end"
    
    SUBSYSTEMS=="pci", ATTRS{vendor}=="0x1412", ATTRS{device}=="0x1712", ATTRS{subsystem_vendor}=="0x1412", ATTRS{subsystem_device}=="0xd633", ENV{PULSE_PROFILE_SET}="via-ice1712.conf"
    
    LABEL="ice1712_end"

If you have a different audio card with the ICE1712 chipset (such as the Delta 66 for example) substitute

    ATTRS{subsystem_device}=="0xd633"

with whatever is given by the command

    lspci -vvnnd1412

Now, create the file

    /usr/share/pulseaudio/alsa-mixer/profile-sets/via-ice1712.conf

whose contents should be those of , or those of the second “code” block in  When you are finished it should look like this.

    # This file is part of PulseAudio.
    #
    # PulseAudio is free software; you can redistribute it and/or modify
    # it under the terms of the GNU Lesser General Public License as
    # published by the Free Software Foundation; either version 2.1 of the
    # License, or (at your option) any later version.
    #
    # PulseAudio is distributed in the hope that it will be useful, but
    # WITHOUT ANY WARRANTY; without even the implied warranty of
    # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    # General Public License for more details.
    #
    # You should have received a copy of the GNU Lesser General Public License
    # along with PulseAudio; if not, write to the Free Software Foundation,
    # Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
    
    ; Via ICE1712 multi-channel audio chipset
    ;
    ; This chipset has up to four stereo pairs of input and four stereo pairs of
    ; output, named channels 1 to 8. Also available are separate S/PDIF stereo
    ; channels (input and output), and a separate "system-out" stereo jack that
    ; supports 6-channel hardware mixing.
    ;
    ; The S/PDIF stereo channels can be controlled via the mixer for hw:0, and
    ; additionally, the 8 main outputs can be loop-routed to a separate stereo
    ; input pair, available as channels 11 and 12.
    ;
    ; Many cards available from vendors do not expose all channels from this chip
    ; to an external port, which effectively reduces the number of channels that
    ; are useful to the user. However, the ALSA driver still exposes all channels
    ; even if they are not connected.
    ;
    ; We knowingly only define a subset of the theoretically possible
    ; mapping combinations as profiles here.
    ;
    ; See default.conf for an explanation on the directives used here.
    
    [General]
    auto-profiles = no
    
    [Mapping analog-mch-in]
    description = Analog Multi-Channel Main Input
    device-strings = hw:%f,0
    #channel-map = front-left,front-right,rear-left,rear-right,front-center,lfe,side-left,side-right,aux0,aux1,aux2,aux3
    channel-map = aux0,aux1,front-left,front-right,aux2,aux3,aux4,aux5,aux6,aux7,aux8,aux9
    direction = input
    
    [Mapping analog-mch-out]
    description = Analog Multi-Channel Main Output
    device-strings = hw:%f,0
    #channel-map = front-left,front-right,rear-left,rear-right,front-center,lfe,side-left,side-right,aux0,aux1
    channel-map = front-left,front-right,aux0,aux1,aux2,aux3,aux4,aux5,aux6,aux7
    direction = output
    
    [Mapping digital-stereo]
    description = Digital Stereo Input/Output
    #device-strings = hw:%f,1
    device-strings = iec958:%f
    channel-map = left,right
    direction = any
    
    [Mapping analog-system-out]
    description = Analog Stereo System-Out
    device-strings = hw:%f,2
    channel-map = left,right
    direction = output
    
    [Profile output:mch]
    description = Multi-Channel Output Active (Digital Disabled)
    output-mappings = analog-mch-out analog-system-out
    input-mappings =
    priority = 90
    skip-probe = yes
    
    [Profile output:mch input:mch]
    description = Multi-Channel Input/Output (Digital Disabled)
    output-mappings = analog-mch-out analog-system-out
    input-mappings = analog-mch-in
    priority = 100
    skip-probe = yes
    
    [Profile output:spdif]
    description = Digital Output (Multi-Channel Disabled)
    output-mappings = digital-stereo analog-system-out
    input-mappings =
    priority = 80
    skip-probe = yes
    
    [Profile output:spdif input:spdif]
    description = Digital Input/Output (Multi-Channel Disabled)
    output-mappings = digital-stereo analog-system-out
    input-mappings = digital-stereo
    priority = 90
    skip-probe = yes
    
    [Profile output:system]
    description = System Output Only
    output-mappings = analog-system-out
    input-mappings =
    priority = 60
    skip-probe = yes

Make sure you save everything and then restart pulseaudio (or reboot your system) to enjoy your Delta 44!
