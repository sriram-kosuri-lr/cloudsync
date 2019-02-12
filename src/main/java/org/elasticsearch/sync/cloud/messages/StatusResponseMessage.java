package org.elasticsearch.sync.cloud.messages;

import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.sync.cloud.status.StatusInfo;

import java.io.IOException;


/**
 * todo: add status endpoint
 */
public class StatusResponseMessage implements ToXContent {

    private final StatusInfo info;

    public StatusResponseMessage(StatusInfo info) {
        this.info = info;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field("totalIndices", info.totalIndices)
                .field("completedIndices", info.completedIndices)
                .field("pendingIndices", info.pendingIndices)
                .field("totalSize", new ByteSizeValue(info.totalSizeInBytes))
                .field("totalCompletedSize", new ByteSizeValue(info.totalCompletedInBytes))
                .field("totalPendingSize", new ByteSizeValue(info.totalPendingInBytes));
    }
}
