# ServiceNow Datamart Loader

The ServiceNow Data Mart Loader is a Java application which uses ServiceNow's Web Services (SOAP) API to extract meta-data and data from ServiceNow. The Loader automatically creates tables in an SQL database based on meta-data extracted from ServiceNow. It supports a variety of load and synchronization operations including 
* back-loading of historical data
* "refresh" which inserts/updates any records which have been inserted/updated in ServiceNow since the last run
* "prune" which deletes any records which have been deleted in ServiceNow since the last run

This program is freely distributed software. You are welcome to redistribute and/or modify it. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY, explicit or implied. 

Documentation please refer to the the [SNDML Wiki](https://github.com/gflewis/sndml/wiki)

<hr/>

This is the repo for SNDML 2.x.

SNDML 3.x has now entered beta testing. For information refer to:
https://github.com/gflewis/sndml3
