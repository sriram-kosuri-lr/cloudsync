package org.elasticsearch.sync.cloud.start;


import org.elasticsearch.client.AdminClient;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.sync.cloud.utils.*;

import java.io.IOException;
import java.util.List;

public class SourceThread extends StartModeThread implements Runnable  {

    private static final int sleepIntervalMsecs = 5000;
    private static final int maxAllowedSnapshots = 1;


    public SourceThread(final AdminClient client, final StartInfo startInfo){
        super(client,startInfo);
    }

    /**
     * Cleanup needed.
     */
    @Override
    public void run() {
        while (true) {
            try {
                createRepository();
                blockOnPending();
                doWork();
                Thread.sleep(sleepIntervalMsecs);
            }catch (IOException|InterruptedException ex ){
                //todo: log and continue.

            }
        }
    }

    /**
     * If more than the 'x' snapshots are present in the repo,
     * this blocks till those are cleared. Similar to a bounded queue.
     */
    private void blockOnPending() throws InterruptedException {
        while(true) {
            ElasticClient elasticClient = new ElasticClient(client);
            int count = elasticClient.snapshotsCount(repository, "_all");
            if (count >= maxAllowedSnapshots) {
                Thread.sleep(sleepIntervalMsecs);
            } else {
                break;
            }
        }
    }
    private void doWork() throws IOException {
        SyncStateFile stateFile = new SyncStateFile();

        String json = stateFile.read();
        List<IndexInfo> listSS = Utils.toSnapshots(json);
        List<IndexInfo> filtered = Utils.sortAndFilter(listSS, IndexInfo.State.READY);
        if(filtered.size() == 0 ) {
            return;
        }

        IndexInfo first = filtered.get(0);
        //update the state file.
        IndexInfo modifiedSS = new IndexInfo(first.getName(),first.getSizeInBytes(), IndexInfo.State.SNAPSHOTED);
        listSS = Utils.replace(modifiedSS,listSS);
        stateFile.write(Utils.toJson(listSS));

        ElasticClient elasticClient = new ElasticClient(client);
        elasticClient.takeSnapshot(repository, snapshotNamePrefix +first.getName(),first.getName());
        //todo: if failed, revert the change in state file.
    }
}
