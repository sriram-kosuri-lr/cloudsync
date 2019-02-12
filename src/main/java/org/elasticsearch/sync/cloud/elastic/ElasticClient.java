package org.elasticsearch.sync.cloud.elastic;


import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryResponse;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusRequest;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusResponse;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.snapshots.SnapshotInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set of Elastic utils
 */
public class ElasticClient {

    private AdminClient client;

    public ElasticClient(AdminClient client) {
        this.client = client;
    }

    /**
     * Get indices matching the pattern.
     * @param pattern
     * @return
     */
    public Map<String, Long> getIndices(String pattern) {
        IndicesStatsResponse r = client.indices().stats(new IndicesStatsRequest().all()).actionGet();
        Map<String, IndexStats> indices = r.getIndices();
        Map<String, Long> filteredIndices = new HashMap<>();
        for (String index : indices.keySet()) {
            //fixme: support regex.
            if (index.startsWith(pattern)) {
                filteredIndices.put(index, indices.get(index).getTotal().store.sizeInBytes());
            }
        }
        return filteredIndices;
    }


    /**
     * Request to take a snapshot.
     * @param index
     * @return
     */
    public void takeSnapshot(final String repository, final String snapshotName, final String index) throws IOException {
        IndicesOptions indicesOptions = IndicesOptions.fromOptions(true, true,
                true, false, IndicesOptions.lenientExpandOpen());

        CreateSnapshotRequest createSnapshotRequest = new CreateSnapshotRequest(repository, snapshotName)
                .indices(index)
                .includeGlobalState(false)
                .indicesOptions(indicesOptions);
                //todo: add chunk_size

        CreateSnapshotResponse createSnapshotResponse = client.cluster().createSnapshot(createSnapshotRequest).actionGet();
        if (RestStatus.ACCEPTED.getStatus() != createSnapshotResponse.status().getStatus()) {
            throw new IOException("IndexInfo Failed.");
        }
    }

    /**
     * Checks if repository is present.
     *
     * @return
     */
    public boolean hasRepository(final String repository) {
        try {
            VerifyRepositoryRequest request = new VerifyRepositoryRequest()
                    .name(repository);
            VerifyRepositoryResponse response = client.cluster().verifyRepository(request).actionGet();
            if (response.getNodes() == null || response.getNodes().length == 0)
                return false;

        } catch (RuntimeException ex) {
            return false;
        }
        return true;
    }

    /**
     * Create repository.
     *
     * @param type
     * @param location if type is fs its folder , if type is gcs its bucket name.
     * @return
     */
    public boolean createRepository(final String type, final String repository, final String location) {
        Map<String, Object> settings = new HashMap<>();

        if("fs".equals(type)) {
            settings.put("location", location);
            settings.put("compress", "true");
        }else if("gcs".equals(type)){
            settings.put("bucket", location);
        }

        PutRepositoryRequest request = new PutRepositoryRequest()
                .type(type)
                .name(repository)
                .settings(settings);

        PutRepositoryResponse response = client.cluster().putRepository(request).actionGet();
        return response.isAcknowledged();
    }

    /**
     * Deletes repository.
     * @param repository
     * @return
     */
    public boolean deleteRepository(final String repository) {
        DeleteRepositoryRequest request = new DeleteRepositoryRequest().name(repository);
        DeleteRepositoryResponse response = client.cluster().deleteRepository(request).actionGet();
        return response.isAcknowledged();
    }

    /**
     * Deletes snapshot
     * @param repository
     * @param snapshot
     * @return
     */
    public boolean deleteSnapshot(final String repository, final String snapshot) {
        DeleteSnapshotRequest request = new DeleteSnapshotRequest().repository(repository).snapshot(snapshot);
        DeleteSnapshotResponse response = client.cluster().deleteSnapshot(request).actionGet();
        return response.isAcknowledged();
    }

    /**
     * Restores snapshot.
     * @param repository
     * @param snapshot
     * @return
     */
    public boolean restoreSnapshot(final String repository, final String snapshot) {
        RestoreSnapshotRequest request = new RestoreSnapshotRequest().repository(repository).snapshot(snapshot);
        RestoreSnapshotResponse response = client.cluster().restoreSnapshot(request).actionGet();
        if (RestStatus.ACCEPTED.getStatus() == response.status().getStatus()) {
            return true;
        }
        return false;
    }

    /**
     * List all snapshots in the repository
     * @param repository
     * @return
     */
    public List<String> listSnapshots(final String repository){
        String[] ss = {"_all"};
        GetSnapshotsRequest request = new GetSnapshotsRequest().repository(repository).snapshots(ss);
        GetSnapshotsResponse response = client.cluster().getSnapshots(request).actionGet();
        List<String> list = new ArrayList<>();
        List<SnapshotInfo> ssInfo = response.getSnapshots();
        for(SnapshotInfo info : ssInfo){
            list.add(info.snapshotId().getName());
        }
        return list;
    }
    /**
     * @param repository
     * @param snapshot
     * @return
     */
    public boolean snapshotsStatus(final String repository, final String snapshot) {
        String[] ss = {snapshot};
        SnapshotsStatusRequest request = new SnapshotsStatusRequest().repository(repository).snapshots(ss);
        SnapshotsStatusResponse response = client.cluster().snapshotsStatus(request).actionGet();
        return (response.getSnapshots().size() == 0 ) ? false : response.getSnapshots().get(0).getState().completed();

    }

    /**
     *
     * @param repository
     * @param snapshot
     * @return
     */
    public int snapshotsCount(final String repository, final String snapshot) {
        String[] ss = {snapshot};
        GetSnapshotsRequest request = new GetSnapshotsRequest().repository(repository).snapshots(ss);
        GetSnapshotsResponse response = client.cluster().getSnapshots(request).actionGet();
        return response.getSnapshots().size();
    }
}
