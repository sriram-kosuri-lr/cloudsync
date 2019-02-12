package org.elasticsearch.sync.cloud.actions;


import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.sync.cloud.start.SinkThread;
import org.elasticsearch.sync.cloud.start.SourceThread;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.sync.cloud.messages.ErrorResponseMessage;
import org.elasticsearch.sync.cloud.start.StartInfo;
import org.elasticsearch.sync.cloud.utils.*;
import org.elasticsearch.sync.cloud.messages.StartResponseMessage;


import java.io.IOException;
import java.util.InputMismatchException;

import java.util.Map;


import static org.elasticsearch.rest.RestRequest.Method.POST;

public class StartRestAction extends BaseRestHandler {


    @Inject
    public StartRestAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/cloudsync/start", this);
    }

    @Override
    protected BaseRestHandler.RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        try {
            //1. Copy the input
            StartInfo startInfo = new StartInfo(restRequest.content().utf8ToString());
            //logger.info("POSTed source start.");

            //todo:
            //2. Check if plug-ins are already installed based on store.

            //3. split logic based on source or sink
            if (StartInfo.SOURCE.equals(startInfo.getMode())) {
                sourceActions(startInfo, client);
            } else if (StartInfo.SINK.equals(startInfo.getMode())) {
                sinkActions(startInfo, client);
            } else {
               throw new InputMismatchException("Invalid mode. Provide source or sink.");
            }
        } catch (Exception ex){
            return channel -> {
                ErrorResponseMessage message = new ErrorResponseMessage(ex.getMessage());
                XContentBuilder builder = channel.newBuilder();
                builder.startObject();
                message.toXContent(builder, restRequest);
                builder.endObject();
                channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, builder));
            };
        }

        return channel -> {
            StartResponseMessage message = new StartResponseMessage();
            XContentBuilder builder = channel.newBuilder();
            builder.startObject();
            message.toXContent(builder, restRequest);
            builder.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.ACCEPTED, builder));
        };
    }

    private void sourceActions(StartInfo startInfo, NodeClient client) throws IOException {
        ElasticClient elastic = new ElasticClient(client.admin());
        Map<String, Long> indices = elastic.getIndices(startInfo.getIndices());

        String snapshotsJson = Utils.toJson(indices);
        SyncStateFile syncFile = new SyncStateFile();
        syncFile.write(snapshotsJson);//todo: write only if its not present.

        SourceThread sourceThread = new SourceThread(client.admin(),startInfo);
        new Thread(sourceThread).start();
    }

    private void sinkActions(StartInfo startInfo, NodeClient client) throws IOException {
        SinkThread sinkThread = new SinkThread(client.admin(),startInfo);
        new Thread(sinkThread).start();
    }
}
