package org.elasticsearch.sync.cloud.start;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class SinkThread extends StartModeThread implements Runnable {

    private static final int sleepIntervalMsecs = 10000;

    public SinkThread(final AdminClient client, final StartInfo startInfo){
        super(client,startInfo);
    }

    @Override
    public void run() {
        try {
            createRepository();
        }catch (IOException ex){
            //todo: log and exit.
        }
        _run();
    }

    private String getIndexName(String snapshot){
        return snapshot.substring(snapshotNamePrefix.length(),snapshot.length());
    }

    private void _run(){
        ElasticClient elasticClient = new ElasticClient(client);
        while (true) {
            try {
                List<String> snapshotNames = elasticClient.listSnapshots(repository);
                for(String ssName : snapshotNames){
                    String index = getIndexName(ssName);

                    boolean ack = elasticClient.restoreSnapshot(repository, ssName);
                    if(ack) {
                        waitForIndexGreenStatus(index);
                        elasticClient.deleteSnapshot(repository, ssName);
                    }
                }
                Thread.sleep(sleepIntervalMsecs);
            } catch (Exception ex) {
                //todo: log
            }
        }
    }

    private void waitForIndexGreenStatus(String index) {
        while (true) {
            ClusterHealthResponse response = client.cluster().prepareHealth(index).get();
            ClusterHealthStatus status = response.getIndices().get(index).getStatus();
            if (!status.equals(ClusterHealthStatus.GREEN)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //ignore
                }
            } else {
                break;
            }
        }
    }
}