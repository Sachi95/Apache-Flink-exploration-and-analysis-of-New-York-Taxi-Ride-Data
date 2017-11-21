
Download The Dataset from the Link Below and copy it in the Data Folder.
https://drive.google.com/open?id=1bpYlr5PDof033AnfOhq3Kzs8IXkyRpgf 

Given a stream of taxi ride events from the public data set of New York Taxi and Limousine Commision. The dataset consists of records about taxi trips in New York city from 2009 to 2015.  We took some of its data, used Apache flink for data stream processing , elasticsearch as a backend to store major information, and tools such as Kibana for dashboard visualization to complete the following objectives.

Our aim is to use as much as features of Apache Flink as possible for analysing the data.

<b> OBJECTIVES: </b>

1. <b> To identify popular locations of New York city </b>

It takes stream of taxi ride events and counts for each coordinate the no. of people that arrive there by taxi. Apache flink features used :- ( map, filter, fold,window).  

2. <b> To identify popular pickup locations for any two particular locations</b>

    It takes stream of taxi ride events and two destination locations as argument and counts for each location the no. of         times it is used for travelling to either of the given destination. Apache flink features used :- ( map, filter, f	       old,join,window) .

3. <b> To split the datastream into day and night time and identify popular locations during those time.</b>

    It takes stream of taxi ride events and perform splits the stream into two new streams, computes for each stream the         popular locations. Apache flink features used :- ( map, filter, fold,split,window) .
 
4. <b> To compare popular locations over time interval of about 6 months </b>

Procedure
Step 1:  Install JDK version 1.8 or above
 
Step 2:  Install IntelliJ IDEA
 
Step 3:  Download Apache Maven 3.x , Flink 0.10, Scala SDK 2.10.2
 
Step 4:  If you are behind a proxy apply this command 
		 with clean install at first time of project setup to download all the required dependencies.
 
Step 5:  Also ensure setting the Apache Maven to your downloaded version and override the settings
 
Step 6:  Setup the import maven projects automatically if you are having issues to download the dependencies
 
Step 7:  Setup the JDK to your downloaded JDK version on hard drive
 
Step 8:  Download Elastic Search <b> 2.4.5 </b> and Kibana <b> 4.5.3 <b>
 
Step 8:  Now setup index in Elastic Search
 
Step 9:  Run Elastic Search
 
Step 9:  Start Kibana and it will automatically pickup existing elastic search index.

