# $Id$

all: ant commons-collections commons-logging hibernate hsqldb mdrlibs

clean:
	-rm -rf ant commons-logging commons-collections hibnerate hsqldb mdrlibs

ant: apache-ant-1.7.0-bin.tar.bz2
	-rm -rf $@ apache-ant-1.7.0
	tar xfj $<
	mv apache-ant-1.7.0 $@
	touch $@

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

# TODO use hibernate package
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
