all: api 

api:
	mvn install 

install:
	mkdir -p /var/log/openvote-worker
	mkdir -p /opt/openvote-worker/bin
	mkdir -p /opt/openvote-worker/etc
	mkdir -p /opt/openvote-worker/ssl
	install ./bin/openvote-worker /opt/openvote-worker/bin/
	install ./etc/example.conf /opt/openvote-worker/etc/
	install ./etc/log4j.properties /opt/openvote-worker/etc/
	rsync -a ./target /opt/openvote-worker/
	ln -s -f /opt/openvote-worker/bin/openvote-worker /etc/init.d/openvote-worker
	install ./etc/logrotate /etc/logrotate.d/openvote-worker
	chown -R admin:admin /opt/openvote-worker
	chown -R admin:admin /var/log/openvote-worker
