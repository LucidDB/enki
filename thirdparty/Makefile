# $Id$

all: commons-collections commons-logging hibernate hsqldb mdrlibs \
	jgrapht findbugs simple-jndi commons-pool commons-dbcp \
	mysql-connector

clean:
	-rm -rf ant commons-logging commons-collections hsqldb \
		mdrlibs junit/junit.jar jgrapht ehcache findbugs \
		simple-jndi commons-pool commons-dbcp

commons-logging: commons-logging-1.0.4.tar.gz
	-rm -rf $@ commons-logging-1.0.4
	tar xfz $<
	mv commons-logging-1.0.4 $@
	touch $@

commons-collections: commons-collections-3.1.tar.gz
	-rm -rf $@ commons-collections-3.1
	tar xfz $<
	mv commons-collections-3.1 $@
	touch $@

commons-pool: commons-pool-1.3.tar.gz
	-rm -rf $@
	tar xfz $<
	mv commons-pool-1.3 $@
	touch $@

commons-dbcp: commons-dbcp-1.2.1.tar.gz
	-rm -rf $@
	tar xfz $<
	mv commons-dbcp-1.2.1 $@
	touch $@

# TODO use an official hibernate tarball package (and add hibernate to clean)
hibernate:

hsqldb: hsqldb_1_8_0_2.zip
	-rm -rf $@
	unzip $<
	touch $@

mdrlibs: mdrextras.tar.gz mdr-standalone.zip uml2mof.zip
	-rm -rf $@
	tar xfz mdrextras.tar.gz
	unzip mdr-standalone.zip -d mdrlibs
	unzip -n uml2mof.zip -d mdrlibs
	touch $@

jgrapht: jgrapht-0.7.1.tar.gz
	-rm -rf $@
	tar xfz $<
	mv jgrapht-0.7.1 $@
	touch $@

findbugs: findbugs-1.3.2.tar.gz
	-rm -rf $@
	tar xfz $<
	mv findbugs-1.3.2 findbugs
	touch $@

simple-jndi: simple-jndi-0.11.4.1.tar.gz
	-rm -rf $@
	tar xfz $<
	mv simple-jndi-0.11.4.1 simple-jndi
	touch $@

mysql-connector: mysql-connector-java-3.1.14.zip
	-rm -rf mysql-connector-java-3.1.14 $@
	unzip -o $<
	mv mysql-connector-java-3.1.14 $@
	touch $@
