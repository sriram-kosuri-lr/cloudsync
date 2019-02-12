package org.elasticsearch.sync.cloud.actions;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.sync.cloud.messages.StopResponseMessage;


import java.io.IOException;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class StopRestAction extends BaseRestHandler {

    @Inject
    public StopRestAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/cloudsync/stop", this);
    }

    @Override
    protected BaseRestHandler.RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String name = restRequest.param("name");
        return channel -> {
            StopResponseMessage message = new StopResponseMessage(name);
            XContentBuilder builder = channel.newBuilder();
            builder.startObject();
            message.toXContent(builder, restRequest);
            builder.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
        };
    }
}
