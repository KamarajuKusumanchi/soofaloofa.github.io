---
title: "A pypiserver Deployment Script"
date: 2015-02-01T14:53:37-06:00
tags: 
  - "pypi"
  - "pip"
  - "bash"
---

At Vendasta we've been slowly adopting pypi and pip for our internal code
libraries and the time has come to deploy our own private pypi server. After
evaluating a few options I settled on the simplistic
[pypiserver](https://pypi.python.org/pypi/pypiserver) -- a barebones
implementation of the [simple HTTP API](https://pypi.python.org/simple/) to
pypi. 

The deployment uses nginx as a front-end to pypiserver. pypiserver itself is ran
and monitored using supervisord. I created a bash script that creates a user and
group to run pypiserver and installs and runs nginx, supervisord and pypiserver.
I've been running this bash script through Vagrant to deploy a custom pypiserver
for private use. I wanted to save this code for posterity and hopefully help
someone else working on the same task.

```bash
#!/usr/bin/env bash

STARTUP_VERSION=1
STARTUP_MARK=/var/startup.script.$STARTUP_VERSION

# Exit if this script has already ran
if [[ -f $STARTUP_MARK ]]; then
  exit 0  
fi

set -o nounset
set -o pipefail
set -o errexit

# Install prerequesites
sudo apt-get update
sudo apt-get install -y vim
sudo apt-get install -y apache2-utils
sudo apt-get install -y nginx
sudo apt-get install -y supervisor

# Install pip
wget "https://bootstrap.pypa.io/get-pip.py"
sudo python get-pip.py

# Install pypiserver with passlib for upload support
sudo pip install passlib
sudo pip install pypiserver

# Set the port configuration
proxy_port=8080
pypi_port=7201

# Create a user and group to run pypiserver
user=pypiusername
password=pypipasswrd
group=$user

sudo groupadd "$group"
sudo useradd $user -m -g $group -G $group
sudo -u $user -H -s eval 'htpasswd -scb $HOME/.htaccess' "$user $password"
sudo -u $user -H -s eval 'mkdir -p $HOME/packages'

##############
# nginx config
##############
echo "$user:$(openssl passwd -crypt $password)" > /etc/nginx/user.pwd

# nginx can't run as a daemon to work with supervisord
echo "daemon off;" >> /etc/nginx/nginx.conf

cat <<EOF > /etc/nginx/sites-enabled/pypi-server.conf
server {
  listen $proxy_port;
  location / {
    proxy_pass http://localhost:$pypi_port;
    auth_basic "PyPi Authentication";
    auth_basic_user_file /etc/nginx/user.pwd;
  }
}
EOF

rm /etc/nginx/sites-enabled/default

###################
# supervisor config
###################
cat <<EOF > /etc/supervisor/conf.d/pypi-server.conf
[program:pypi-server]
command=pypi-server -p $pypi_port -P /home/$user/.htaccess /home/$user/packages
directory=/home/$user
user=$user
autostart=true
autorestart=true
stderr_logfile=/var/log/pypi-server.err.log
stdout_logfile=/var/log/pypi-server.out.log
EOF

cat <<EOF > /etc/supervisor/conf.d/nginx.conf
[program:nginx]
command=/usr/sbin/nginx
autostart=true
autorestart=true
stdout_events_enabled=true
stderr_events_enabled=true
EOF

sudo supervisorctl reread
sudo supervisorctl update

touch $STARTUP_MARK
```
