# You are probably in the wrong repo!

* This is the repo for SNDML version 2, which is no longer supported.
* You are probably looking for SNDML version 3, which is the latest version.
* The latest version is here:
* * https://github.com/gflewis/sndml3

<hr/>

SNDML3 (ServiceNow Datamart Loader) is a Java command-line application which exports data from ServiceNow to an SQL database such as MySQL, PostgreSQL, Oracle or Microsoft SQL Server. SNDML uses the ServiceNow REST API to extract data from ServiceNow. It uses JDBC to load the target. It creates tables in the target database based on extracted meta-data. It supports a variety of load and synchronization operations. 

Version 3 is essentially a complete rebuild of the application released in 2018. The following are significant changes since version 2.
* The SOAP API has been replaced with the REST API and the JSONv2 API for improved performance.
* The version 2 "script" syntax has been abandoned and replaced with a simpler YAML syntax.
* Version 3 includes a `partition` option for improved reliability when backloading large task based tables.
* Version 3 includes a `metrics` file option to enable incremental loads since the last run.
* Version 3 includes a `sync` action which compares `sys_updated_on` to determine which records need to be updated, inserted or deleted.
* While the `dialect` option is still supported, it is no longer required. By default the code will select a dialect from the templates file based on the JDBC URL.

For a quick tutorial, please see https://github.com/gflewis/sndml3/wiki/Getting-Started.

This program is freely distributed software. You are welcome to redistribute and/or modify it. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY, explicit or implied. 
