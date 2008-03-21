<?xml version="1.0"?> 
<!-- $Id$ -->
<!-- This stylesheet takes as input a plugin model XMI and -->
<!-- performs various transformations on it, producing -->
<!-- PluginMetamodelTransformed.xsl.  -->

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:Model="org.omg.xmi.namespace.Model"
  >
  <xsl:output method="xml" indent="yes" />

  <!-- Rename the PrimitiveTypes package to PrimitiveTypeRef -->
  <xsl:template match="Model:Package[@name='PrimitiveTypes']/@name" >
    <xsl:attribute name="name">PrimitiveTypesRef</xsl:attribute>
  </xsl:template>

  <!-- Rename import package-->
  <xsl:template match="Model:Import[@name='PrimitiveTypes']/@name" >
    <xsl:attribute name="name">PrimitiveTypesRef</xsl:attribute>
  </xsl:template>


  <!-- Prefix all xmi id's with eeme to avoid conflicts with main model -->

  <xsl:template match="@xmi.id">
    <xsl:attribute name="xmi.id">
      <xsl:value-of select="concat('eeme',.)"/>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="@xmi.idref">
    <xsl:attribute name="xmi.idref">
      <xsl:value-of select="concat('eeme',.)"/>
    </xsl:attribute>
  </xsl:template>

  <!-- Pass everything else through unchanged -->
  <xsl:template match="/ | @* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
