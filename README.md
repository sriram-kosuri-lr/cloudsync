### CloudSync
*An Elasticsearch Plugin* 

* Runs as Elasticsearch plugin.    
* CloudSync uses Elasticsearch Snapshot and Restore API's and provides a "streaming service" to move data 
from one cluster to another cluster. 
* Plan is to open source it. 

##### Use Case(s)
1. With the launch of LR Cloud, we want to have a simple, cost effective way of moving existing Elasticsearch data 
   from on-prem to LR Cloud.
1. This supports moving 100's of terabytes of Elasticsearch data.     
1. Support blue/green deployment process. We want to do this with (almost) zero downtime.
1. In most cases as the data that needs to be migrated could be large, we want to provide visibility on the transfer 
   process and also estimate the cost and time for the transfer.
1. We could also use this plugin for DR of Elasticsearch cluster. 
1. We could also are used to replicate indices from cluster to another. 
1. We could also use this as backup/restore. 


##### CloudSync Install

1. Install cloudsync
    1. On both source and sink clusters.   
    `
    /usr/share/elasticsearch/bin/elasticsearch-plugin install file:///home/logrhythm/cloud-sync-1.0.0-SNAPSHOT.zip
    `   
1. Install GCS plugin
    1. Download GCS plugin from: https://artifacts.elastic.co/downloads/elasticsearch-plugins/repository-gcs/repository-gcs-5.6.3.zip
    1. On both source and sink clusters.  
    `
    bin/elasticsearch-plugin install file:///home/logrhythm/hack/repository-gcs-5.6.3.zip
    `
    1. Follow steps from: https://www.elastic.co/guide/en/elasticsearch/plugins/5.6/repository-gcs-usage.html 
    1. Adding GCS creds to the Elasticsearch keystore. (On Source)
     ` 
     /usr/share/elasticsearch/bin/elasticsearch-keystore add-file gcs.client.default.credentials_file /gcs/my_file.json
     `
    1. If Sink cluster is on GCP, it doesnt needs GCS creds in Elasticsearch keystore.
        

##### CloudSync API
1. Start the Source:  
    ```
    PUT /cloudsync/start
    {
        "mode" : "source",  
        "store" : "fs",             
        "indices" : "logs-*",
        "location": "/mount/dx_backup",
    }
    ```
    1. Other stores supported: aws-s3, gcp..
    1. This needs to be issued with every restart.
    
1. Start the Sink:
    `
    PUT /cloudsync/start
    {
        "mode" : "sink",
        "store" : "fs",
        "location": "/mount/dx_backup",
    }
    `

1. Curl examples for 'filesystem' store. 

    1. Start Source
    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
      "mode": "source",
      "store": "fs",  
      "indices": "logs",
      "location": "/opt/lr/cloudsync"
    }'
    `    
    1. Start Sink
    
    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
        "mode": "sink",
        "store": "fs",  
        "indices": "logs",
        "location": "/opt/lr/cloudsync"
    }'
    `
1. Curl examples for 'gcs' store 

    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
      "mode": "source",
      "store": "gcs",  
      "indices": "logs",
      "location": "dx_cloud"
    }'
    `
    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
      "mode": "sink",
      "store": "gcs",  
      "indices": "logs",
      "location": "dx_cloud"
    }'
    `


1. Sync Status 

    `GET /cloudsync/status`

    `curl localhost:9200/cloudsync/status`

1. Stop Source
    `POST /cloudsync/stop`

1. Stop Sink
    `POST /cloudsync/stop`


#### Elasticsearch Snapshot & Restore
*Quick Reference*

1. Verify repository is present
    `
    curl -X POST "localhost:9200/_snapshot/dx_backup/_verify"
    `
1. Create repository
    `
    curl -XPUT 'http://localhost:9200/_snapshot/dx_backup' -H 'Content-Type: application/json' -d '{
        "type": "fs",
        "settings": {
            "location": "/opt/lr/cloudsync",
            "compress": true
        }
    }'
    `
    
1. Delete repository 
  1. All snapshots must be deleted before this operation)
    `
    curl -X DELETE "localhost:9200/_snapshot/dx_backup"
    `   

1. Snapshot an index:
    `
    curl -X PUT "localhost:9200/_snapshot/dx_backup/snapshot_1" -H 'Content-Type: application/json' -d'
    {
    
      "indices": "logs-2017-10-21",
      "ignore_unavailable": true,
      "include_global_state": false,
      "chunk_size": "10m"
    }'
    `

2. Check indexInfo status: 
    `curl -X GET "localhost:9200/_snapshot/dx_backup/snapshot_1/_status?pretty"`

3. Delete a indexInfo: 
    `curl -X DELETE "localhost:9200/_snapshot/dx_backup/snapshot_1"`

4. Restore a indexInfo: 
    `curl -X POST "localhost:9201/_snapshot/dx_backup/snapshot_1/_restore"`

5. List of all utils  
    `curl -X GET "localhost:9200/_snapshot/dx_backup/_all?pretty"`


##### Developer Notes
1. Built using maven.
1. Todo list:
    1. Enable logging - currently no logging in place. Had issues with log4j versions.
    1. Check behaviour if multiple instances/nodes are running with this plug-in. or how to make only 
    single instance runs.
    1. Remove state file on source cluster. [repo.path to : /opt/lr/cloudsync] for state file only on source. 
    1. Suport `cloudsync/stop` on both source and sink modes.
    
