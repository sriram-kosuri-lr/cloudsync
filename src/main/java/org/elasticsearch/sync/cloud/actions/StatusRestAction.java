package org.elasticsearch.sync.cloud.actions;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.sync.cloud.messages.StatusResponseMessage;
import org.elasticsearch.sync.cloud.status.StatusInfo;
import org.elasticsearch.sync.cloud.utils.IndexInfo;
import org.elasticsearch.sync.cloud.utils.SyncStateFile;
import org.elasticsearch.sync.cloud.utils.Utils;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class StatusRestAction extends BaseRestHandler {

    protected final  String repository = "dx_backup";

    @Inject
    public StatusRestAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/cloudsync/status", this);
    }

    @Override
    protected BaseRestHandler.RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        //todo: valid only for source

        //read state file.
        //read open snapshots
        //calc: indices:  total, done, pending, in_progress
        //calc: size also.

        StatusInfo statusInfo = new StatusInfo();
        SyncStateFile stateFile = new SyncStateFile();
        String json = stateFile.read();
        List<IndexInfo> indices = Utils.toSnapshots(json);
        List<String> snapShots = new ElasticClient(client.admin()).listSnapshots(repository);


        statusInfo.totalIndices = indices.size();
        int currentSnapShoted = 0;
        for(IndexInfo index :indices){
            statusInfo.totalSizeInBytes += index.getSizeInBytes();

            if(index.getState() == IndexInfo.State.READY){
                statusInfo.pendingIndices++;
                statusInfo.totalPendingInBytes += index.getSizeInBytes();
            }
            if(index.getState() == IndexInfo.State.SNAPSHOTED){
                currentSnapShoted++;
            }
        }

        statusInfo.completedIndices = currentSnapShoted - snapShots.size();

        //fixme: below calc
        statusInfo.totalCompletedInBytes = statusInfo.totalSizeInBytes - statusInfo.totalPendingInBytes;

        return channel -> {
            StatusResponseMessage message = new StatusResponseMessage(statusInfo);
            XContentBuilder builder = channel.newBuilder();
            builder.startObject();
            message.toXContent(builder, restRequest);
            builder.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
        };
    }
}
