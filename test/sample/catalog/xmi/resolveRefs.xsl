<?xml version="1.0"?> 
<!-- $Id$ -->
<!-- This stylesheet takes as input an XMI document containing the -->
<!-- EEM and resolves cross-model references. -->

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:Model="org.omg.xmi.namespace.Model"
  >
  <xsl:output method="xml" indent="yes" />

  <!-- Filter out the PluginBaseRef subpackage.  -->
  <xsl:template match="Model:Package[@name='PluginBaseRef']">
  </xsl:template>

  <!-- Filter out the SampleE subpackage.  -->
  <xsl:template match="Model:Package[@name='SampleE']">
  </xsl:template>

  <!-- Filter out the PrimitiveTypesRef subpackage.  -->
  <xsl:template match="Model:Package[@name='PrimitiveTypesRef']">
  </xsl:template>

  <!-- Filter out the PrimitiveTypesRef subpackage import.  -->
  <xsl:template match="Model:Import[@name='PrimitiveTypesRef']">
  </xsl:template>

  <!-- Merge SampleE into Sample.  -->
  <xsl:template match="Model:Package[@name='Sample']">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <Model:Namespace.contents>
        <xsl:apply-templates select="Model:Namespace.contents/*"/>
        <xsl:apply-templates select="//Model:Package[@name='SampleE']/Model:Namespace.contents/*"/>
      </Model:Namespace.contents>
    </xsl:copy>
  </xsl:template>

  <!-- When we see an idref which refers to a class in package
       PluginBaseRef, remap it to the id of the real PlubinBase class instead.  -->
  <xsl:template match="@xmi.idref">
    <xsl:variable 
      name="refEemClass" 
      select=
      "//Model:Package[@name='PluginBaseRef']//Model:Class[@xmi.id=current()]" />
    <xsl:variable 
      name="refPrimitive" 
      select=
      "//Model:Package[@name='PrimitiveTypesRef']//Model:PrimitiveType[@xmi.id=current()]" />
    <xsl:choose>
      <xsl:when test="$refEemClass">
        <xsl:variable 
          name="refEemClassName" 
          select="$refEemClass/@name"/>
        <xsl:variable 
          name="realEemClass" 
          select=
          "//Model:Package[@name='EEM']//Model:Class[@name=$refEemClassName]"/>
        <xsl:attribute name="xmi.idref">
          <xsl:value-of select="$realEemClass/@xmi.id"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="$refPrimitive">
        <xsl:variable 
          name="refPrimitiveName" 
          select="$refPrimitive/@name"/>
        <xsl:variable 
          name="realPrimitiveType" 
          select=
          "//Model:Package[@name='PrimitiveTypes']//Model:PrimitiveType[@name=$refPrimitiveName]"/>
        <xsl:attribute name="xmi.idref">
          <xsl:value-of select="$realPrimitiveType/@xmi.id"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- Pass everything else through unchanged -->
  <xsl:template match="/ | @* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
