package org.elasticsearch.sync.cloud.start;

import org.elasticsearch.client.AdminClient;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;

import java.io.IOException;

public abstract class StartModeThread {

    protected final  String repository = "dx_backup";
    protected final String snapshotNamePrefix = "snapshot_";
    protected final StartInfo startInfo;
    protected final AdminClient client;

    public StartModeThread(final AdminClient client, final StartInfo startInfo){
        this.startInfo = startInfo;
        this.client = client;
    }

    protected void createRepository() throws IOException {
        ElasticClient elasticClient = new ElasticClient(client);
        boolean hasRepository = elasticClient.hasRepository(repository);
        if(!hasRepository){
            boolean ack = elasticClient.createRepository(startInfo.getStore(),repository,startInfo.getLocation());
            if(!ack){
                throw new IOException("Failed to create repository.");
            }
        }
    }

}
