<?xml version="1.0"?> 
<!-- $Id$ -->
<!-- This stylesheet takes as input an XMI document and merges it -->
<!-- with the input XMI document. -->

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:Model="org.omg.xmi.namespace.Model"
  >

  <xsl:output method="xml" indent="yes" />

  <!-- location of plugin model XMI from ant -->
  <xsl:param name="pluginXmiFilename"/>

  <!-- Introduce some top-level packages -->
  <xsl:template match="XMI/XMI.content/Model:Package[@name='EEM']">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <Model:Namespace.contents>
        <xsl:apply-templates select="Model:Namespace.contents/*" />
        <xsl:copy-of select="document($pluginXmiFilename)/XMI/XMI.content/*"/>
      </Model:Namespace.contents>
    </xsl:copy>
  </xsl:template>

  <!-- Pass everything else through unchanged -->
  <xsl:template match="/ | @* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
