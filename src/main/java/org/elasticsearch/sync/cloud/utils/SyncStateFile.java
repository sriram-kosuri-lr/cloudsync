package org.elasticsearch.sync.cloud.utils;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * fixme: move this state to elastic index.
 * Knows the location of the file(s) to store the sync-tasks details.
 * Reads those details.
 * Write those details.
 */
public class SyncStateFile {

    private String syncStateFile;
    private String location = "/opt/lr/cloudsync";

    public SyncStateFile(){
        syncStateFile = location+ File.separator + "cloudsync.state";
    }

    public void write(String json) throws IOException {
        FileUtils.write(new File(syncStateFile),json,"utf-8");
    }

    public String read() throws IOException {
        return FileUtils.readFileToString(new File(syncStateFile),"utf-8");
    }

}
