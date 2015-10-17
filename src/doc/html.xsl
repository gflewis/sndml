<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY nbsp "&#160;">
<!ENTITY mdash "&#x2014;">  
]>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:param name="version">x.x.x</xsl:param>
  <xsl:strip-space elements="*"/>
  <xsl:preserve-space elements="p join"/>  
  <xsl:output method="html"/>
  <xsl:variable name="title" select="/doc/@title"/>
  <xsl:variable name="warranty">
    <xsl:text>This program is free software. </xsl:text>
    <xsl:text>You are welcome to redistribute and/or modify it. </xsl:text>
    <xsl:text>This program is distributed in the hope that it will be useful, </xsl:text>
    <xsl:text>but WITHOUT ANY WARRANTY, explicit or implied. </xsl:text>
  </xsl:variable>  
  <xsl:template match="/doc">
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="styles.css"/>
        <title><xsl:value-of select="$title"/></title>
      </head>
      <body>
        <table class="frame" width="100%">
          <tr>
            <td colspan="2" class="header">
              <div class="header">              
                <xsl:call-template name="header"/>
              </div>
            </td>
          </tr>
          <tr>
            <td class="leftnav">
              <div class="leftnav">
                <xsl:call-template name="leftnav"/>
              </div>
            </td>
            <td class="main">
              <div class="main">
                <xsl:element name="h1">
                  <xsl:value-of select="@title"/>
                </xsl:element>
                <xsl:apply-templates select="*"/>
              </div>
            </td>
          </tr>          
        </table>
      </body>
    </html>
  </xsl:template>
  <xsl:template name="header">
    <span class="hdr-big">ServiceNow DataPump </span>
    <span class="hdr-small">(<i>a.k.a.</i> Data Mart Loader)</span>
  </xsl:template>
  <xsl:template name="leftnav">
    <ul>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">Introduction</xsl:with-param>
        <xsl:with-param name="page">index</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">Quick Start</xsl:with-param>
        <xsl:with-param name="page">quickstart</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">Concepts</xsl:with-param>
        <xsl:with-param name="page">concepts</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">Configuration</xsl:with-param>
        <xsl:with-param name="page">configuration</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">Scripts</xsl:with-param>
        <xsl:with-param name="page">scripts</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">GUI</xsl:with-param>
        <xsl:with-param name="page">gui</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">FAQ</xsl:with-param>
        <xsl:with-param name="page">faq</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="navlink">
        <xsl:with-param name="label">Downloads</xsl:with-param>
        <xsl:with-param name="page">downloads</xsl:with-param>
      </xsl:call-template>
    </ul>
  </xsl:template>
  <xsl:template name="navlink">
    <xsl:param name="label"/>
    <xsl:param name="page"/>
    <xsl:variable name="thispage" select="/doc/@name"/>    
    <xsl:variable name="url">
      <xsl:value-of select="concat($page,'.html')"/>
    </xsl:variable>
    <li>
      <xsl:if test="$page=$thispage">
        <xsl:attribute name="class">selected</xsl:attribute>
      </xsl:if>
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:value-of select="$url"/>         
        </xsl:attribute>
        <xsl:value-of select="$label"/>
      </xsl:element>
    </li>
  </xsl:template>
  <xsl:template match="version">
    <xsl:value-of select="$version"/>
  </xsl:template>
  <xsl:template match="join">
    <xsl:apply-templates select="node()"/>
  </xsl:template>
  <xsl:template match="p">
    <xsl:element name="p">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="warning">
    <xsl:element name="p">
      <xsl:attribute name="class">warning</xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="h2">
    <xsl:choose>
      <xsl:when test="@name">
        <xsl:element name="a">
          <xsl:attribute name="name">
            <xsl:value-of select="@name"/>
          </xsl:attribute>
        </xsl:element>
      </xsl:when>
    </xsl:choose>
    <xsl:element name="h2">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="page">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="@name"/>
        <xsl:text>.html</xsl:text>
        <xsl:choose>
          <xsl:when test="@sect">
            <xsl:text>#</xsl:text>
            <xsl:value-of select="@sect"/>
          </xsl:when>
        </xsl:choose>
      </xsl:attribute>
      <xsl:value-of select="."/>      
    </xsl:element>
  </xsl:template>
  <xsl:template match="link">
    <xsl:variable name="url">
      <xsl:choose>
        <xsl:when test="@url">
          <xsl:value-of select="@url"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="text">
      <xsl:value-of select="."/>
    </xsl:variable>
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="$url"/>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="string-length($text)=0">
          <xsl:value-of select="$url"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$text"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>
  <xsl:template match="image">
    <xsl:element name="img">
      <xsl:attribute name="src">
        <xsl:text>images/</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>.png</xsl:text>
      </xsl:attribute>
    </xsl:element>
  </xsl:template>
  <xsl:template match="screenshot">
    <xsl:variable name="url">
      <xsl:text>images/</xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>.png</xsl:text>
    </xsl:variable>
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="$url"/>
      </xsl:attribute>
      <xsl:element name="img">
        <xsl:attribute name="src">
          <xsl:value-of select="$url"/>
        </xsl:attribute>
        <xsl:attribute name="width">600</xsl:attribute>
        <xsl:attribute name="border">1</xsl:attribute>        
      </xsl:element>
    </xsl:element>
    <xsl:element name="hr"/>
  </xsl:template>
  <xsl:template match="code">
    <xsl:element name="span">
      <xsl:attribute name="class">code</xsl:attribute>
      <xsl:value-of select="."/>
    </xsl:element>
  </xsl:template> 
  <xsl:template match="var">
    <xsl:element name="span">
      <xsl:attribute name="class">var</xsl:attribute>
      <xsl:value-of select="."/>
    </xsl:element>
    <xsl:text> </xsl:text>
  </xsl:template>
  <xsl:template match="kw">
    <xsl:value-of select="."/>
    <xsl:text> </xsl:text>
  </xsl:template>
  <xsl:template match="optional">
    <xsl:text>[ </xsl:text>
    <xsl:apply-templates/>
    <xsl:text>] </xsl:text>
  </xsl:template>
  <xsl:template match="mandatory">
    <xsl:text>{ </xsl:text>
    <xsl:apply-templates/>
    <xsl:text>} </xsl:text>
  </xsl:template>
  <xsl:template match="or">
    <xsl:text>| </xsl:text>
  </xsl:template>
  <xsl:template match="curly">
    <xsl:text>"{" </xsl:text>
    <xsl:apply-templates/>
    <xsl:text>"}" </xsl:text>
  </xsl:template>
  <xsl:template match="name">
    <xsl:element name="span">
      <xsl:attribute name="class">name</xsl:attribute>
      <xsl:value-of select="."/>      
    </xsl:element>
  </xsl:template>
  <xsl:template match="filename">
    <xsl:element name="span">
      <xsl:attribute name="class">filename</xsl:attribute>
      <xsl:value-of select="."/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="qatable">
    <table class="faq">
      <xsl:apply-templates match="qa"/>
    </table>
  </xsl:template>
  <xsl:template match="qa">
    <tr>
      <th class="faq-q">Q:</th>
      <td class="faq-q"><xsl:apply-templates select="q"/></td>
    </tr>
    <tr>
      <th class="faq-a">A:</th>
      <td class="faq-a"><xsl:apply-templates select="a"/></td>
    </tr>
  </xsl:template>
  <xsl:template match="q|a">
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="syntax">
    <xsl:element name="span">
      <xsl:attribute name="class">syntax</xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="first">
    <xsl:element name="p">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="more">
    <xsl:element name="p">
      <xsl:attribute name="class">indent</xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="example">
    <xsl:element name="pre">
      <xsl:attribute name="class">example</xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="notation">
    <xsl:element name="p">
      <xsl:apply-templates select="notationsyntax"/>
      <xsl:text> &mdash; </xsl:text>
      <xsl:apply-templates select="definition"/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="notationsyntax">
    <xsl:element name="span">
      <xsl:attribute name="class">syntax</xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>/  
  <xsl:template match="definition">
    <xsl:apply-templates/>
  </xsl:template>
  <!-- standard copy template --> 
  <xsl:template match="@*|node()">
	<xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
	</xsl:copy>
  </xsl:template>
</xsl:stylesheet>