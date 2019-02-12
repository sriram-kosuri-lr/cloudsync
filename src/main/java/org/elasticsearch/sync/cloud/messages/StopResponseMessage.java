package org.elasticsearch.sync.cloud.messages;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * todo: add stop endpoint.
 */
public class StopResponseMessage implements ToXContent {

    private final String name;

    public StopResponseMessage(String name) {
        if (name == null) {
            this.name = "World";
        } else {
            this.name = name;
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field("message", "Hello " + name + "!");
    }
}
