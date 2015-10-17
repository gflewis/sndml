<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:strip-space elements="*"/>
  <xsl:preserve-space elements="p join"/>
  <xsl:output method="text"/>
  <xsl:variable name="newline">
    <xsl:text>&#xa;</xsl:text>
  </xsl:variable>
  <xsl:variable name="indent">
    <xsl:text>&amp;nbsp;&amp;nbsp;&amp;nbsp;&amp;nbsp; </xsl:text>
  </xsl:variable>
  <xsl:template match="/">
    <xsl:apply-templates select="doc/*"/>
  </xsl:template>
  <xsl:template match="h2">
    <xsl:value-of select="$newline"/>
    <xsl:text>h2. </xsl:text>
    <xsl:value-of select="."/>
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template match="h3">
    <xsl:value-of select="$newline"/>
    <xsl:text>h3. </xsl:text>
    <xsl:value-of select="."/>
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template match="p">
    <xsl:call-template name="nobreak">
      <xsl:with-param name="text">
        <xsl:apply-templates select="node()"/>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:value-of select="$newline"/> 
    <xsl:value-of select="$newline"/> 
  </xsl:template> 
  <xsl:template match="join">
    <xsl:call-template name="nobreak">
      <xsl:with-param name="text">
        <xsl:apply-templates select="node()"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="br">
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template match="pre">
    <xsl:text>{noformat}</xsl:text>
    <xsl:apply-templates/> 
    <xsl:text>{noformat}</xsl:text>
    <xsl:value-of select="$newline"/> 
  </xsl:template>
  <xsl:template match="code">
    <xsl:text>{{</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>}}</xsl:text>
  </xsl:template> 
  <xsl:template match="b">
    <xsl:text>{*}</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>{*}</xsl:text>
  </xsl:template>
  <xsl:template match="i">
    <xsl:text>{_}</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>{_}</xsl:text>
  </xsl:template>
  <xsl:template match="notation">
    <xsl:text>| </xsl:text>
    <xsl:apply-templates select="notationsyntax"/>
    <xsl:text> | </xsl:text>
    <xsl:apply-templates select="definition"/>
    <xsl:text> |</xsl:text>
    <xsl:value-of select="$newline"/>    
  </xsl:template>
  <xsl:template match="notationsyntax">
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="definition">
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="syntax">
    <xsl:text>{panel}</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:apply-templates/>    
    <xsl:text>{panel}</xsl:text>
    <xsl:value-of select="$newline"/>    
  </xsl:template>
  <xsl:template match="first">
    <xsl:apply-templates/>
    <xsl:value-of select="$newline"/>    
  </xsl:template>
  <xsl:template match="more">
    <xsl:value-of select="$indent"/>
    <xsl:apply-templates/>
    <xsl:value-of select="$newline"/>    
  </xsl:template>
  <xsl:template match="optional">
    <xsl:text>\[ </xsl:text>
    <xsl:apply-templates/>
    <xsl:text>\] </xsl:text>
  </xsl:template>  
  <xsl:template match="mandatory">
    <xsl:text>\{ </xsl:text>
    <xsl:apply-templates/>
    <xsl:text>\} </xsl:text>
  </xsl:template>
  <xsl:template match="or">
    <xsl:text>\| </xsl:text>
  </xsl:template>
  <xsl:template match="var">
    <xsl:text>{_}{*}</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>{*}{_} </xsl:text>
  </xsl:template>
  <xsl:template match="kw">
    <xsl:text>{{</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>}} </xsl:text>
  </xsl:template>
  <xsl:template match="curly">
    <xsl:text>"\{" </xsl:text>
    <xsl:apply-templates/>
    <xsl:text>"\}" </xsl:text>
  </xsl:template>
  <xsl:template match="example">
    <xsl:text>{noformat}</xsl:text>
    <xsl:apply-templates/>    
    <xsl:text>{noformat}</xsl:text>
    <xsl:value-of select="$newline"/>    
    <xsl:value-of select="$newline"/>    
  </xsl:template>
  <xsl:template match="name">
    <xsl:text>*</xsl:text>
    <xsl:apply-templates/>    
    <xsl:text>*</xsl:text>
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
      <xsl:choose>
        <xsl:when test=".">
          <xsl:value-of select="."/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$url"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>  
    <xsl:text>[</xsl:text>
    <xsl:value-of select="$text"/>
    <xsl:text>|</xsl:text>
    <xsl:value-of select="$url"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  <xsl:template match="page">
    <xsl:choose>
      <xsl:when test="@name='scripts'">
        <xsl:text>[ServiceNow Datapump Scripts]</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>PAGE NOT FOUND: </xsl:text>
        <xsl:value-of select="@name"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="table">
    <xsl:apply-templates select="tr"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template match="tr">
    <xsl:apply-templates select="th|td"/>
    <xsl:text>|</xsl:text>
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template match="th">
    <xsl:text>|| </xsl:text>
    <xsl:value-of select="."/>
    <xsl:text> </xsl:text>
  </xsl:template> 
  <xsl:template match="td">
    <xsl:text>| </xsl:text>
    <xsl:apply-templates select="node()"/>
    <xsl:text> </xsl:text>
  </xsl:template>
  <xsl:template match="ul">
    <xsl:param name="bullets"/>
    <xsl:apply-templates select="li">
      <xsl:with-param name="bullets" select="concat($bullets,'*')"/>
    </xsl:apply-templates>
    <xsl:choose>
      <xsl:when test="$bullets"/>
      <xsl:otherwise>
        <xsl:value-of select="$newline"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="ol">
    <xsl:param name="bullets"/>
    <xsl:apply-templates select="li">
      <xsl:with-param name="bullets" select="concat($bullets,'#')"/>
    </xsl:apply-templates>
    <xsl:choose>
      <xsl:when test="$bullets"/>
      <xsl:otherwise>
        <xsl:value-of select="$newline"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="li">
    <xsl:param name="bullets"/>
    <xsl:value-of select="$bullets"/>
    <xsl:text> </xsl:text>
    <xsl:apply-templates>
      <xsl:with-param name="bullets" select="$bullets"/>
    </xsl:apply-templates>
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>
  <xsl:template match="*">
    <xsl:value-of select="$newline"/>
    <xsl:text>DEFAULT </xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:value-of select="$newline"/>
  </xsl:template>
  <xsl:template name="nobreak">
    <xsl:param name="text"/>
    <xsl:choose>
      <xsl:when test="contains($text, $newline)">
        <xsl:value-of select="substring-before($text, $newline)"/>
        <xsl:text> </xsl:text>
        <xsl:call-template name="nobreak">
          <xsl:with-param name="text" select="substring-after($text, $newline)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="string-replace">
    <xsl:param name="text" />
    <xsl:param name="old" />
    <xsl:param name="new" />
    <xsl:choose>
      <xsl:when test="contains($text, $old)">
        <xsl:value-of select="substring-before($text,$old)" />
        <xsl:value-of select="$new" />
        <xsl:call-template name="string-replace">
          <xsl:with-param name="text" select="substring-after($text,$old)" />
          <xsl:with-param name="old" select="$old" />
          <xsl:with-param name="new" select="$new" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>  
</xsl:stylesheet>